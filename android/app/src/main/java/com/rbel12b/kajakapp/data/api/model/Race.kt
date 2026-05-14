package com.rbel12b.kajakapp.data.api.model

import com.google.gson.annotations.SerializedName

data class RaceDetail(
    @SerializedName("Race") val race: RaceInfo = RaceInfo(),
    @SerializedName("IsFinished") val isFinished: Boolean = false,
    @SerializedName("Boats") val boats: List<Boat> = emptyList(),
)

data class Boat(
    @SerializedName("FinishPosition") val finishPosition: String = "",
    @SerializedName("FinishTime") val finishTime: String = "",
    @SerializedName("FinishTimeDelta") val finishTimeDelta: String = "",
    @SerializedName("StartNumber") val startNumber: String = "",
    @SerializedName("Athletes") val athletes: List<BoatAthlete> = emptyList(),
)

data class BoatAthlete(
    @SerializedName("Id") val id: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("Nation") val nation: String = "",
    @SerializedName("KajakappEmoji") val emoji: String = "",
)
