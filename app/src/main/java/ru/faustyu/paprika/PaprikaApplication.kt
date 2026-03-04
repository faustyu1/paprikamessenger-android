package ru.faustyu.paprika

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ru.faustyu.paprika.data.repository.MessageRepository
import javax.inject.Inject

@HiltAndroidApp
class PaprikaApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var messageRepository: MessageRepository
    
    @Inject
    lateinit var prefsManager: ru.faustyu.paprika.data.PrefsManager
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Sync manual Network module with prefs
        ru.faustyu.paprika.data.network.NetworkModule.init(prefsManager)
        
        // Setup periodic message sync
        messageRepository.setupPeriodicSync()
    }
}
