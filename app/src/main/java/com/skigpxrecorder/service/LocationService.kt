package com.skigpxrecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skigpxrecorder.MainActivity
import com.skigpxrecorder.R
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var repository: com.skigpxrecorder.data.repository.GpxRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var wakeLock: PowerManager.WakeLock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var autoSaveJob: Job? = null

    private val _locationFlow = MutableSharedFlow<TrackPoint>(extraBufferCapacity = 100)
    val locationFlow: SharedFlow<TrackPoint> = _locationFlow.asSharedFlow()

    private val _batteryWarningFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val batteryWarningFlow: SharedFlow<Boolean> = _batteryWarningFlow.asSharedFlow()

    private var isRecording = false
    private var currentSpeed = 0f
    private var currentElevation = 0f
    private var gpsAccuracy = 0f
    private var elapsedTime = 0L
    private var startTime = 0L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = level * 100 / scale.toFloat()

                when {
                    batteryPct <= Constants.BATTERY_AUTO_STOP_THRESHOLD -> {
                        stopRecording()
                        stopSelf()
                    }
                    batteryPct <= Constants.BATTERY_WARNING_THRESHOLD -> {
                        serviceScope.launch {
                            _batteryWarningFlow.emit(true)
                        }
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        registerBatteryReceiver()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.SERVICE_ACTION_START -> startRecording()
            Constants.SERVICE_ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (isRecording) return

        isRecording = true
        startTime = System.currentTimeMillis()
        
        val notification = createNotification()
        startForeground(Constants.NOTIFICATION_ID, notification)

        startLocationUpdates()
        startAutoSave()
    }

    private fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        stopLocationUpdates()
        autoSaveJob?.cancel()

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    handleNewLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun handleNewLocation(location: Location) {
        android.util.Log.d("LocationService", "Location received: accuracy=${location.accuracy}m")

        // Update UI with current GPS state (even if we'll filter it out)
        gpsAccuracy = location.accuracy
        currentSpeed = location.speed * 3.6f // Convert m/s to km/h
        currentElevation = location.altitude.toFloat()
        elapsedTime = (System.currentTimeMillis() - startTime) / 1000

        if (location.accuracy > Constants.GPS_ACCURACY_THRESHOLD) {
            android.util.Log.w("LocationService", "Location filtered out: accuracy ${location.accuracy}m > threshold ${Constants.GPS_ACCURACY_THRESHOLD}m")
            return
        }

        android.util.Log.i("LocationService", "Location accepted: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m")
        val trackPoint = TrackPoint.fromLocation(location)

        serviceScope.launch {
            _locationFlow.emit(trackPoint)
            repository.addTrackPoint(trackPoint)
        }

        updateNotification()
    }

    private fun startAutoSave() {
        autoSaveJob = serviceScope.launch {
            while (isRecording) {
                delay(Constants.AUTO_SAVE_INTERVAL)
                if (isRecording) {
                    repository.saveTempGpx()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(getString(R.string.recording_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText("Speed: %.1f km/h | Distance: %.0f m".format(
                currentSpeed,
                repository.getCurrentDistance()
            ))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SkiGPXRecorder::RecordingWakeLock"
        )
        @Suppress("WakelockTimeout")
        wakeLock.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        if (isRecording) {
            stopRecording()
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
    
    fun getCurrentStats(): ServiceStats {
        return ServiceStats(
            isRecording = isRecording,
            currentSpeed = currentSpeed,
            currentElevation = currentElevation,
            gpsAccuracy = gpsAccuracy,
            elapsedTime = elapsedTime
        )
    }

    data class ServiceStats(
        val isRecording: Boolean,
        val currentSpeed: Float,
        val currentElevation: Float,
        val gpsAccuracy: Float,
        val elapsedTime: Long
    )
}
