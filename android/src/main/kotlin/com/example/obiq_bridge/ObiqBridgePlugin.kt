package app.obiq.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ObiqBridgePlugin */
class ObiqBridgePlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var context: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var appEventChannel: EventChannel

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "obiq_bridge")
        appEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "obiq_bridge_app_event")

        methodChannel.setMethodCallHandler(this)
        appEventChannel.setStreamHandler(ObiqBridgeAppEventListener(context))
    }

      override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isDefaultLauncher" -> {
                result.success(isDefaultLauncher())
            }
            "getApps" -> {
                val apps = getApps()
                result.success(apps)
            }
            "openApp" -> {
                val packageName: String? = call.argument("packageName")
                if (packageName != null) {
                    openApp(packageName, result)
                } else {
                    result.error("UNAVAILABLE", "Package name not provided.", null)
                }
            }
            "uninstallApp" -> {
                val packageName: String? = call.argument("packageName")
                if (packageName != null) {
                    uninstallApp(packageName)
                    result.success(true)
                } else {
                    result.error("UNAVAILABLE", "Package name not provided.", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
        val packageName = context.packageName
        return defaultLauncherPackage == packageName
    }

    private fun getApps(): List<Map<String, Any>> {
        val packageManager = context.packageManager
        val apps = mutableListOf<Map<String, Any>>()

        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        packages.forEach { applicationInfo ->
            val packageName = applicationInfo.packageName
            val name = packageManager.getApplicationLabel(applicationInfo).toString()
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                val isSystem = ObiqBridgeUtils.isSystemApp(applicationInfo)
                apps.add(
                        mapOf("packageName" to packageName, "name" to name, "isSystem" to isSystem)
                )
            }
        }

        return apps
    }

    private fun openApp(packageName: String, result: Result) {
        val packageManager = context.packageManager

        // Check if the app is installed
        val appInstalled =
                try {
                    packageManager.getApplicationInfo(packageName, 0)
                    true // Application found
                } catch (e: PackageManager.NameNotFoundException) {
                    false // Application not found
                }

        if (appInstalled) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                ObiqBridgeUtils.startActivityWithNewTask(context, launchIntent)

                result.success(true) // App was successfully launched
            } else {
                result.error("NOT_FOUND", "App not found for package name: $packageName", null)
            }
        } else {
            result.error("NOT_INSTALLED", "App not installed for package name: $packageName", null)
        }
    }

    private fun uninstallApp(packageName: String) {
        var intent = Intent(Intent.ACTION_DELETE)

        intent.setData(Uri.parse("package:$packageName"))

        ObiqBridgeUtils.startActivityWithNewTask(context, intent)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appEventChannel.setStreamHandler(null)
        methodChannel.setMethodCallHandler(null)
    }
}
