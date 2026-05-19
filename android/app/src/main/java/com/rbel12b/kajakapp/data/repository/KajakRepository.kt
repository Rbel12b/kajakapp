package com.rbel12b.kajakapp.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rbel12b.kajakapp.data.api.KajakApi
import com.rbel12b.kajakapp.data.api.model.Athlete
import com.rbel12b.kajakapp.data.api.model.AthleteDetail
import com.rbel12b.kajakapp.data.api.model.Competition
import com.rbel12b.kajakapp.data.api.model.CompetitionDetail
import com.rbel12b.kajakapp.data.api.model.RaceDetail
import com.rbel12b.kajakapp.data.cache.FileCache

class KajakRepository(private val api: KajakApi, private val cache: FileCache) {

    private val gson = Gson()

    suspend fun getCompetitions(forceRefresh: Boolean = false): Result<List<Competition>> {
        val key = "competitions"
        if (!forceRefresh) {
            cache.read(key)?.let { json ->
                return runCatching {
                    gson.fromJson(json, object : TypeToken<List<Competition>>() {}.type)
                }
            }
        }
        return runCatching { api.getCompetitions() }
            .onSuccess { cache.write(key, gson.toJson(it)) }
    }

    suspend fun getCompetition(competitionId: String, forceRefresh: Boolean = false): Result<CompetitionDetail> {
        val key = "competition_$competitionId"
        if (!forceRefresh) {
            cache.read(key)?.let { json ->
                return runCatching { gson.fromJson(json, CompetitionDetail::class.java) }
            }
        }
        return runCatching { api.getCompetition(competitionId) }
            .onSuccess { cache.write(key, gson.toJson(it)) }
    }

    suspend fun getRace(competitionId: String, raceId: String, forceRefresh: Boolean = false): Result<RaceDetail> {
        val key = "race_${competitionId}_$raceId"
        if (!forceRefresh) {
            cache.read(key)?.let { json ->
                return runCatching { gson.fromJson(json, RaceDetail::class.java) }
            }
        }
        return runCatching { api.getRace(competitionId, raceId) }
            .onSuccess { cache.write(key, gson.toJson(it)) }
    }

    suspend fun getAthletes(forceRefresh: Boolean = false): Result<List<Athlete>> {
        val key = "athletes"
        if (!forceRefresh) {
            cache.read(key)?.let { json ->
                return runCatching {
                    gson.fromJson(json, object : TypeToken<List<Athlete>>() {}.type)
                }
            }
        }
        return runCatching { api.getAthletes() }
            .onSuccess { cache.write(key, gson.toJson(it)) }
    }

    suspend fun getAthlete(athleteId: String, forceRefresh: Boolean = false): Result<AthleteDetail> {
        val key = "athlete_$athleteId"
        if (!forceRefresh) {
            cache.read(key)?.let { json ->
                return runCatching { gson.fromJson(json, AthleteDetail::class.java) }
            }
        }
        return runCatching { api.getAthlete(athleteId) }
            .onSuccess { cache.write(key, gson.toJson(it)) }
    }
}
