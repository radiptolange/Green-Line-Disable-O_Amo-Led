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
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    // State
    private var isVertical = true
    private var thickness = 2
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

            // Update parameters from Flutter
            if (intent.hasExtra("thickness")) {
                thickness = intent.getIntExtra("thickness", 2)
            }
            if (intent.hasExtra("isVertical")) {
                isVertical = intent.getBooleanExtra("isVertical", true)
            }

            // If view exists, update or remove it to redraw
            if (overlayView != null) {
                windowManager.removeView(overlayView)
                overlayView = null // Force recreate
            }
            showOverlay()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        // Create the view (Just a colored Box)
        overlayView = FrameLayout(this).apply {
            setBackgroundColor(colorHex)

            // Add a subtle border/handle so user knows they can grab it (optional)
            // setForeground(resources.getDrawable(R.drawable.border, null))
        }

        // Layout Params
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val width = if (isVertical) thickness else WindowManager.LayoutParams.MATCH_PARENT
        val height = if (isVertical) WindowManager.LayoutParams.MATCH_PARENT else thickness

        params = WindowManager.LayoutParams(
            width,
            height,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Default Position (Top-Left)
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 0
        params.y = 0

        // DRAG LISTENER
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isVertical) {
                            // Vertical Line: Move X only, Keep Y at 0 (Fullscreen)
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = 0
                        } else {
                            // Horizontal Line: Move Y only, Keep X at 0 (Fullscreen)
                            params.x = 0
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                        }
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
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
            .setContentText("Tap to configure")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()

        startForeground(1, notification)
    }
}
