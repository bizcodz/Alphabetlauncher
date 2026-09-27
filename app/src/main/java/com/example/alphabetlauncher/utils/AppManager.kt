package com.example.alphabetlauncher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.alphabetlauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AppManager(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        
        val apps = resolveInfoList.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val packageName = resolveInfo.activityInfo.packageName
            val icon = resolveInfo.loadIcon(packageManager)
            
            // Exclude our own app
            if (packageName != context.packageName) {
                AppInfo(
                    name = appName,
                    packageName = packageName,
                    icon = icon
                )
            } else {
                null
            }
        }
        
        // Sort alphabetically
        apps.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
}