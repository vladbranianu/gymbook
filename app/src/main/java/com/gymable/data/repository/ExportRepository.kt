package com.gymable.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.gymable.data.ExportData
import com.gymable.data.ExerciseExport
import com.gymable.data.GymDataExporter
import com.gymable.data.GymExport
import com.gymable.data.MuscleGroupExport
import com.gymable.data.PdfExporter
import com.gymable.data.SessionWithSets
import com.gymable.data.local.dao.ExportDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ExportResult(
    val pdfFileName: String,
    val backupFileName: String,
    // Non-null only on API < 29 — used to fall back to share intent
    val pdfUri: Uri? = null,
    val jsonUri: Uri? = null
) {
    val savedToDownloads: Boolean get() = pdfUri == null
}

@Singleton
class ExportRepository @Inject constructor(
    private val exportDao: ExportDao,
    private val pdfExporter: PdfExporter,
    private val gymDataExporter: GymDataExporter,
    private val themeRepository: ThemeRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend fun exportData(from: Long, until: Long): ExportResult {
        val accent = themeRepository.accent.value
        val data = loadExportData(from, until)
        val dateSuffix = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val pdfName = "gymbook_report_$dateSuffix.pdf"
        val backupName = "gymbook_backup_$dateSuffix.gymdata"

        val pdfDocument = pdfExporter.generate(data, accent.accent.toArgb(), accent.onAccent.toArgb())
        val pdfBytes = ByteArrayOutputStream().also { pdfDocument.writeTo(it) }.toByteArray()
        pdfDocument.close()

        val jsonContent = gymDataExporter.generate(data, from, until)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloads(pdfName, "application/pdf", pdfBytes)
            saveToDownloads(backupName, "application/octet-stream", jsonContent.toByteArray())
            ExportResult(pdfFileName = pdfName, backupFileName = backupName)
        } else {
            val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val pdfFile = File(dir, pdfName).also { it.writeBytes(pdfBytes) }
            val jsonFile = File(dir, backupName).also { it.writeText(jsonContent) }
            fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            ExportResult(pdfFileName = pdfName, backupFileName = backupName, pdfUri = uri(pdfFile), jsonUri = uri(jsonFile))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownloads(fileName: String, mimeType: String, bytes: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create Downloads entry for $fileName")
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }

    private suspend fun loadExportData(from: Long, until: Long): ExportData {
        val gyms = exportDao.getAllGyms()
        return ExportData(
            gyms = gyms.mapNotNull { gym ->
                val muscleGroups = exportDao.getMuscleGroupsForGym(gym.id).mapNotNull { mg ->
                    val exercises = exportDao.getExercisesForMuscleGroup(mg.id).mapNotNull { ex ->
                        val sessions = exportDao.getSessionsForExercise(ex.id, from, until).map { session ->
                            SessionWithSets(
                                session = session,
                                sets = exportDao.getSetsForSession(session.id)
                            )
                        }
                        if (sessions.isEmpty()) null
                        else ExerciseExport(
                            exercise = ex,
                            record = exportDao.getRecordForExercise(ex.id),
                            sessions = sessions
                        )
                    }
                    if (exercises.isEmpty()) null
                    else MuscleGroupExport(muscleGroup = mg, exercises = exercises)
                }
                if (muscleGroups.isEmpty()) null
                else GymExport(gym = gym, muscleGroups = muscleGroups)
            }
        )
    }
}
