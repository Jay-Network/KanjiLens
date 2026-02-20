package com.jworks.kanjilens.domain.models

data class KanjiInfo(
    val literal: String,
    val grade: Int?,
    val strokeCount: Int,
    val frequency: Int?,
    val jlpt: Int?,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val meanings: List<String>
) {
    val gradeLabel: String?
        get() = grade?.let { "Grade $it" }

    val jlptLabel: String?
        get() = jlpt?.let {
            // KANJIDIC2 uses old JLPT levels (1-4), map to N-levels
            when (it) {
                1 -> "JLPT N1"
                2 -> "JLPT N2"
                3 -> "JLPT N3"
                4 -> "JLPT N4"
                else -> "JLPT N$it"
            }
        }
}
