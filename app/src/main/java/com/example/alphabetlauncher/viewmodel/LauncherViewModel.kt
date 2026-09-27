package com.example.alphabetlauncher.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphabetlauncher.model.AppInfo
import com.example.alphabetlauncher.utils.AppManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appManager = AppManager(application)
    private val prefs: SharedPreferences = application.getSharedPreferences("alphabet_launcher_prefs", Context.MODE_PRIVATE)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _groupedApps = MutableStateFlow<Map<Char, List<AppInfo>>>(emptyMap())
    val groupedApps: StateFlow<Map<Char, List<AppInfo>>> = _groupedApps.asStateFlow()

    private val _favoriteApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val favoriteApps: StateFlow<List<AppInfo>> = _favoriteApps.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action in listOf(Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_CHANGED)) {
                loadApps()
            }
        }
    }

    init {
        loadApps()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appManager.getInstalledApps()
            _installedApps.value = apps
            
            // Group by starting letter (A-Z only, put others in '#')
            val grouped = apps.groupBy { app ->
                val firstChar = app.name.firstOrNull()?.uppercaseChar() ?: '#'
                if (firstChar in 'A'..'Z') firstChar else '#'
            }
            _groupedApps.value = grouped
            
            // Load favorites from SharedPreferences
            val savedFavorites = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
            if (savedFavorites.isNotEmpty()) {
                val favApps = apps.filter { it.packageName in savedFavorites }
                _favoriteApps.value = favApps.ifEmpty { apps.take(6) }
            } else {
                _favoriteApps.value = apps.take(6)
            }
        }
    }
    
    fun toggleFavorite(appInfo: AppInfo) {
        val currentFavorites = _favoriteApps.value.toMutableList()
        val savedFavorites = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        if (currentFavorites.any { it.packageName == appInfo.packageName }) {
            currentFavorites.removeAll { it.packageName == appInfo.packageName }
            savedFavorites.remove(appInfo.packageName)
        } else {
            currentFavorites.add(appInfo)
            savedFavorites.add(appInfo.packageName)
        }
        
        prefs.edit().putStringSet("favorites", savedFavorites).apply()
        _favoriteApps.value = currentFavorites
    }

    fun launchApp(packageName: String) {
        val context = getApplication<Application>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}