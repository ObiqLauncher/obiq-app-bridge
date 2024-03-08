package app.obiq.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo

object ObiqBridgeUtils {
    fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    fun startActivityWithNewTask(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
