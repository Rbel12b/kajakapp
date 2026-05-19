package com.rbel12b.kajakapp

import android.app.Application
import com.rbel12b.kajakapp.data.api.KajakApi
import com.rbel12b.kajakapp.data.cache.FileCache
import com.rbel12b.kajakapp.data.repository.KajakRepository
import com.rbel12b.kajakapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

class KajakApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var kajakRepository: KajakRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        val api = KajakApi {
            runBlocking { settingsRepository.tokenFlow.first() }
        }
        val fileCache = FileCache(File(cacheDir, "api_cache"))
        kajakRepository = KajakRepository(api, fileCache)
    }
}
