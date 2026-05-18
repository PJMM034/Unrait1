package com.example.unrait.ui.screens.ride

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// Imports de Google Maps y Localización
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.Polyline
import com.example.unrait.ui.screens.ride.SeguimientoViajeScreen // <--- ¡AGREGA ESTA LÍNEA!
// Colores de tu tema
val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val BackgroundGray = Color(0xFFF5F6F8)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedirRaiteScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- COORDENADAS EXACTAS CORREGIDAS ---
    val ubicacionesFijas = remember {
        mapOf(
            "ITSCC" to LatLng(25.044167567667646, -111.63924349196883),
            "Cd. Insurgentes" to LatLng(25.2625, -111.7753),
            "Villa Morelos" to LatLng(24.931200016117486, -111.62658273766641),
            "Pto. San Carlos" to LatLng(24.7867, -112.1058)
        )
    }

    // --- ESTADOS PARA PERMISOS ---
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    // --- ESTADOS DEL MAPA Y GEOLOCALIZACIÓN ---
    val destinoMarkerState = rememberMarkerState()
    var destinoSeleccionado by remember { mutableStateOf(false) }
    var direccionTexto by remember { mutableStateOf("Toca el mapa para marcar el destino") }

    val cameraPositionState = rememberCameraPositionState {
        // Iniciamos la cámara en el ITSCC por defecto
        position = CameraPosition.fromLatLngZoom(ubicacionesFijas["ITSCC"]!!, 15f)
    }

    // Función para centrar la cámara en el punto azul (Usuario real)
    val centrarEnMiUbicacion = {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val miPosicion = LatLng(location.latitude, location.longitude)
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(miPosicion, 16f))
                    }
                }
            }
        }
    }

    // Efecto para pedir permisos al abrir la pantalla y centrar en el usuario
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            // Si ya tiene permiso, centramos automáticamente en su ubicación
            centrarEnMiUbicacion()
        }
    }

    // Efecto para traducir las coordenadas a nombre de calle cuando el pin se mueve
    LaunchedEffect(destinoMarkerState.position) {
        if (destinoSeleccionado) {
            direccionTexto = "Buscando dirección..."
            scope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val pos = destinoMarkerState.position
                    val addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        // Tratamos de obtener la calle y colonia
                        val nombreCalle = address.thoroughfare
                        val colonia = address.subLocality ?: address.locality

                        val direccionFinal = if (nombreCalle != null && colonia != null) {
                            "$nombreCalle, $colonia"
                        } else {
                            address.getAddressLine(0) ?: "Dirección seleccionada"
                        }

                        withContext(Dispatchers.Main) {
                            direccionTexto = direccionFinal
                        }
                    } else {
                        withContext(Dispatchers.Main) { direccionTexto = "Ubicación en el mapa" }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { direccionTexto = "Coordenada seleccionada" }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedir Raite", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // --- GOOGLE MAP INTERACTIVO ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Muestra el botón por defecto de Google Maps y el punto azul
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                onMapClick = { latLng ->
                    destinoMarkerState.position = latLng
                    destinoSeleccionado = true
                }
            ) {
                // Marcador Fijo del ITSCC
                Marker(
                    state = rememberMarkerState(position = ubicacionesFijas["ITSCC"]!!),
                    title = "ITSCC",
                    snippet = "Instituto Tecnológico Superior de Ciudad Constitución"
                )

                // El Marcador Rojo del destino SOLO aparece si tocaste el mapa o elegiste un chip
                if (destinoSeleccionado) {
                    Marker(
                        state = destinoMarkerState,
                        title = "Tu Destino",
                        snippet = "Mantén presionado para arrastrar",
                        draggable = true // ¡Permite arrastrar para corregir!
                    )
                }
            }

            // --- INTERFAZ FLOTANTE SUPERIOR ---
            Column(modifier = Modifier.align(Alignment.TopCenter)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // BOTÓN DE MI UBICACIÓN
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { centrarEnMiUbicacion() } // Al tocar, centra el mapa en el usuario
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Origen", tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Centrar en mi ubicación", fontSize = 14.sp, color = NavyBlue, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 32.dp), color = Color.LightGray)

                        // INDICADOR DE DESTINO (Muestra la calle real)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Destino", tint = Color.Red, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = direccionTexto, // Aquí sale la calle y colonia o la instrucción
                                fontSize = if (destinoSeleccionado) 14.sp else 13.sp,
                                color = if (destinoSeleccionado) NavyBlue else Color.Gray,
                                fontWeight = if (destinoSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // FILA DE CHIPS (Localidades)
                ScrollableTabRow(
                    selectedTabIndex = -1,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    ubicacionesFijas.forEach { (nombre, coords) ->
                        SuggestionChip(
                            onClick = {
                                destinoMarkerState.position = coords
                                destinoSeleccionado = true
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(coords, 16f))
                                }
                            },
                            label = { Text(nombre) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color.White,
                                labelColor = NavyBlue
                            )
                        )
                    }
                }
            }

            // --- TARJETA INFERIOR ---
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Opciones de Raite", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, OrangePrimary, RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = "Auto", tint = OrangePrimary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Raite Estándar", fontWeight = FontWeight.Bold, color = NavyBlue)
                                Text("Donativo voluntario", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Sugerido", fontSize = 10.sp, color = Color.Gray)
                            Text("$15 MXN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NavyBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { com.example.unrait.mostrarPantalla.value = "detalles_viaje" },
                        enabled = destinoSeleccionado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyBlue,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            if (!destinoSeleccionado) "SELECCIONA UN DESTINO" else "SOLICITAR RAITE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallesViajeScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Viaje", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // TARJETA DEL CONDUCTOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TU CONDUCTOR", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E5EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Mario Gomez", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Text(" 4.9 ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("(120 viajes)", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                        }

                        IconButton(
                            onClick = { /* Llamar */ },
                            modifier = Modifier
                                .background(OrangePrimary, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = "Llamar", tint = Color.White)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("VEHÍCULO", fontSize = 10.sp, color = Color.Gray)
                            Text("Toyota Corolla", fontWeight = FontWeight.Medium, color = NavyBlue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PLACA", fontSize = 10.sp, color = Color.Gray)
                            Text("5432-XYZ", fontWeight = FontWeight.Bold, color = NavyBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TARJETA DE RUTA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("PUNTO DE ORIGEN", fontSize = 10.sp, color = Color.Gray)
                            Text("ITSCC, Prof. Marcelo Rubio Ruiz", fontSize = 14.sp, color = NavyBlue, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Box(modifier = Modifier
                        .padding(start = 11.dp, top = 8.dp, bottom = 8.dp)
                        .width(2.dp)
                        .height(24.dp)
                        .background(Color.LightGray))

                    Row {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("DESTINO FINAL", fontSize = 10.sp, color = Color.Gray)
                            Text("Destino seleccionado en mapa", fontSize = 14.sp, color = NavyBlue, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TARJETA DONATIVO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Donativo Sugerido", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Text("$15 MXN", color = OrangePrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTÓN CANCELAR
            Button(
                onClick = { com.example.unrait.mostrarPantalla.value = "main" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("CANCELAR VIAJE", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

enum class PassengerTripState {
    ESPERANDO_CONDUCTOR,
    CONDUCTOR_LLEGO,
    EN_VIAJE,
    PANIC_MODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoViajeScreen(onFinalizar: () -> Unit) {
    var tripState by remember { mutableStateOf(PassengerTripState.ESPERANDO_CONDUCTOR) }

    // Ubicaciones simuladas
    val miPosicion = LatLng(25.2650, -111.7700) // Donde está esperando el estudiante
    val posicionConductor = LatLng(25.2625, -111.7753) // Conductor acercándose
    val itscc = LatLng(25.044167, -111.639243) // Destino final

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(miPosicion, 14f)
    }

    // Simular que el conductor llega después de 5 segundos
    LaunchedEffect(tripState) {
        if (tripState == PassengerTripState.ESPERANDO_CONDUCTOR) {
            kotlinx.coroutines.delay(5000)
            tripState = PassengerTripState.CONDUCTOR_LLEGO
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when(tripState) {
                                PassengerTripState.ESPERANDO_CONDUCTOR -> "Conductor en camino"
                                PassengerTripState.CONDUCTOR_LLEGO -> "¡Tu conductor llegó!"
                                PassengerTripState.EN_VIAJE -> "Viaje en curso"
                                PassengerTripState.PANIC_MODE -> "EMERGENCIA SOS"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (tripState == PassengerTripState.ESPERANDO_CONDUCTOR) {
                            Text("Llega en aprox. 3 min", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onFinalizar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (tripState == PassengerTripState.PANIC_MODE) Color.Red else NavyBlue
                )
            )
        },
        floatingActionButton = {
            // BOTÓN DE PÁNICO DEL PASAJERO
            FloatingActionButton(
                onClick = { tripState = PassengerTripState.PANIC_MODE },
                containerColor = Color.Red,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Pánico", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- MAPA DE SEGUIMIENTO ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                Marker(state = rememberMarkerState(position = miPosicion), title = "Mi Ubicación")

                if (tripState == PassengerTripState.ESPERANDO_CONDUCTOR || tripState == PassengerTripState.CONDUCTOR_LLEGO) {
                    Marker(state = rememberMarkerState(position = posicionConductor), title = "Conductor (Mario)")
                    // Ruta del conductor hacia mí
                    Polyline(points = listOf(posicionConductor, miPosicion), color = NavyBlue, width = 12f)
                }

                if (tripState == PassengerTripState.EN_VIAJE) {
                    Marker(state = rememberMarkerState(position = itscc), title = "ITSCC (Destino)")
                    // Ruta de nosotros hacia el TEC
                    Polyline(points = listOf(miPosicion, itscc), color = OrangePrimary, width = 12f)
                }
            }

            // --- TARJETA DEL CONDUCTOR (Abajo) ---
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 24.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    if (tripState == PassengerTripState.PANIC_MODE) {
                        Text("⚠️ PROTOCOLO DE SEGURIDAD", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Ubicación enviada a contactos de emergencia.", color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
                        Button(onClick = { /* Lógica 911 */ }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("LLAMAR A SEGURIDAD", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { tripState = PassengerTripState.EN_VIAJE }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("Falsa Alarma", color = Color.Gray)
                        }
                    } else {
                        // Info del Conductor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE0E5EC)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Mario Gomez", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Toyota Corolla • ", fontSize = 12.sp, color = Color.Gray)
                                        Text("CZ-4521-B", fontSize = 12.sp, color = NavyBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Botón de WhatsApp
                            IconButton(
                                onClick = { /* Abrir WhatsApp */ },
                                modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(40.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Código de seguridad (Clave para carpooling seguro)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Código de abordaje:", color = NavyBlue, fontSize = 14.sp)
                            Text("8421", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = OrangePrimary, letterSpacing = 2.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Lógica del Botón Principal
                        when (tripState) {
                            PassengerTripState.ESPERANDO_CONDUCTOR -> {
                                OutlinedButton(
                                    onClick = onFinalizar,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) { Text("Cancelar Solicitud") }
                            }
                            PassengerTripState.CONDUCTOR_LLEGO -> {
                                Button(
                                    onClick = { tripState = PassengerTripState.EN_VIAJE },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) { Text("YA ESTOY A BORDO", fontWeight = FontWeight.Bold, color = Color.White) }
                            }
                            PassengerTripState.EN_VIAJE -> {
                                Button(
                                    onClick = { /* El conductor finaliza el viaje, no el pasajero, pero simulamos salida aquí */ onFinalizar() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                                ) { Text("Compartir mi ruta en vivo", fontWeight = FontWeight.Bold, color = Color.White) }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}