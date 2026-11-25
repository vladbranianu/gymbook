package com.gymable.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class UnitSystem { KG, LBS }

val LocalUnitSystem = staticCompositionLocalOf { UnitSystem.KG }

fun Float.toDisplayWeight(unit: UnitSystem): Float =
    if (unit == UnitSystem.LBS) this * 2.20462f else this

fun Float.toStorageKg(unit: UnitSystem): Float =
    if (unit == UnitSystem.LBS) this * 0.45359f else this

fun UnitSystem.label(): String = if (this == UnitSystem.LBS) "lbs" else "kg"

fun Float.fmtWeight(unit: UnitSystem): String {
    val v = toDisplayWeight(unit)
    return if (v % 1f == 0f) "${v.toInt()} ${unit.label()}" else "%.1f ${unit.label()}".format(v)
}
