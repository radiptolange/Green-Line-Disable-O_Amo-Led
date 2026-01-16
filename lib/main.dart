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

class LineConfig {
  final int id;
  double thickness;

  LineConfig({required this.id, this.thickness = 2.0});

  Map<String, dynamic> toMap() {
    return {
      "id": id,
      "thickness": thickness.toInt(),
    };
  }
}

class _OverlayControllerState extends State<OverlayController> {
  static const platform = MethodChannel('com.example.amoled/overlay');

  final List<LineConfig> _lines = [];
  int _nextId = 0;
  bool _isServiceRunning = false;

  @override
  void initState() {
    super.initState();
    _checkServiceStatus();
  }

  Future<void> _checkServiceStatus() async {
    // Ideally we would ask the native side if the service is running.
    // For now, we assume it's not running on fresh app start,
    // or we could implement a 'isServiceRunning' method.
    // But since we want to sync state, let's just start with an empty list or
    // maybe we just rely on the user adding lines again.
    // Better: let's try to "sync" with what we have locally.
  }

  Future<void> _checkPermission() async {
    final bool hasPermission = await platform.invokeMethod('checkPermission');
    if (!hasPermission) {
      await platform.invokeMethod('requestPermission');
    }
  }

  Future<void> _syncOverlay() async {
    try {
      if (!_isServiceRunning && _lines.isNotEmpty) {
        await _checkPermission();
      }

      if (_lines.isEmpty) {
         // If no lines, we can technically stop the service,
         // OR we can just clear all overlays and keep service running.
         // The user requirement "background run always" implies we should keep it running
         // as long as there is at least one line, or maybe explicitly toggled.
         // But if there are no lines, there is nothing to show.
         // Let's stop the service if list is empty to save resources,
         // UNLESS the user explicitly wants it "on".
         // But "lines only" implies if I delete all lines, nothing is there.
      }

      final List<Map<String, dynamic>> linesData = _lines.map((l) => l.toMap()).toList();

      await platform.invokeMethod('syncLines', {"lines": linesData});
      setState(() => _isServiceRunning = true);

    } on PlatformException catch (e) {
      print("Failed to sync overlay: '${e.message}'.");
    }
  }

  void _addLine() {
    setState(() {
      _lines.add(LineConfig(id: _nextId++));
    });
    _syncOverlay();
  }

  void _removeLine(int id) {
    setState(() {
      _lines.removeWhere((l) => l.id == id);
    });
    _syncOverlay();
  }

  void _updateLineThickness(int id, double thickness) {
    setState(() {
      final line = _lines.firstWhere((l) => l.id == id);
      line.thickness = thickness;
    });
    _syncOverlay();
  }

  Future<void> _stopAll() async {
    try {
      await platform.invokeMethod('stopOverlay');
      setState(() {
        _isServiceRunning = false;
        _lines.clear();
        _nextId = 0;
      });
    } on PlatformException catch (e) {
      print("Error stopping service: $e");
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
        actions: [
          IconButton(
            icon: const Icon(Icons.power_settings_new),
            color: Colors.red,
            onPressed: _stopAll,
            tooltip: "Stop Service & Clear All",
          )
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            const Text(
              "Vertical Black Lines Only",
              style: TextStyle(color: Colors.white70, fontSize: 16),
            ),
            const SizedBox(height: 10),
            Expanded(
              child: _lines.isEmpty
                  ? const Center(
                      child: Text(
                        "No lines added.\nTap '+' to add a vertical line.",
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.white54),
                      ),
                    )
                  : ListView.builder(
                      itemCount: _lines.length,
                      itemBuilder: (context, index) {
                        final line = _lines[index];
                        return Card(
                          color: Colors.grey[850],
                          margin: const EdgeInsets.only(bottom: 12),
                          child: Padding(
                            padding: const EdgeInsets.all(12.0),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text(
                                      "Line #${line.id + 1}",
                                      style: const TextStyle(
                                          color: Colors.white, fontWeight: FontWeight.bold),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.delete, color: Colors.redAccent),
                                      onPressed: () => _removeLine(line.id),
                                    ),
                                  ],
                                ),
                                Row(
                                  children: [
                                    const Text("Thickness:", style: TextStyle(color: Colors.white70)),
                                    Expanded(
                                      child: Slider(
                                        value: line.thickness,
                                        min: 1,
                                        max: 50,
                                        divisions: 49,
                                        label: line.thickness.round().toString(),
                                        activeColor: Colors.greenAccent,
                                        onChanged: (val) => _updateLineThickness(line.id, val),
                                      ),
                                    ),
                                    Text(
                                      "${line.thickness.toInt()}px",
                                      style: const TextStyle(color: Colors.white),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _addLine,
        backgroundColor: Colors.greenAccent,
        foregroundColor: Colors.black,
        icon: const Icon(Icons.add),
        label: const Text("Add Line"),
      ),
    );
  }
}
