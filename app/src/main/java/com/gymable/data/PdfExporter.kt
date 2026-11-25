package com.gymable.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.gymable.data.local.entity.WorkoutSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor() {

    fun generate(data: ExportData, accentArgb: Int, onAccentArgb: Int): PdfDocument =
        Renderer(accentArgb, onAccentArgb).render(data)

    // ── Per-call renderer — holds accent so singleton stays thread-safe ────────

    private inner class Renderer(accentArgb: Int, onAccentArgb: Int) {

        private val W = 595f
        private val H = 842f
        private val margin = 48f
        private val contentW = W - margin * 2

        private val accent     = accentArgb
        private val accentText = onAccentArgb
        private val textPrimary   = Color.rgb(0x14, 0x14, 0x14)
        private val textSecondary = Color.rgb(0x60, 0x69, 0x7A)
        private val gold          = Color.rgb(0xCC, 0x8E, 0x1A)
        private val separator     = Color.rgb(0xDF, 0xE3, 0xE8)
        private val rowAlt        = Color.rgb(0xF5, 0xF7, 0xFA)

        private val fmtDate = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)

        fun render(data: ExportData): PdfDocument {
            val document = PdfDocument()
            val ctx = PageContext(document)

            drawCover(ctx, data)
            ctx.newPage()

            for (gymExport in data.gyms) {
                drawGym(ctx, gymExport)
            }

            ctx.newPage()
            drawAppendix(ctx, data)
            ctx.finish()
            return document
        }

        // ── Cover ─────────────────────────────────────────────────────────────

        private fun drawCover(ctx: PageContext, data: ExportData) {
            val c = ctx.canvas

            c.drawRect(0f, 0f, W, 170f, fill(accent))

            c.drawText("GYMBOOK", W / 2f, 100f,
                text(40f, Color.WHITE, Typeface.BOLD, Paint.Align.CENTER, letterSpacing = 0.22f))
            c.drawText("Track every set.", W / 2f, 132f,
                text(13f, Color.argb(210, 255, 255, 255), align = Paint.Align.CENTER))
            c.drawText("WORKOUT DATA EXPORT", W / 2f, 155f,
                text(8.5f, Color.argb(160, 255, 255, 255), letterSpacing = 0.12f, align = Paint.Align.CENTER))

            c.drawText("Exported on  ${fmtDate.format(Date(data.exportDateMs))}",
                margin, 200f, text(10f, textSecondary))
            c.drawLine(margin, 208f, W - margin, 208f, line(separator))

            val totalGyms      = data.gyms.size
            val totalExercises = data.gyms.sumOf { g -> g.muscleGroups.sumOf { it.exercises.size } }
            val totalSessions  = data.gyms.sumOf { g -> g.muscleGroups.sumOf { mg -> mg.exercises.sumOf { it.sessions.size } } }
            val totalRecords   = data.gyms.sumOf { g -> g.muscleGroups.sumOf { mg -> mg.exercises.count { it.record != null } } }

            listOf(
                totalGyms.toString()      to "Gyms",
                totalExercises.toString() to "Exercises",
                totalSessions.toString()  to "Sessions",
                totalRecords.toString()   to "Records"
            ).forEachIndexed { i, (value, label) ->
                val colW = contentW / 4
                val cx = margin + i * colW + colW / 2f
                c.drawText(value, cx, 244f, text(26f, accent, Typeface.BOLD, Paint.Align.CENTER))
                c.drawText(label.uppercase(), cx, 258f,
                    text(8f, textSecondary, align = Paint.Align.CENTER, letterSpacing = 0.08f))
            }

            c.drawLine(margin, 268f, W - margin, 268f, line(separator))
            c.drawText(
                "This file can be imported back into Gymbook to restore your data.",
                margin, 284f, text(9f, textSecondary)
            )
        }

        // ── Gym ───────────────────────────────────────────────────────────────

        private fun drawGym(ctx: PageContext, gymExport: GymExport) {
            ctx.ensureSpace(44f)
            ctx.canvas.drawRect(margin - 14f, ctx.y, W - margin + 14f, ctx.y + 32f, fill(accent))
            ctx.canvas.drawText(
                gymExport.gym.name.uppercase(),
                margin, ctx.y + 22f,
                text(12f, accentText, Typeface.BOLD, letterSpacing = 0.08f)
            )
            ctx.y += 40f
            for (mg in gymExport.muscleGroups) drawMuscleGroup(ctx, mg)
            ctx.y += 4f
        }

        // ── Muscle group ──────────────────────────────────────────────────────

        private fun drawMuscleGroup(ctx: PageContext, mgExport: MuscleGroupExport) {
            ctx.ensureSpace(32f)
            val mgPaint = text(11.5f, textPrimary, Typeface.BOLD)
            ctx.canvas.drawText(mgExport.muscleGroup.name, margin, ctx.y + 12f, mgPaint)
            val tw = mgPaint.measureText(mgExport.muscleGroup.name)
            ctx.canvas.drawLine(margin, ctx.y + 15f, margin + tw, ctx.y + 15f,
                Paint().apply { color = accent; strokeWidth = 1.5f })
            ctx.y += 22f
            for (ex in mgExport.exercises) drawExercise(ctx, ex)
            ctx.y += 4f
        }

        // ── Exercise ──────────────────────────────────────────────────────────

        private fun drawExercise(ctx: PageContext, exExport: ExerciseExport) {
            ctx.ensureSpace(32f)
            val c = ctx.canvas

            c.drawText(exExport.exercise.name, margin + 8f, ctx.y + 12f,
                text(11f, textPrimary, Typeface.BOLD))

            val recText = exExport.record?.let {
                "PR  ${trimFloat(it.maxWeight)} kg × ${it.maxReps} reps"
            } ?: "No record"
            val recColor = if (exExport.record != null) gold else textSecondary
            val recStyle = if (exExport.record != null) Typeface.BOLD else Typeface.NORMAL
            c.drawText(recText, W - margin, ctx.y + 12f,
                text(9f, recColor, recStyle, align = Paint.Align.RIGHT))
            ctx.y += 18f

            if (exExport.sessions.isNotEmpty()) drawSessionsTable(ctx, exExport.sessions.map { it.session.date to it.sets })

            ctx.ensureSpace(8f)
            ctx.canvas.drawLine(margin + 8f, ctx.y + 2f, W - margin, ctx.y + 2f, line(separator))
            ctx.y += 10f
        }

        // ── Sessions table ────────────────────────────────────────────────────

        private fun drawSessionsTable(ctx: PageContext, sessions: List<Pair<Long, List<WorkoutSet>>>) {
            ctx.ensureSpace(12f)
            val hdrPaint = text(7.5f, textSecondary)
            ctx.canvas.drawText("Date", margin + 8f, ctx.y + 9f, hdrPaint)
            ctx.canvas.drawText("Sets", margin + 8f + 92f, ctx.y + 9f, hdrPaint)
            ctx.y += 11f

            sessions.forEachIndexed { index, (date, sets) ->
                ctx.ensureSpace(12f)
                if (index % 2 == 0) {
                    ctx.canvas.drawRect(margin + 4f, ctx.y, W - margin, ctx.y + 11f, fill(rowAlt))
                }
                val rowPaint = text(8.5f, textSecondary, typeface = Typeface.MONOSPACE)
                val setsText = sets.joinToString("  ") { "${it.reps}×${trimFloat(it.weight)}kg" }
                ctx.canvas.drawText(fmtDate.format(Date(date)), margin + 8f, ctx.y + 9f, rowPaint)
                ctx.canvas.drawText(setsText, margin + 8f + 92f, ctx.y + 9f, rowPaint)
                ctx.y += 11f
            }
            ctx.y += 2f
        }

        // ── Appendix ──────────────────────────────────────────────────────────

        private fun drawAppendix(ctx: PageContext, data: ExportData) {
            val c = ctx.canvas
            c.drawText("MACHINE-READABLE DATA", margin, ctx.y + 10f,
                text(9f, textSecondary, Typeface.BOLD, letterSpacing = 0.1f))
            ctx.y += 14f
            c.drawLine(margin, ctx.y, W - margin, ctx.y, line(separator))
            ctx.y += 10f
            c.drawText("Used by Gymbook import. Do not edit.",
                margin, ctx.y + 9f, text(8f, textSecondary))
            ctx.y += 16f

            val dataPaint = text(6.5f, textSecondary, typeface = Typeface.MONOSPACE)

            fun writeLine(txt: String) {
                ctx.ensureSpace(9f)
                ctx.canvas.drawText(txt, margin, ctx.y + 7f, dataPaint)
                ctx.y += 8f
            }

            writeLine("--- GYMBOOK DATA EXPORT v1 ---")
            for (gymExport in data.gyms) {
                writeLine("GYM|${gymExport.gym.name}")
                for (mg in gymExport.muscleGroups) {
                    writeLine("MG|${gymExport.gym.name}|${mg.muscleGroup.name}")
                    for (ex in mg.exercises) {
                        writeLine("EX|${mg.muscleGroup.name}|${ex.exercise.name}")
                        ex.record?.let {
                            writeLine("REC|${ex.exercise.name}|${it.maxWeight}|${it.maxReps}")
                        }
                        for (sws in ex.sessions) {
                            val setsStr = sws.sets.joinToString("|") { "${it.reps}|${it.weight}" }
                            writeLine("SES|${ex.exercise.name}|${sws.session.date}|$setsStr")
                        }
                    }
                }
            }
            writeLine("--- END ---")
        }

        // ── Page context ──────────────────────────────────────────────────────

        inner class PageContext(private val document: PdfDocument) {
            private var pageNum = 0
            private var page: PdfDocument.Page = startPage()
            var y: Float = margin
            val canvas: Canvas get() = page.canvas

            private fun startPage(): PdfDocument.Page {
                pageNum++
                val info = PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNum).create()
                return document.startPage(info).also { it.canvas.drawColor(Color.WHITE) }
            }

            fun newPage() {
                drawFooter(page.canvas, pageNum)
                document.finishPage(page)
                page = startPage()
                y = margin
            }

            fun finish() {
                drawFooter(page.canvas, pageNum)
                document.finishPage(page)
            }

            fun ensureSpace(needed: Float) {
                if (y + needed > H - margin - 20f) newPage()
            }

            private fun drawFooter(c: Canvas, num: Int) {
                c.drawLine(margin, H - 28f, W - margin, H - 28f, line(separator))
                c.drawText("GYMBOOK  ·  Page $num", W / 2f, H - 12f,
                    text(8f, textSecondary, align = Paint.Align.CENTER))
            }
        }

        // ── Paint helpers ──────────────────────────────────────────────────────

        private fun fill(color: Int) = Paint().apply { this.color = color; style = Paint.Style.FILL }
        private fun line(color: Int) = Paint().apply { this.color = color; strokeWidth = 0.5f }
        private fun text(
            size: Float,
            color: Int,
            bold: Int = Typeface.NORMAL,
            align: Paint.Align = Paint.Align.LEFT,
            letterSpacing: Float = 0f,
            typeface: Typeface = Typeface.DEFAULT
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            this.typeface = Typeface.create(typeface, bold)
            textAlign = align
            this.letterSpacing = letterSpacing
        }

        private fun trimFloat(v: Float) = if (v % 1f == 0f) v.toInt().toString() else v.toString()
    }
}
