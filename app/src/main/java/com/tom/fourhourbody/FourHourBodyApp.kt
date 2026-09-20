package com.tom.fourhourbody

import android.app.Application
import com.tom.fourhourbody.data.db.DatabaseSeeder
import com.tom.fourhourbody.notifications.Notifier
import com.tom.fourhourbody.notifications.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FourHourBodyApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.ensureChannels(this)

        applicationScope.launch {
            DatabaseSeeder.seedIfNeeded(container.database)
            container.rescheduleReminders()
            ReminderWorker.enqueue(this@FourHourBodyApp)
        }
    }
}
