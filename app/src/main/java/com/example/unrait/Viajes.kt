package com.example.unrait

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajesScreen() {
    var origenText by remember { mutableStateOf("Buscando ubicación...") }
    var destinoText by remember { mutableStateOf("") }
    var selectedVehiculoIndex by remember { mutableIntStateOf(0) }
    
    var originLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-16.4897, -68.1193), 14f)
    }

    val destinationMarkerState = rememberMarkerState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
        }
    )

    // Función para obtener dirección desde coordenadas (Geocoding Inverso)
    fun updateAddress(latLng: LatLng, isOrigin: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addressLine = addresses[0].getAddressLine(0)
                            if (isOrigin) {
                                origenText = addressLine
                                originLatLng = latLng
                            } else {
                                destinoText = addressLine
                                destinationLatLng = latLng
                                destinationMarkerState.position = latLng
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addressLine = addresses[0].getAddressLine(0)
                        withContext(Dispatchers.Main) {
                            if (isOrigin) {
                                origenText = addressLine
                                originLatLng = latLng
                            } else {
                                destinoText = addressLine
                                destinationLatLng = latLng
                                destinationMarkerState.position = latLng
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isOrigin) destinoText = "Ubicación en el mapa"
                }
            }
        }
    }

    // Obtener ubicación real al inicio
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    updateAddress(currentLatLng, true)
                }
            }
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val vehiculos = listOf(
        Vehiculo("Económico", "5-10 min", "15 BOB", Icons.Filled.DirectionsCar),
        Vehiculo("Confort", "3-7 min", "25 BOB", Icons.Filled.DirectionsCar),
        Vehiculo("Mantenimiento", "8-12 min", "12 BOB", Icons.Filled.DirectionsBus)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedir Viaje", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { mostrarPantalla.value = "main" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = WhiteBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Contenedor del Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                    onMapClick = { latLng ->
                        updateAddress(latLng, false)
                    }
                ) {
                    originLatLng?.let {
                        Marker(
                            state = rememberMarkerState(position = it),
                            title = "Origen",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                    
                    if (destinationLatLng != null) {
                        Marker(
                            state = destinationMarkerState,
                            title = "Destino (Arrastra para mover)",
                            draggable = true,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                        
                        LaunchedEffect(destinationMarkerState.isDragging) {
                            if (!destinationMarkerState.isDragging) {
                                updateAddress(destinationMarkerState.position, false)
                            }
                        }
                    }
                }

                // Card de búsqueda sobre el mapa
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = origenText,
                            onValueChange = { origenText = it },
                            label = { Text("Origen") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null, tint = OrangePrimary) },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = destinoText,
                            onValueChange = { destinoText = it },
                            label = { Text("Destino (Toca el mapa o arrastra el pin)") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = Color.Red) },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            placeholder = { Text("Toca el mapa para marcar") }
                        )
                    }
                }
                
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val currentLatLng = LatLng(it.latitude, it.longitude)
                                    cameraPositionState.move(CameraUpdateFactory.newLatLng(currentLatLng))
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    containerColor = Color.White,
                    contentColor = NavyBlue
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación")
                }
            }

            // Sección inferior: Selección de Vehículo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selecciona tu transporte",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 180.dp)
                    ) {
                        items(vehiculos.size) { index ->
                            val vehiculo = vehiculos[index]
                            VehiculoItem(
                                vehiculo = vehiculo,
                                isSelected = selectedVehiculoIndex == index,
                                onClick = { selectedVehiculoIndex = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val selected = vehiculos[selectedVehiculoIndex]
                            viajeDetalle.value = ViajeConfirmado(
                                origen = origenText,
                                destino = destinoText,
                                vehiculoNombre = selected.nombre,
                                precio = selected.precio,
                                iconoVehiculo = selected.icono
                            )
                            mostrarPantalla.value = "confirmacion"
                        },
                        enabled = destinationLatLng != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (destinationLatLng == null) "SELECCIONA DESTINO EN MAPA" 
                            else "CONFIRMAR ${vehiculos[selectedVehiculoIndex].nombre.uppercase()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

data class Vehiculo(val nombre: String, val tiempo: String, val precio: String, val icono: ImageVector)

@Composable
fun VehiculoItem(vehiculo: Vehiculo, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) OrangePrimary.copy(alpha = 0.1f) else Color(0xFFF8F8F8))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) OrangePrimary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = vehiculo.icono,
            contentDescription = null,
            tint = if (isSelected) OrangePrimary else NavyBlue,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(vehiculo.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyBlue)
            Text(vehiculo.tiempo, fontSize = 13.sp, color = Color.Gray)
        }
        Text(vehiculo.precio, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = NavyBlue)
    }
}
