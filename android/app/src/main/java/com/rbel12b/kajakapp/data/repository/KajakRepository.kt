package com.rbel12b.kajakapp.data.repository

import com.rbel12b.kajakapp.data.api.KajakApi
import com.rbel12b.kajakapp.data.api.model.Athlete
import com.rbel12b.kajakapp.data.api.model.AthleteDetail
import com.rbel12b.kajakapp.data.api.model.Competition
import com.rbel12b.kajakapp.data.api.model.CompetitionDetail
import com.rbel12b.kajakapp.data.api.model.RaceDetail

class KajakRepository(private val api: KajakApi) {

    suspend fun getCompetitions(): Result<List<Competition>> = runCatching {
        api.getCompetitions()
    }

    suspend fun getCompetition(competitionId: String): Result<CompetitionDetail> = runCatching {
        api.getCompetition(competitionId)
    }

    suspend fun getRace(competitionId: String, raceId: String): Result<RaceDetail> = runCatching {
        api.getRace(competitionId, raceId)
    }

    suspend fun getAthletes(): Result<List<Athlete>> = runCatching {
        api.getAthletes()
    }

    suspend fun getAthlete(athleteId: String): Result<AthleteDetail> = runCatching {
        api.getAthlete(athleteId)
    }
}
