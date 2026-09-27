package com.example.alphabetlauncher.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alphabetlauncher.ui.components.AlphabetBar
import com.example.alphabetlauncher.ui.components.AppItem
import com.example.alphabetlauncher.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val groupedApps by viewModel.groupedApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var isInteractingWithBar by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf('A') }
    var isSearching by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = isSearching || isInteractingWithBar) {
        if (isSearching) {
            isSearching = false
            viewModel.updateSearchQuery("")
            keyboardController?.hide()
        } else if (isInteractingWithBar) {
            isInteractingWithBar = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                var startY = 0f
                var isTracking = false
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointer = event.changes.firstOrNull() ?: continue
                        
                        // Ignore touches on the right edge (Alphabet bar) unless searching
                        if (!isSearching && pointer.position.x > size.width - 150f) {
                            isTracking = false
                            continue
                        }

                        if (event.changes.any { it.pressed } && !isTracking) {
                            startY = pointer.position.y
                            isTracking = true
                        }
                        
                        if (isTracking && event.changes.any { it.pressed }) {
                            val dy = pointer.position.y - startY
                            if (isSearching) {
                                // Swipe down detected while searching to dismiss
                                if (dy > 80f) {
                                    isSearching = false
                                    viewModel.updateSearchQuery("")
                                    keyboardController?.hide()
                                    isTracking = false
                                }
                            } else {
                                // Swipe up detected (negative dy)
                                if (dy < -80f) {
                                    isSearching = true
                                    isTracking = false
                                }
                            }
                        }
                        
                        if (event.changes.all { !it.pressed }) {
                            isTracking = false
                        }
                    }
                }
            }
    ) {
        
        AnimatedVisibility(
            visible = isSearching,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 250)
            ) + fadeIn(animationSpec = tween(durationMillis = 250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 200)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            modifier = Modifier.zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 48.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search apps...") },
                    shape = RoundedCornerShape(100),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    leadingIcon = { 
                        IconButton(onClick = { 
                            isSearching = false
                            viewModel.updateSearchQuery("")
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val filteredApps = installedApps.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) 
                }
                
                LazyColumn {
                    items(filteredApps) { app ->
                        AppItem(
                            app = app,
                            onClick = { viewModel.launchApp(app.packageName) },
                            onLongClick = { viewModel.toggleFavorite(app) }
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = !isSearching,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            if (isInteractingWithBar) {
                // Show filtered apps by alphabet
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 40.dp)
                ) {
                    Text(
                        text = selectedLetter.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(24.dp)
                    )
                    
                    val appsToShow = groupedApps[selectedLetter] ?: emptyList()
                    
                    if (appsToShow.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No apps",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn {
                            items(appsToShow) { app ->
                                AppItem(
                                    app = app,
                                    onClick = { viewModel.launchApp(app.packageName) },
                                    onLongClick = { viewModel.toggleFavorite(app) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Resting state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, top = 64.dp)
                ) {
                    ClockAndDate()
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    LazyColumn {
                        items(favoriteApps) { app ->
                            AppItem(
                                app = app,
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = { viewModel.toggleFavorite(app) }
                            )
                        }
                    }
                }
            }
        }

        // Alphabet Bar pinned to the right
        if (!isSearching) {
            AlphabetBar(
                modifier = Modifier.align(Alignment.CenterEnd),
                availableLetters = groupedApps.keys,
                onLetterSelected = { selectedLetter = it },
                onInteractionStart = { isInteractingWithBar = true },
                onInteractionEnd = { isInteractingWithBar = false }
            )
        }
    }
}

@Composable
fun ClockAndDate() {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf(formatTime()) }
    var dateText by remember { mutableStateOf(formatDate()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_TIME_TICK) {
                    timeText = formatTime()
                    dateText = formatDate()
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column {
        Text(
            text = timeText,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = dateText,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

private fun formatTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

private fun formatDate(): String {
    val sdf = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    return sdf.format(Date())
}