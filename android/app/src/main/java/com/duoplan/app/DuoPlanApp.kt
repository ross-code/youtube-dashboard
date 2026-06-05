package com.duoplan.app

import android.app.Application
import com.duoplan.app.auth.GoogleAuthManager
import com.duoplan.app.data.PlanRepository
import com.duoplan.app.data.SettingsRepository
import com.duoplan.app.data.remote.NetworkModule

/** Lightweight manual DI container, created once for the process. */
class AppContainer(app: Application) {
    val settings = SettingsRepository(app)
    val auth = GoogleAuthManager(app)
    private val api = NetworkModule.calendarApi(auth)
    val plans = PlanRepository(api)
}

class DuoPlanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
