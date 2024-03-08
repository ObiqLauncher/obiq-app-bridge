import 'package:obiq_bridge/models/device_app.dart';

enum DeviceAppEventAction { installed, updated, changed, uninstalled }

class DeviceAppEvent {
  DeviceAppEvent(
    this.action,
    this.packageName, {
    this.app,
  });

  final DeviceAppEventAction action;
  final String packageName;
  final DeviceApp? app;
}
