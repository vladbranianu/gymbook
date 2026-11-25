package com.gymable.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.UnitSystem
import com.gymable.ui.theme.label
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.settings.SettingsViewModel
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.TextSecondary
import com.gymable.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onColor: () -> Unit,
    onSupportDev: () -> Unit,
    onContact: () -> Unit,
    onEditData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importSuccess by viewModel.importSuccess.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showExportSuccess by remember { mutableStateOf(false) }
    var exportSuccessNames by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(exportResult) {
        exportResult?.let { result ->
            if (result.savedToDownloads) {
                exportSuccessNames = "${result.pdfFileName}\n${result.backupFileName}"
                showExportSuccess = true
            } else {
                // API < 29 fallback: open share sheet
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        arrayListOf(result.pdfUri!!, result.jsonUri!!)
                    )
                    putExtra(Intent.EXTRA_SUBJECT, "GymBook Data Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Save or share export"))
            }
            viewModel.consumeExportResult()
        }
    }

    // File picker for import — accepts any file (user must pick .gymdata)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    // Dialogs
    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = { Text("Export Saved") },
            text = {
                Text(
                    "Files saved to your Downloads folder:\n\n$exportSuccessNames",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showExportSuccess = false }) { Text("OK") }
            }
        )
    }

    if (showExportDialog) {
        ExportDateRangeDialog(
            onDismiss = { showExportDialog = false },
            onExport = { from, until ->
                showExportDialog = false
                viewModel.exportData(from, until)
            }
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Import Backup") },
            text = {
                Text(
                    "This will permanently replace all existing workout data with the contents of the selected file. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    showImportConfirm = false
                    pendingImportUri?.let { viewModel.importData(it) }
                    pendingImportUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri = null
                }) { Text("Cancel") }
            }
        )
    }

    if (importSuccess) {
        LaunchedEffect(Unit) { viewModel.consumeImportSuccess() }
        AlertDialog(
            onDismissRequest = { viewModel.consumeImportSuccess() },
            title = { Text("Import Complete") },
            text = { Text("Your workout data has been restored successfully.") },
            confirmButton = {
                TextButton(onClick = { viewModel.consumeImportSuccess() }) { Text("OK") }
            }
        )
    }

    importError?.let { err ->
        AlertDialog(
            onDismissRequest = { viewModel.consumeImportError() },
            title = { Text("Import Failed") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { viewModel.consumeImportError() }) { Text("OK") }
            }
        )
    }

    ScreenScaffold(
        title = "Settings",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SettingsItem(
                    icon = Icons.Rounded.ColorLens,
                    label = "Color",
                    description = "Customize the app accent color",
                    onClick = onColor
                )
                SettingsItem(
                    icon = Icons.Rounded.Edit,
                    label = "Edit Exercises",
                    description = "Rename or delete gyms, muscle groups and exercises",
                    onClick = onEditData
                )
                SettingsItem(
                    icon = Icons.Rounded.Favorite,
                    label = "Support the Developer",
                    description = "Donate to unlock premium accent colors",
                    onClick = onSupportDev
                )
                SettingsItem(
                    icon = Icons.Rounded.Email,
                    label = "Contact & Feedback",
                    description = "Send a message to the developer",
                    onClick = onContact
                )
                UnitToggleItem(
                    unit = unitSystem,
                    onUnitChange = viewModel::setUnitSystem
                )
                SettingsItem(
                    icon = Icons.Rounded.Upload,
                    label = if (isExporting) "Exporting…" else "Export Data",
                    description = "Save workout history to PDF + backup file",
                    isLoading = isExporting,
                    onClick = { if (!isExporting) showExportDialog = true }
                )
                SettingsItem(
                    icon = Icons.Rounded.Download,
                    label = if (isImporting) "Importing…" else "Import Data",
                    description = "Restore workout data from a .gymdata backup file",
                    isLoading = isImporting,
                    onClick = { if (!isImporting) importLauncher.launch(arrayOf("application/octet-stream", "application/json", "application/x-json")) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDateRangeDialog(
    onDismiss: () -> Unit,
    onExport: (from: Long, until: Long) -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var fromMs by remember { mutableStateOf<Long?>(null) }
    var untilMs by remember { mutableStateOf<Long?>(null) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingUntil by remember { mutableStateOf(false) }

    if (pickingFrom) {
        val state = rememberDatePickerState(initialSelectedDateMillis = fromMs)
        DatePickerDialog(
            onDismissRequest = { pickingFrom = false },
            confirmButton = {
                TextButton(onClick = {
                    fromMs = state.selectedDateMillis
                    pickingFrom = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickingFrom = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (pickingUntil) {
        val state = rememberDatePickerState(initialSelectedDateMillis = untilMs)
        DatePickerDialog(
            onDismissRequest = { pickingUntil = false },
            confirmButton = {
                TextButton(onClick = {
                    untilMs = state.selectedDateMillis
                    pickingUntil = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickingUntil = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Export Date Range",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Select the period of workout data to include in the export.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column {
                        DateRow(
                            label = "From",
                            dateText = fromMs?.let { fmt.format(Date(it)) } ?: "Select start date",
                            hasValue = fromMs != null,
                            onClick = { pickingFrom = true }
                        )
                        HorizontalDivider(color = CardBorder)
                        DateRow(
                            label = "To",
                            dateText = untilMs?.let { fmt.format(Date(it)) } ?: "Select end date",
                            hasValue = untilMs != null,
                            onClick = { pickingUntil = true }
                        )
                    }
                }

                val rangeValid = fromMs != null && untilMs != null && fromMs!! <= untilMs!!
                val rangeError = fromMs != null && untilMs != null && fromMs!! > untilMs!!

                if (rangeError) {
                    Text(
                        text = "Start date must be before end date.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val from = fromMs!!
                            val until = untilMs!! + 24L * 60 * 60 * 1000
                            onExport(from, until)
                        },
                        enabled = rangeValid
                    ) { Text("Export") }
                }
            }
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    dateText: String,
    hasValue: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasValue) FontWeight.Medium else FontWeight.Normal,
                color = if (hasValue) MaterialTheme.colorScheme.onSurface else TextSecondary
            )
        }
    }
}

@Composable
private fun UnitToggleItem(unit: UnitSystem, onUnitChange: (UnitSystem) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Scale,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unit of Measurement",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Weight display in workouts and stats",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    listOf(UnitSystem.KG, UnitSystem.LBS).forEach { u ->
                        val isSelected = u == unit
                        Surface(
                            onClick = { onUnitChange(u) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = u.label(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
