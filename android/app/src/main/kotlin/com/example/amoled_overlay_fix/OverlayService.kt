package com.example.amoled_overlay_fix

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager

    // Map of ID -> View
    private val activeOverlays = mutableMapOf<Int, View>()
    // Map of ID -> LayoutParams (to update position)
    private val activeParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    private var colorHex = 0xFF000000.toInt() // Pure Black

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action == "STOP") {
                stopSelf()
                return START_NOT_STICKY
            }

            if (action == "SYNC") {
                val linesData = intent.getParcelableArrayListExtra<Bundle>("lines_data")
                if (linesData != null) {
                    syncLines(linesData)
                }
            }
        }
        return START_STICKY
    }

    private fun syncLines(linesData: ArrayList<Bundle>) {
        val receivedIds = mutableSetOf<Int>()

        for (bundle in linesData) {
            val id = bundle.getInt("id")
            val thickness = bundle.getInt("thickness")
            receivedIds.add(id)

            if (activeOverlays.containsKey(id)) {
                // Update existing
                updateLine(id, thickness)
            } else {
                // Create new
                createLine(id, thickness)
            }
        }

        // Remove lines that are no longer in the list
        val iterator = activeOverlays.keys.iterator()
        while (iterator.hasNext()) {
            val existingId = iterator.next()
            if (!receivedIds.contains(existingId)) {
                // Remove view
                val view = activeOverlays[existingId]
                if (view != null) {
                    windowManager.removeView(view)
                }
                activeParams.remove(existingId)
                iterator.remove()
            }
        }
    }

    private fun createLine(id: Int, thickness: Int) {
        // Create the view (Just a colored Box)
        val overlayView = FrameLayout(this).apply {
            setBackgroundColor(colorHex)
        }

        // Layout Params
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Vertical only as per request
        val width = thickness
        val height = WindowManager.LayoutParams.MATCH_PARENT

        val params = WindowManager.LayoutParams(
            width,
            height,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Default Position (Center-ish horizontally, full vertical)
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 100 // Default offset so they don't all stack exactly on 0
        params.y = 0

        // DRAG LISTENER
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialTouchX = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialTouchX = event.rawX
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Vertical Line: Move X only
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(overlayView, params)
        activeOverlays[id] = overlayView
        activeParams[id] = params
    }

    private fun updateLine(id: Int, thickness: Int) {
        val params = activeParams[id]
        val view = activeOverlays[id]

        if (params != null && view != null) {
            if (params.width != thickness) {
                params.width = thickness
                windowManager.updateViewLayout(view, params)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove all views
        for (view in activeOverlays.values) {
            windowManager.removeView(view)
        }
        activeOverlays.clear()
        activeParams.clear()
    }

    // Boilerplate for Foreground Service (Required for Android 14+)
    private fun startForegroundService() {
        val channelId = "overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AMOLED Fix Running")
            .setContentText("Lines active. Tap to configure.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()

        startForeground(1, notification)
    }
}
