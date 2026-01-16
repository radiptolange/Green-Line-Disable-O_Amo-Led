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
                "syncLines" -> {
                    if (Settings.canDrawOverlays(this)) {
                        val intent = Intent(this, OverlayService::class.java)
                        intent.action = "SYNC"

                        // Pass data to service as a Serializable List of Maps or similar
                        val linesList = call.argument<List<Map<String, Any>>>("lines")

                        // We need to bundle this efficiently. ArrayList<Bundle> is one way.
                        // Or just pass the raw list since it is serializable if it contains basic types.
                        // However, putting List<Map> directly into Intent extras can be tricky.
                        // Let's pack it into a basic Serializable structure or iterate.
                        // Simplest for now: ArrayList of Bundles.

                        val packedList = ArrayList<android.os.Bundle>()
                        linesList?.forEach { map ->
                            val b = android.os.Bundle()
                            b.putInt("id", map["id"] as Int)
                            b.putInt("thickness", map["thickness"] as Int)
                            packedList.add(b)
                        }

                        intent.putParcelableArrayListExtra("lines_data", packedList)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                        result.success("Synced")
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
