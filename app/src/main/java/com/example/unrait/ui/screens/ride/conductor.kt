package com.example.unrait.ui.screens.ride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unrait.NavyBlue
import com.example.unrait.OrangePrimary
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

enum class TripStatus {
    IDLE,           // Esperando solicitudes
    PICKING_UP,     // Yendo por los pasajeros uno por uno
    ON_BOARD        // Todos en el vehículo, hacia el ITSCC
}

data class Passenger(
    val id: String,
    val name: String, 
    val location: LatLng, 
    val pickupPoint: String, 
    var isPickedUp: Boolean = false,
    val career: String = "Ingeniería en Sistemas",
    val semester: String = "7mo Semestre",
    val rating: String = "4.8"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorScreen(onBack: () -> Unit) {
    var isOnline by remember { mutableStateOf(false) }
    var tripStatus by remember { mutableStateOf(TripStatus.IDLE) }
    
    // Lista de pasajeros vinculados
    val acceptedPassengers = remember { mutableStateListOf<Passenger>() }
    var currentPickingUpIndex by remember { mutableIntStateOf(0) }
    
    // Notificaciones (Solicitudes)
    val notifications = remember { mutableStateListOf<Passenger>() }
    var showNotificationDialog by remember { mutableStateOf(false) }
    
    // Estados para las hojas modales
    var showProfileSheet by remember { mutableStateOf(false) }
    var showPassengersSheet by remember { mutableStateOf(false) }
    var showRouteSheet by remember { mutableStateOf(false) }
    
    // Estado para ver info detallada del usuario
    var selectedPassengerInfo by remember { mutableStateOf<Passenger?>(null) }

    // Ubicaciones simuladas
    val conductorInitialLoc = LatLng(25.032, -111.655)
    val user1Loc = LatLng(25.040, -111.645)
    val user2Loc = LatLng(25.050, -111.635)
    val itscc = LatLng(25.044167, -111.639243)

    // Agregamos un usuario por defecto en vinculación y una notificación
    LaunchedEffect(Unit) {
        if (acceptedPassengers.isEmpty()) {
            acceptedPassengers.add(Passenger("1", "Juan Pérez", user1Loc, "Entrada Principal", false, "Ing. Sistemas", "7mo", "4.8"))
            tripStatus = TripStatus.PICKING_UP
            
            notifications.add(Passenger("2", "María García", user2Loc, "Parada Central", false, "Lic. Administración", "5to", "4.9"))
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(conductorInitialLoc, 14f)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isOnline) "EN LÍNEA" else "DESCONECTADO",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { showNotificationDialog = true }) {
                            Icon(Icons.Filled.Notifications, "Notificaciones", tint = Color.White)
                        }
                        if (notifications.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = NavyBlue)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                if (isOnline) {
                    Marker(state = rememberMarkerState(position = conductorInitialLoc), title = "Tu ubicación")
                    
                    if (tripStatus == TripStatus.ON_BOARD) {
                        Marker(state = rememberMarkerState(position = itscc), title = "Destino: ITSCC")
                    } else if (tripStatus == TripStatus.PICKING_UP) {
                        acceptedPassengers.getOrNull(currentPickingUpIndex)?.let { passenger ->
                            Marker(state = rememberMarkerState(position = passenger.location), title = "Recoger a ${passenger.name}")
                        }
                    }
                }
            }

            // PANEL SUPERIOR RATING
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107))
                    Text(" 4.9 Rating", fontWeight = FontWeight.Bold, color = NavyBlue)
                }
            }

            // PANEL DE CONTROL INFERIOR
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActionIconButton(icon = Icons.Default.Person, label = "Vehículo", onClick = { showProfileSheet = true })
                        ActionIconButton(
                            icon = Icons.Default.Groups, 
                            label = "Vinculados (${acceptedPassengers.size})", 
                            onClick = { showPassengersSheet = true }
                        )
                        ActionIconButton(icon = Icons.Default.DirectionsCar, label = "Ruta", color = OrangePrimary, onClick = { showRouteSheet = true })
                    }

                    if (isOnline) {
                        when (tripStatus) {
                            TripStatus.PICKING_UP -> {
                                val current = acceptedPassengers.getOrNull(currentPickingUpIndex)
                                Button(
                                    onClick = { 
                                        if (current != null) {
                                            acceptedPassengers[currentPickingUpIndex] = current.copy(isPickedUp = true)
                                            if (currentPickingUpIndex < acceptedPassengers.size - 1) {
                                                currentPickingUpIndex++
                                            } else {
                                                tripStatus = TripStatus.ON_BOARD
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("CONFIRMAR PASAJERO A BORDO", fontWeight = FontWeight.Bold)
                                }
                            }
                            TripStatus.ON_BOARD -> {
                                Button(
                                    onClick = { 
                                        tripStatus = TripStatus.IDLE
                                        acceptedPassengers.clear()
                                        currentPickingUpIndex = 0
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("VIAJE COMPLETADO EN ITSCC", fontWeight = FontWeight.Bold)
                                }
                            }
                            TripStatus.IDLE -> {
                                Button(
                                    onClick = { isOnline = false },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("DESCONECTARSE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { isOnline = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("PONERSE EN LÍNEA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DIALOGO DE NOTIFICACIONES
            if (showNotificationDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationDialog = false },
                    title = { Text("Solicitudes de Viaje", color = NavyBlue, fontWeight = FontWeight.Bold) },
                    text = {
                        if (notifications.isEmpty()) {
                            Text("No tienes solicitudes pendientes.")
                        } else {
                            LazyColumn {
                                items(notifications) { request ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(request.name, fontWeight = FontWeight.Bold, color = NavyBlue)
                                            Text("Punto: ${request.pickupPoint}", fontSize = 12.sp)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = { notifications.remove(request) }) { Text("Ignorar", color = Color.Gray) }
                                                Button(
                                                    onClick = { 
                                                        acceptedPassengers.add(request)
                                                        notifications.remove(request)
                                                        if (tripStatus == TripStatus.IDLE) tripStatus = TripStatus.PICKING_UP
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                                ) { Text("Aceptar") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showNotificationDialog = false }) { Text("Cerrar") } },
                    containerColor = Color.White
                )
            }

            // INFO DETALLADA DEL USUARIO VINCULADO
            if (selectedPassengerInfo != null) {
                AlertDialog(
                    onDismissRequest = { selectedPassengerInfo = null },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(80.dp), tint = NavyBlue)
                            Text(selectedPassengerInfo!!.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = NavyBlue)
                            Text(selectedPassengerInfo!!.career, color = Color.Gray)
                            Text(selectedPassengerInfo!!.semester, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                Text(" ${selectedPassengerInfo!!.rating} Calificación", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Recoger en: ${selectedPassengerInfo!!.pickupPoint}", fontWeight = FontWeight.Medium)
                        }
                    },
                    confirmButton = { Button(onClick = { selectedPassengerInfo = null }, colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)) { Text("Volver") } },
                    containerColor = Color.White
                )
            }

            // HOJAS MODALES
            if (showPassengersSheet) {
                ModalBottomSheet(onDismissRequest = { showPassengersSheet = false }, containerColor = Color.White) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Pasajeros Vinculados", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        acceptedPassengers.forEach { passenger ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPassengerInfo = passenger }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (passenger.isPickedUp) Icons.Default.CheckCircle else Icons.Default.Person, null, tint = if (passenger.isPickedUp) Color(0xFF4CAF50) else OrangePrimary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(passenger.name, fontWeight = FontWeight.Bold)
                                    Text("Ver detalles del perfil", fontSize = 12.sp, color = OrangePrimary)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            if (showProfileSheet) {
                ModalBottomSheet(onDismissRequest = { showProfileSheet = false }, containerColor = Color.White) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Información del Vehículo", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Toyota Corolla 2022 • Blanco", fontSize = 18.sp)
                        Text("Placas: CZ-4521-B", fontWeight = FontWeight.Bold, color = OrangePrimary)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            if (showRouteSheet) {
                ModalBottomSheet(onDismissRequest = { showRouteSheet = false }, containerColor = Color.White) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Ruta de Viaje", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        acceptedPassengers.forEachIndexed { index, p ->
                            RouteItem(label = "Recoger a ${p.name}", point = p.pickupPoint, isActive = tripStatus == TripStatus.PICKING_UP && currentPickingUpIndex == index, isDone = p.isPickedUp)
                        }
                        RouteItem(label = "Destino Final", point = "ITSCC Tecnológico", isActive = tripStatus == TripStatus.ON_BOARD, isDone = false, isLast = true)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RouteItem(label: String, point: String, isActive: Boolean, isDone: Boolean, isLast: Boolean = false) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonChecked, null, tint = if (isDone) Color(0xFF4CAF50) else if (isActive) OrangePrimary else Color.LightGray)
            if (!isLast) Box(modifier = Modifier.width(2.dp).height(30.dp).background(Color.LightGray))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) NavyBlue else Color.Gray)
            Text(point, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, label: String, onClick: () -> Unit, color: Color = NavyBlue) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(onClick = onClick, containerColor = Color(0xFFF5F6F8), contentColor = color, shape = CircleShape, modifier = Modifier.size(56.dp), elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)) {
            Icon(icon, label)
        }
        Text(label, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
    }
}
