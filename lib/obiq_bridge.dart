import 'package:flutter/services.dart';
import 'package:obiq_bridge/models/device_app.dart';
import 'package:obiq_bridge/models/device_app_event.dart';

class ObiqBridge {
  static const _methodChannel = MethodChannel('obiq_bridge');
  static const _appEventChannel = EventChannel('obiq_bridge_app_event');

  // Check if the Obiq launcher is the default one.
  static Future<bool> isDefaultLauncher() async {
    final bool isDefault = await _methodChannel.invokeMethod('isDefaultLauncher');

    return isDefault;
  }

  /// Get a list of the installed applications.
  static Future<List<DeviceApp>> getApps() async {
    final List<dynamic> localApps = await _methodChannel.invokeMethod('getApps');

    return localApps
        .map(
          (appMap) => DeviceApp.fromMap(
            Map<String, dynamic>.from(appMap),
          ),
        )
        .toList();
  }

  // Open an app.
  static Future<bool> openApp(String packageName) async {
    final bool wasOpened = await _methodChannel.invokeMethod('openApp', {'packageName': packageName});

    return wasOpened;
  }

  /// The bool is whether the attempt to uninstall succeeded. It does not indicate whether the uninstall actually succeeded.
  static Future<bool> uninstallApp(String packageName) async {
    final bool uninstallAttempt = await _methodChannel.invokeMethod('uninstallApp', {'packageName': packageName});

    return uninstallAttempt;
  }

  /// Listen to events for when an app is installed, updated, changed or uninstalled.
  static Stream<DeviceAppEvent> listenToAppEvents() {
    return _appEventChannel.receiveBroadcastStream().map((dynamic event) {
      final Map<String, dynamic> deviceApp = Map<String, dynamic>.from(event);

      final DeviceAppEventAction action = DeviceAppEventAction.values.firstWhere(
        (e) => e.toString().split('.').last == deviceApp['action'] as String,
        orElse: () => DeviceAppEventAction.uninstalled,
      );

      DeviceApp? app;
      try {
        app = DeviceApp.fromMap(deviceApp);
      } catch (e) {
        // Couldn't map. Just continue.
      }

      return DeviceAppEvent(action, deviceApp['packageName'], app: app);
    });
  }
}
