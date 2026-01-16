package com.example.amoled_overlay_fix

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.amoled/overlay"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "checkPermission" -> {
                    result.success(Settings.canDrawOverlays(this))
                }
                "requestPermission" -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, 1234)
                    result.success(true)
                }
                "startOverlay" -> {
                    if (Settings.canDrawOverlays(this)) {
                        val intent = Intent(this, OverlayService::class.java)

                        // Pass data to service
                        val thickness = call.argument<Int>("thickness") ?: 2
                        val isVertical = call.argument<Boolean>("isVertical") ?: true

                        intent.putExtra("thickness", thickness)
                        intent.putExtra("isVertical", isVertical)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                        result.success("Overlay Started")
                    } else {
                        result.error("PERMISSION_DENIED", "Overlay permission not granted", null)
                    }
                }
                "stopOverlay" -> {
                    val intent = Intent(this, OverlayService::class.java)
                    intent.action = "STOP"
                    startService(intent) // Triggers onStartCommand with STOP action
                    result.success("Overlay Stopped")
                }
                else -> result.notImplemented()
            }
        }
    }
}
