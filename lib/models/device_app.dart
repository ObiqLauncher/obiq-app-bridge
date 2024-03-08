class DeviceApp {
  DeviceApp(
    this.packageName,
    this.name,
    this.isSystem,
  );

  final String packageName;
  final String name;
  final bool isSystem;

  factory DeviceApp.fromMap(Map<String, dynamic> map) {
    return DeviceApp(
      map['packageName'],
      map['name'],
      map['isSystem'],
    );
  }
}
