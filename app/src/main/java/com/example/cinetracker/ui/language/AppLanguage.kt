package com.example.cinetracker.ui.language

import androidx.annotation.StringRes
import com.example.cinetracker.R

enum class AppLanguage(
    val tag: String,
    val shortLabel: String,
    @param:StringRes val labelRes: Int,
) {
    SLOVAK(
        tag = "sk",
        shortLabel = "SK",
        labelRes = R.string.language_slovak,
    ),
    ENGLISH(
        tag = "en",
        shortLabel = "EN",
        labelRes = R.string.language_english,
    ),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SLOVAK
    }
}
