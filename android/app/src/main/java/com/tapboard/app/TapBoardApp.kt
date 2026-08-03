package com.tapboard.app

import android.app.Application
import com.tapboard.app.connection.ConnectionRepository
import com.tapboard.app.settings.SettingsRepository

class TapBoardApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var connectionRepository: ConnectionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        connectionRepository = ConnectionRepository(this, settingsRepository)
    }

    companion object {
        lateinit var instance: TapBoardApp
            private set
    }
}
