package xyz.fcr.sberrunner.presentation.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.fcr.sberrunner.R
import xyz.fcr.sberrunner.presentation.service.notification.AudioNotificator.Companion.VOICE_COMPLETE
import xyz.fcr.sberrunner.presentation.service.notification.AudioNotificator.Companion.VOICE_PAUSE
import xyz.fcr.sberrunner.presentation.service.notification.AudioNotificator.Companion.VOICE_RESUME
import xyz.fcr.sberrunner.presentation.service.notification.AudioNotificator.Companion.VOICE_START
import xyz.fcr.sberrunner.presentation.service.notification.IAudioNotificator
import xyz.fcr.sberrunner.presentation.App
import xyz.fcr.sberrunner.utils.Constants
import xyz.fcr.sberrunner.utils.Constants.ACTION_PAUSE_SERVICE
import xyz.fcr.sberrunner.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import xyz.fcr.sberrunner.utils.Constants.ACTION_STOP_SERVICE
import xyz.fcr.sberrunner.utils.Constants.AMOUNT_TO_SKIP
import xyz.fcr.sberrunner.utils.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import xyz.fcr.sberrunner.utils.Constants.LOCATION_UPDATE_INTERVAL
import xyz.fcr.sberrunner.utils.Constants.NOTIFICATION_CHANNEL_ID
import xyz.fcr.sberrunner.utils.Constants.NOTIFICATION_CHANNEL_NAME
import xyz.fcr.sberrunner.utils.Constants.NOTIFICATION_ID
import xyz.fcr.sberrunner.utils.TrackingUtility
import xyz.fcr.sberrunner.utils.hasBasicLocationPermissions
import javax.inject.Inject
import kotlin.math.round

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

/**
 * ???????????? ????????.
 * ?????????????????? ?? ?????????????????? ?????????? ?????????????????? ????????, ???????????? ???????????? ????????.
 * ?????????? ???????????????? ???????????????????? ???? Activity.
 */
class RunningService : LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var audioNotificator: IAudioNotificator

    private lateinit var currentNotification: NotificationCompat.Builder

    private val timeRunInSeconds = MutableLiveData<Long>()

    private var isFirstRun = true
    private var serviceKilled = false

    private var skipFirstTwoPositions = AMOUNT_TO_SKIP

    companion object {
        val timeRunInMillis = MutableLiveData<Long>()

        val avgSpeed = MutableLiveData<Float>()
        val distance = MutableLiveData<Float>()
        val calories = MutableLiveData<Float>()

        val isTracking = MutableLiveData<Boolean>()
        val isPaused = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    init {
        App.appComponent.inject(this)
    }

    override fun onCreate() {
        super.onCreate()
        currentNotification = baseNotificationBuilder
        postInitialValues()

        isTracking.observe(this) {
            updateNotificationTrackingState(it)
            updateLocationChecking(it)
        }
    }

    /**
     * ?????????????????????????? MutableLiveData ???????????????????? ????????????????????
     */
    private fun postInitialValues() {
        timeRunInMillis.postValue(0L)
        avgSpeed.postValue(0.0f)
        distance.postValue(0.0f)
        calories.postValue(0.0f)

        isTracking.postValue(false)
        isPaused.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
    }

    /**
     * ?????????????????? ?????????????? ???? ?????? ?????????? intent
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    startOrResumeService()
                }
                ACTION_PAUSE_SERVICE -> {
                    audioNotificator.play(VOICE_PAUSE)
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    audioNotificator.play(VOICE_COMPLETE)
                    stopService()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * ???????????? ?????????????? (?????? ?????????????????????????? ????????????, ???????? ?????? ?? ??????????)
     */
    private fun startOrResumeService() {
        isPaused.postValue(false)

        if (isFirstRun) {
            audioNotificator.play(VOICE_START)
            startForegroundService()
            isFirstRun = false
            serviceKilled = false
        } else {
            audioNotificator.play(VOICE_RESUME)
            startTimer()
        }
    }

    /**
     * ?????????? ?? ???????????? ??????????????
     */
    private fun pauseService() {
        isTimerEnabled = false
        skipFirstTwoPositions = AMOUNT_TO_SKIP
        isTracking.postValue(false)
        isPaused.postValue(true)
    }

    /**
     * ?????????????????? ??????????????
     */
    private fun stopService() {
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    /**
     * ???????????????????? ???????????????? ?????????????????? ????????????????????
     *
     * @param isTracking [Boolean] - ???????? ?????????????????????? ?? ?????????????? ??????????????
     */
    @SuppressLint("MissingPermission")
    private fun updateLocationChecking(isTracking: Boolean) {
        if (isTracking) {
            if (this.hasBasicLocationPermissions()) {
                val request = LocationRequest.create().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_UPDATE_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (skipFirstTwoPositions < 1) {
                if (isTracking.value!!) {
                    result.locations.let { locations ->
                        for (location in locations) {
                            addPathPoint(location)
                        }
                    }
                }
            } else {
                skipFirstTwoPositions--
            }
        }
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    /**
     * ???????????? ??????????????
     */
    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true

        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                    updateInfo()
                }

                delay(Constants.TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    /**
     * ???????????????????? livedata ????????????????
     */
    private fun updateInfo() {
        var distanceInMeters = 0f

        val paths = pathPoints.value
        val time = timeRunInMillis.value

        if (paths != null && time != null) {
            for (polyline in pathPoints.value!!) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            audioNotificator.checkIfVoiceNotificationNeeded(distance.value!!, distanceInMeters / 1000f)

            distance.postValue(distanceInMeters / 1000f)
            avgSpeed.postValue((round((distanceInMeters / 1000f) / (time / 1000f / 60 / 60) * 10) / 10f))
            calories.postValue(distanceInMeters / 1000f)
        }
    }

    /**
     * ???????????????????? ???????????????????? ?? ???????? PathPoints
     */
    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    /**
     * ???????????????????? ???????????? polyline ?? ???????? pathPoints ?????? ???????????????????????????? ?????? ???????????? ????????????????
     */
    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    /**
     * ?????????????????? foreground ???????????? ?? ???????????? ??????????????????????
     */
    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        startForeground(NOTIFICATION_ID, currentNotification.build())
        startTimer()
        isTracking.postValue(true)

        timeRunInSeconds.observe(this) {
            if (!serviceKilled) {
                val notification =
                    currentNotification.setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000L))

                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        }
    }

    /**
     * ?????????????????? ?????????????? ?? ????????????????????????
     */
    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if (isTracking) {
            App.appComponent.context().getString(R.string.pause)
        } else {
            App.appComponent.context().getString(R.string.resume)
        }

        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, RunningService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, RunningService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        currentNotification.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currentNotification, ArrayList<NotificationCompat.Action>())
        }

        if (!serviceKilled) {
            currentNotification = baseNotificationBuilder
                .addAction(R.drawable.ic_profile, notificationActionText, pendingIntent)

            notificationManager.notify(NOTIFICATION_ID, currentNotification.build())
        }
    }

    /**
     * ?????????????????????? ?????? Android O+
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}