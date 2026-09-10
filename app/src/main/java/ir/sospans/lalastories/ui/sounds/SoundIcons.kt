package ir.sospans.lalastories.ui.sounds

import androidx.annotation.DrawableRes
import ir.sospans.lalastories.R

/** Bundled artwork for each calm sound; null falls back to a generic icon. */
@DrawableRes
fun soundImageRes(id: String): Int? = when (id) {
    "fan" -> R.drawable.sound_fan
    "hairdryer" -> R.drawable.sound_hairdryer
    "vacuum" -> R.drawable.sound_vacuum
    else -> null
}
