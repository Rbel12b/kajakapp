package com.rbel12b.kajakapp.data.api.model

import com.google.gson.annotations.SerializedName

data class Athlete(
    @SerializedName("Id") val id: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("Nation") val nation: String = "",
    @SerializedName("BirthYear") val birthYear: String = "",
    @SerializedName("Club") val club: String = "",
    @SerializedName("IcfWorldRank") val icfWorldRank: Int = -1,
    @SerializedName("KajakappEmoji") val emoji: String = "",
)

data class AthleteDetail(
    @SerializedName("Athlete") val athlete: Athlete = Athlete(),
    @SerializedName("Races") val races: Map<String, AthleteRaceEntry> = emptyMap(),
)

data class AthleteRaceEntry(
    @SerializedName("Race") val race: RaceInfo = RaceInfo(),
    @SerializedName("IsFinished") val isFinished: Boolean = false,
    @SerializedName("FinishPosition") val finishPosition: String = "",
    @SerializedName("FinishTime") val finishTime: String = "",
)
