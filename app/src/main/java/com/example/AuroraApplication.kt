package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ChannelRepository
import com.example.data.repository.VODRepository
import com.example.player.PlayerEngineManager

class AuroraApplication : Application() {

    // Dependency Container / Service Locator manual DI
    lateinit var database: AppDatabase
    lateinit var repository: ChannelRepository
    lateinit var vodRepository: VODRepository
    lateinit var playerEngineManager: PlayerEngineManager

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = ChannelRepository(database.channelDao(), database.epgDao(), this)
        vodRepository = VODRepository(this)
        playerEngineManager = PlayerEngineManager(this)
    }
}
