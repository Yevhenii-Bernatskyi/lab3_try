package com.example.lab3_try

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.lab3_try.ui.theme.Lab3_tryTheme // Твоя тема
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.List

import androidx.compose.runtime.setValue // для mutableStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel



class MainActivity : ComponentActivity() {

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach { entry ->
                if (!entry.value) {
                    allGranted = false
                    Log.w("MainActivity", "Дозвіл ${entry.key} не надано")
                }
            }
            if (allGranted) {
                Log.d("MainActivity", "Всі запитані дозволи надано")
            } else {
                Log.w("MainActivity", "Не всі запитані дозволи надано. Користувач має надати їх вручну.")
                // Тут можна показати Snackbar або діалог з проханням надати дозволи в налаштуваннях
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Залишаємо твій виклик

        // Ініціалізація OSMDroid (робити перед setContentView або використанням MapView)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContent {
            Lab3_tryTheme { // Використовуємо твою тему
                LocationTrackerScreen()
            }
        }
        checkAndRequestPermissions() // Запит дозволів при старті
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Для Android 14 (API 34) і вище для Foreground Service типу location
        if (Build.VERSION.SDK_INT >= 34) { // Build.VERSION_CODES.UPSIDE_DOWN_CAKE наразі може не бути в стабільній версії SDK
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d("MainActivity", "Запит дозволів: $permissionsToRequest")
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("MainActivity", "Всі необхідні дозволи вже надано")
        }
    }

    private fun sendCommandToService(action: String) {
        if (!arePermissionsSufficientForService()) {
            Log.w("MainActivity", "Спроба відправити команду сервісу без достатніх дозволів.")
            checkAndRequestPermissions() // Запросити дозволи знову
            // Можна показати Snackbar або Toast
            // Toast.makeText(this, "Будь ласка, надайте необхідні дозволи для запуску сервісу", Toast.LENGTH_LONG).show()
            return
        }

        Intent(this, LocationService::class.java).also {
            it.action = action
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            } else {
                startService(it)
            }
        }
    }

    // Перевіряє дозволи, необхідні саме для запуску LocationService
    private fun arePermissionsSufficientForService(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val postNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val foregroundLocationGranted = if (Build.VERSION.SDK_INT >= 34) { // UPSIDE_DOWN_CAKE
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        Log.d("MainActivityPermissions", "FineLoc: $fineLocationGranted, PostNotif: $postNotificationsGranted, FGSLoc: $foregroundLocationGranted")
        return fineLocationGranted && postNotificationsGranted && foregroundLocationGranted
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LocationTrackerScreen(tracksViewModel: TracksViewModel = viewModel()) {
        val context = LocalContext.current
        val isTracking by LocationService.isTracking.observeAsState(initial = false)

        val mapView = remember {
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                val startPoint = GeoPoint(48.9226, 24.7111) // Івано-Франківськ
                controller.setCenter(startPoint)
            }
        }

        var currentPolyline by remember { mutableStateOf<Polyline?>(null) } // Для малювання треку

        // Стан для відображення діалогу зі списком треків
        var showTracksDialog by remember { mutableStateOf(false) }

        // Спостерігаємо за вибраним треком з ViewModel
        val selectedTrackPoints by tracksViewModel.selectedTrackPoints.observeAsState()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(title = { Text("Трекер Місцезнаходження") })
            },
            floatingActionButton = {
                Row {
                    FloatingActionButton(onClick = {
                        if (isTracking) {
                            sendCommandToService(LocationService.ACTION_STOP_TRACKING)
                        } else {
                            if (arePermissionsSufficientForService()) {
                                sendCommandToService(LocationService.ACTION_START_TRACKING)
                            } else {
                                checkAndRequestPermissions()
                                Log.i("MainActivity", "Запит дозволів перед стартом трекінгу.")
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isTracking) "Зупинити запис" else "Почати запис"
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FloatingActionButton(onClick = {
                        tracksViewModel.clearSelectedTrack() // Очистимо попередній вибраний трек на карті
                        currentPolyline = drawPathOnMap(mapView, emptyList(), currentPolyline) // Очистимо карту
                        showTracksDialog = true // Показати діалог
                    }) {
                        Icon(Icons.Filled.List, "Збережені треки")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding) // Відступи від Scaffold
                    .padding(horizontal = 16.dp), // Горизонтальні відступи для всього контенту Column
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Текст статусу (завжди зверху)
                Text(
                    text = if (isTracking) "Запис треку: Активний" else "Запис треку: Зупинено",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp) // Відступ зверху
                )

                // 2. Карта OSMDroid (фіксована висота)
//                AndroidView(
//                    factory = { mapView },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(400.dp) // ЗАДАЄМО ФІКСОВАНУ ВИСОТУ КАРТІ
//                        .padding(vertical = 16.dp)
//                        .clipToBounds() // Вертикальні відступи для карти
//                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp) // Або твоє значення
                        .padding(vertical = 16.dp)
                        .clipToBounds() // <--- ДОДАЙ ЦЕ
                ) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Розширюваний простір, який "відштовхне" кнопку вниз
                Spacer(modifier = Modifier.weight(1f))

                // 4. Кнопка "Налаштування дозволів" (завжди знизу)
                Button(
                    onClick = {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
                            val packageNameValue = context.packageName
                            it.data = Uri.fromParts("package", packageNameValue, null)
                            context.startActivity(it)
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp) // Відступ знизу
                ) {
                    Text("Налаштування дозволів")
                }
            }
        }

        if (showTracksDialog) {
            Dialog(onDismissRequest = { showTracksDialog = false }) { // Dialog замість AlertDialog для більшої гнучкості
                TracksListScreen(
                    tracksViewModel = tracksViewModel,
                    onTrackSelected = { track ->
                        tracksViewModel.loadTrackPoints(track.id)
                        showTracksDialog = false // Закрити діалог після вибору
                    },
                    onDismiss = { showTracksDialog = false }
                )
            }
        }

        // Ефект для малювання вибраного треку на карті
        LaunchedEffect(selectedTrackPoints) {
            selectedTrackPoints?.let { points ->
                val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
                currentPolyline = drawPathOnMap(mapView, geoPoints, currentPolyline)
                if (geoPoints.isNotEmpty()) {
                    // Опціонально: перемістити карту, щоб показати весь трек
                    // mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), true, 100)
                    // Або просто до першої точки
                    mapView.controller.animateTo(geoPoints.first())
                } else {
                    // Якщо точок немає (наприклад, трек очищено), прибираємо лінію
                    currentPolyline = drawPathOnMap(mapView, emptyList(), currentPolyline)
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                Log.d("MapViewLifecycle", "MapView onDetach called")
                mapView.onDetach()
            }
        }

    }

    private fun drawPathOnMap(map: MapView, geoPoints: List<GeoPoint>, oldPolyline: Polyline?): Polyline? {
        oldPolyline?.let { map.overlays.remove(it); map.invalidate() }

        if (geoPoints.size < 2) {
            // Якщо стара лінія була, але нових точок немає, просто повертаємо null,
            // стара лінія вже видалена.
            return null
        }

        val newPolyline = Polyline()
        newPolyline.setPoints(geoPoints)
        // Встанови колір та товщину перед додаванням на карту
        newPolyline.outlinePaint.color = android.graphics.Color.RED // Використовуй android.graphics.Color
        newPolyline.outlinePaint.strokeWidth = 8f

        map.overlays.add(newPolyline)
        map.invalidate() // Перемалювати карту
        return newPolyline
    }
}