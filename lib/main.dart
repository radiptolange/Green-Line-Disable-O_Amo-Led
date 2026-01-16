import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MaterialApp(home: OverlayController()));
}

class OverlayController extends StatefulWidget {
  const OverlayController({super.key});

  @override
  State<OverlayController> createState() => _OverlayControllerState();
}

class _OverlayControllerState extends State<OverlayController> {
  static const platform = MethodChannel('com.example.amoled/overlay');

  bool _isOverlayActive = false;
  bool _isVertical = true;
  double _thickness = 2.0;

  Future<void> _checkPermission() async {
    final bool hasPermission = await platform.invokeMethod('checkPermission');
    if (!hasPermission) {
      await platform.invokeMethod('requestPermission');
    }
  }

  Future<void> _toggleOverlay() async {
    try {
      if (_isOverlayActive) {
        await platform.invokeMethod('stopOverlay');
        setState(() => _isOverlayActive = false);
      } else {
        await _checkPermission();
        // Send config map
        await platform.invokeMethod('startOverlay', {
          "thickness": _thickness.toInt(),
          "isVertical": _isVertical,
        });
        setState(() => _isOverlayActive = true);
      }
    } on PlatformException catch (e) {
      print("Failed to toggle overlay: '${e.message}'.");
    }
  }

  // Called when slider moves to update overlay in real-time
  Future<void> _updateOverlayConfig() async {
    if (_isOverlayActive) {
      // Restarting works as an "update" because of how we wrote the Service
      await platform.invokeMethod('startOverlay', {
        "thickness": _thickness.toInt(),
        "isVertical": _isVertical,
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[900],
      appBar: AppBar(
        title: const Text("AMOLED Line Fixer"),
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Status Indicator
            Icon(
              _isOverlayActive ? Icons.layers : Icons.layers_clear,
              size: 80,
              color: _isOverlayActive ? Colors.greenAccent : Colors.grey,
            ),
            const SizedBox(height: 20),
            Text(
              _isOverlayActive ? "Overlay Active" : "Overlay Inactive",
              style: const TextStyle(color: Colors.white, fontSize: 24),
            ),
            const SizedBox(height: 40),

            // Controls
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.grey[850],
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  const Text("Thickness (pixels)", style: TextStyle(color: Colors.white70)),
                  Slider(
                    value: _thickness,
                    min: 1,
                    max: 20,
                    divisions: 19,
                    label: _thickness.round().toString(),
                    activeColor: Colors.greenAccent,
                    onChanged: (value) {
                      setState(() => _thickness = value);
                      _updateOverlayConfig(); // Live update
                    },
                  ),
                  const SizedBox(height: 10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text("Orientation:", style: TextStyle(color: Colors.white)),
                      ToggleButtons(
                        isSelected: [_isVertical, !_isVertical],
                        onPressed: (index) {
                          setState(() => _isVertical = index == 0);
                          _updateOverlayConfig();
                        },
                        color: Colors.white,
                        selectedColor: Colors.black,
                        fillColor: Colors.greenAccent,
                        borderRadius: BorderRadius.circular(8),
                        children: const [
                          Padding(padding: EdgeInsets.symmetric(horizontal: 16), child: Text("Vertical")),
                          Padding(padding: EdgeInsets.symmetric(horizontal: 16), child: Text("Horizontal")),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),

            const Spacer(),

            // Main Action Button
            SizedBox(
              width: double.infinity,
              height: 55,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: _isOverlayActive ? Colors.redAccent : Colors.greenAccent,
                  foregroundColor: Colors.black,
                ),
                onPressed: _toggleOverlay,
                child: Text(
                  _isOverlayActive ? "TURN OFF" : "ENABLE OVERLAY",
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              "Drag the line on screen to position it.",
              style: TextStyle(color: Colors.white54, fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}
