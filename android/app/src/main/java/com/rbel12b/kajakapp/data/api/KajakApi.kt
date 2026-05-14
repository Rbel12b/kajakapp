package com.rbel12b.kajakapp.data.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rbel12b.kajakapp.data.api.model.Athlete
import com.rbel12b.kajakapp.data.api.model.AthleteDetail
import com.rbel12b.kajakapp.data.api.model.CompetitionDetail
import com.rbel12b.kajakapp.data.api.model.Competition
import com.rbel12b.kajakapp.data.api.model.RaceDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class KajakApi(private val tokenProvider: suspend () -> String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): String {
        val token = tokenProvider()
        val queryParams = buildString {
            append("a=$token")
            params.forEach { (k, v) -> append("&$k=$v") }
        }
        val url = "https://kajakapp.com$path?$queryParams"
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                response.body?.string() ?: throw Exception("Empty response")
            }
        }
    }

    suspend fun getCompetitions(): List<Competition> {
        val json = get("/api/getCompetitions")
        val type = object : TypeToken<List<Competition>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun getCompetition(competitionId: String): CompetitionDetail {
        val json = get("/api/getCompetition", mapOf("competitionId" to competitionId))
        return gson.fromJson(json, CompetitionDetail::class.java) ?: CompetitionDetail()
    }

    suspend fun getRace(competitionId: String, raceId: String): RaceDetail {
        val json = get("/api/getRace", mapOf("competitionId" to competitionId, "raceId" to raceId))
        return gson.fromJson(json, RaceDetail::class.java) ?: RaceDetail()
    }

    suspend fun getAthletes(): List<Athlete> {
        val json = get("/api/getAthletes")
        val type = object : TypeToken<List<Athlete>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun getAthlete(athleteId: String): AthleteDetail {
        val json = get("/api/getAthlete", mapOf("athleteId" to athleteId))
        return gson.fromJson(json, AthleteDetail::class.java) ?: AthleteDetail()
    }
}
