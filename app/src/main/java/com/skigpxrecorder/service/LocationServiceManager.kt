package com.skigpxrecorder.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.skigpxrecorder.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var locationService: LocationService? = null
    private var isBound = false

    private val _serviceBoundFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val serviceBoundFlow: SharedFlow<Boolean> = _serviceBoundFlow.asSharedFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            isBound = true
            _serviceBoundFlow.tryEmit(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isBound = false
            _serviceBoundFlow.tryEmit(false)
        }
    }

    fun bindService() {
        Intent(context, LocationService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            locationService = null
        }
    }

    fun startRecording() {
        Intent(context, LocationService::class.java).apply {
            action = Constants.SERVICE_ACTION_START
            context.startService(this)
        }
        if (!isBound) {
            bindService()
        }
    }

    fun stopRecording() {
        Intent(context, LocationService::class.java).apply {
            action = Constants.SERVICE_ACTION_STOP
            context.startService(this)
        }
    }

    fun getService(): LocationService? = locationService

    fun isRecording(): Boolean = locationService?.isCurrentlyRecording() ?: false
}
