package com.example.unrait.ui.screens.ride

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.unrait.network.PeticionPasajeroRequest
import com.example.unrait.network.UnraitApi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedirRaiteScreen(onBack: () -> Unit, onSolicitudEnviada: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Variables de estado
    var origen by remember { mutableStateOf("Buscando tu ubicación...") }
    var destino by remember { mutableStateOf("") }
    var miUbicacionLatLng by remember { mutableStateOf<LatLng?>(null) }
    var marcadorDestino by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val defaultLocation = LatLng(25.044167, -111.639243) // ITSCC
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Permisos de ubicación
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    // Obtener ubicación actual al abrir la pantalla
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    miUbicacionLatLng = currentLatLng
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }
                    // Geocoding inverso para obtener el nombre de la calle/ciudad
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            origen = addresses[0].thoroughfare ?: addresses[0].locality ?: "Mi Ubicación Actual"
                        } else {
                            origen = "Ubicación detectada por GPS"
                        }
                    } catch (e: Exception) {
                        origen = "Ubicación detectada por GPS"
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pedir Raite", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = NavyBlue)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 1. EL MAPA INTERACTIVO
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                onMapClick = { latLng ->
                    // Al tocar el mapa, se pone el marcador rojo de destino
                    marcadorDestino = latLng
                    destino = "Ubicación seleccionada en mapa"
                }
            ) {
                // Marcador Rojo de Destino
                marcadorDestino?.let { pos ->
                    Marker(
                        state = rememberMarkerState(position = pos),
                        title = "Destino",
                        snippet = "Punto de llegada"
                    )
                }
            }

            // 2. CAJA SUPERIOR FLOTANTE (Indicaciones)
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(origen, fontWeight = FontWeight.Medium, color = NavyBlue)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (destino.isEmpty()) "Toca el mapa para marcar el destino" else destino,
                            color = if (destino.isEmpty()) Color.Gray else NavyBlue,
                            fontWeight = if (destino.isEmpty()) FontWeight.Normal else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. BOTONES RÁPIDOS DE DESTINO
            val destinosRapidos = listOf(
                "ITSCC" to LatLng(25.044167, -111.639243),
                "Cd. Insurgentes" to LatLng(25.2625, -111.7753),
                "Villa Morelos" to LatLng(25.1054, -111.6888),
                "Puerto San Carlos" to LatLng(24.7892, -112.1106)
            )

            LazyRow(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 130.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(destinosRapidos) { (nombre, latLng) ->
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                            .clickable {
                                marcadorDestino = latLng
                                destino = nombre
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(nombre, color = NavyBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 4. TARJETA INFERIOR (Opciones de Raite)
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Opciones de Raite", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Opción seleccionada
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, OrangePrimary, RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Raite Estándar", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                            Text("Donativo voluntario", color = Color.Gray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Sugerido", color = Color.Gray, fontSize = 10.sp)
                            Text("$15 MXN", fontWeight = FontWeight.ExtraBold, color = NavyBlue, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null && destino.isNotEmpty()) {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val req = PeticionPasajeroRequest(currentUser.uid, origen, destino, "Ubicación GPS")
                                        val res = UnraitApi.retrofitService.solicitarConductor(req)
                                        if (res.success) {
                                            onSolicitudEnviada() // Manda a la pantalla de "Buscando conductor..."
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al solicitar el viaje", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (destino.isEmpty()) Color.LightGray else NavyBlue,
                            contentColor = Color.White
                        ),
                        enabled = destino.isNotEmpty() && !isLoading,
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (destino.isEmpty()) "SELECCIONA UN DESTINO" else "SOLICITAR CONDUCTOR",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}