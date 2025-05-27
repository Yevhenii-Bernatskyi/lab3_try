package com.example.lab3_try

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.example.lab3_try.AppDatabase
import com.example.lab3_try.LocationPoint
import com.example.lab3_try.MainActivity
import com.example.lab3_try.R
import com.example.lab3_try.Track
import kotlinx.coroutines.launch

class LocationService : LifecycleService() {

    companion object {
        const val ACTION_START_TRACKING = "com.example.lab3_try.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.lab3_try.ACTION_STOP_TRACKING"
        const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Відстеження Місцезнаходження"
        const val NOTIFICATION_ID = 1

        val isTracking = MutableLiveData<Boolean>()
    }

    private var isServiceStarted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private lateinit var db: AppDatabase
    private var currentTrackId: Long = -1L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            if (isTracking.value == true) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Нова локація: ${location.latitude}, ${location.longitude}")
                    if (currentTrackId != -1L) {
                        lifecycleScope.launch {
                            val point = LocationPoint(
                                trackId = currentTrackId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = System.currentTimeMillis()
                            )
                            db.locationPointDao().insertPoint(point)

                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        db = AppDatabase.getDatabase(applicationContext)
        isTracking.postValue(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            when (it.action) {
                ACTION_START_TRACKING -> {
                    if (!isServiceStarted) {
                        Log.d("LocationService", "Запуск відстеження...")
                        startForegroundServiceWithNotification()
                        startLocationUpdates()
                        isServiceStarted = true
                        isTracking.postValue(true)
                    }
                }
                ACTION_STOP_TRACKING -> {
                    Log.d("LocationService", "Зупинка відстеження...")
                    stopLocationUpdates()
                    isTracking.postValue(false)
                    isServiceStarted = false
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Дозвіл на місцезнаходження не надано.")
            isTracking.postValue(false)
            return
        }

        // Створюємо новий трек в БД
        lifecycleScope.launch {
            val newTrack = Track(
                name = "Трек від ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                startTime = System.currentTimeMillis()
            )
            currentTrackId = db.trackDao().insertTrack(newTrack)
            Log.d("LocationService", "Створено новий трек з ID: $currentTrackId")
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500L)
            .build()

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationService", "Запит на оновлення місцезнаходження запущено.")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Помилка безпеки при запиті оновлень: ${e.message}")
            isTracking.postValue(false)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        Log.d("LocationService", "Оновлення місцезнаходження зупинено.")
        if (currentTrackId != -1L) {
            lifecycleScope.launch {
                db.trackDao().updateTrackEndTime(currentTrackId, System.currentTimeMillis())
                Log.d("LocationService", "Оновлено час закінчення для треку ID: $currentTrackId")
                currentTrackId = -1L
            }
        }
        stopForeground(true)
    }

    private fun startForegroundServiceWithNotification() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val notificationIcon = R.drawable.ic_stat_location

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Відстеження треку")
            .setContentText("Запис вашого маршруту активний...")
            .setSmallIcon(notificationIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Робить сповіщення таким, що не можна змахнути
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationService", "Сервіс знищено.")
        if (isTracking.value == true) {
            stopLocationUpdates()
        }
    }
}