package com.rbel12b.kajakapp.data.api.model

import com.google.gson.annotations.SerializedName

data class Competition(
    @SerializedName("Id") val id: String = "",
    @SerializedName("NameEn") val nameEn: String = "",
    @SerializedName("NameHu") val nameHu: String = "",
    @SerializedName("LocationEn") val locationEn: String = "",
    @SerializedName("LocationHu") val locationHu: String = "",
    @SerializedName("StartDate") val startDate: String = "",
    @SerializedName("EndDate") val endDate: String = "",
    @SerializedName("CompetitionCategory") val category: String = "",
    @SerializedName("CompetitionIcon") val icon: String = "",
) {
    val displayName: String get() = nameEn.ifBlank { nameHu }
    val displayLocation: String get() = locationEn.ifBlank { locationHu }
}

data class CompetitionDetail(
    @SerializedName("Competition") val competition: Competition = Competition(),
    @SerializedName("Races") val races: Map<String, RaceEntry> = emptyMap(),
)

data class RaceEntry(
    @SerializedName("Race") val race: RaceInfo = RaceInfo(),
    @SerializedName("IsFinished") val isFinished: Boolean = false,
)

data class RaceInfo(
    @SerializedName("Id") val id: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("Round") val round: String = "",
    @SerializedName("StartDate") val startDate: String = "",
    @SerializedName("RaceCategory") val raceCategory: String = "",
    @SerializedName("IsBestFinal") val isBestFinal: Boolean = false,
    @SerializedName("Competition") val competition: RaceCompetitionRef? = null,
)

data class RaceCompetitionRef(
    @SerializedName("Id") val id: String = "",
    @SerializedName("NameEn") val nameEn: String = "",
    @SerializedName("NameHu") val nameHu: String = "",
    @SerializedName("CompetitionIcon") val icon: String = "",
) {
    val displayName: String get() = nameEn.ifBlank { nameHu }
}
