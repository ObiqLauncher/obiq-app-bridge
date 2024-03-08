package app.obiq.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import io.flutter.plugin.common.EventChannel

class ObiqBridgeAppEventListener(private val context: Context) : EventChannel.StreamHandler {

    private var eventSink: EventChannel.EventSink? = null
    private val appsBroadcastReceiver = createBroadcastReceiver()

    private fun createBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val packageName = intent?.data?.schemeSpecificPart ?: return
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> handlePackageAdded(packageName)
                    Intent.ACTION_PACKAGE_REPLACED -> handlePackageReplaced(packageName)
                    Intent.ACTION_PACKAGE_CHANGED -> handlePackageChanged(packageName)
                    Intent.ACTION_PACKAGE_REMOVED -> handlePackageRemoved(packageName, intent)
                }
            }
        }
    }

    private fun handlePackageAdded(packageName: String) {
        val appData = getAppData(packageName)
        eventSink?.success(appData.apply { put("action", "installed") })
    }

    private fun handlePackageReplaced(packageName: String) {
        val appData = getAppData(packageName)
        eventSink?.success(appData.apply { put("action", "updated") })
    }

    private fun handlePackageChanged(packageName: String) {
        val appData = getAppData(packageName)
        eventSink?.success(appData.apply { put("action", "changed") })
    }

    private fun handlePackageRemoved(packageName: String, intent: Intent) {
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        if (!replacing) {
            eventSink?.success(mapOf("action" to "uninstalled", "packageName" to packageName))
        }
    }

    private fun getAppData(packageName: String): HashMap<String, Any?> {
        val packageManager = context.packageManager
        val appInfo =
                try {
                    packageManager.getApplicationInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
        val appName =
                appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: "Unknown"
        val isSystem = appInfo?.let { ObiqBridgeUtils.isSystemApp(it) } ?: false

        return hashMapOf("packageName" to packageName, "name" to appName, "isSystem" to isSystem)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        this.eventSink = events
        val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addDataScheme("package")
                }
        context.registerReceiver(appsBroadcastReceiver, filter)
    }

    override fun onCancel(arguments: Any?) {
        context.unregisterReceiver(appsBroadcastReceiver)
        eventSink = null
    }
}
