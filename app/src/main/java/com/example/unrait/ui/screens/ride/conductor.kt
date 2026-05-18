package com.example.unrait.ui.screens.ride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Imports de Google Maps
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


enum class DriverState {
    OFFLINE,
    ONLINE_IDLE,
    EN_CAMINO_A_RECOGER,
    ON_TRIP,
    TRIP_FINISHED,
    PANIC_MODE
}

data class PassengerReq(
    val id: String,
    val name: String,
    val location: LatLng,
    val requestPoint: String,
    val phone: String = "6130000000",
    var meetingPoint: String = "",
    var isPickedUp: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var driverState by remember { mutableStateOf(DriverState.OFFLINE) }

    val activePassengers = remember { mutableStateListOf<PassengerReq>() }
    val incomingRequests = remember { mutableStateListOf<PassengerReq>() }
    var currentPassengerIndex by remember { mutableIntStateOf(0) }

    var showPublishRideSheet by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }

    // Ubicaciones
    val driverLoc = LatLng(25.2625, -111.7753) // Ejemplo: Cd. Insurgentes
    val user1Loc = LatLng(25.2650, -111.7700)
    val itscc = LatLng(25.044167, -111.639243)

    // Estilos de texto
    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = OrangePrimary,
        focusedLabelColor = OrangePrimary,
        unfocusedLabelColor = Color.DarkGray,
        unfocusedBorderColor = Color.LightGray
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(driverLoc, 14f)
    }

    LaunchedEffect(driverState) {
        if (driverState == DriverState.ONLINE_IDLE && incomingRequests.isEmpty() && activePassengers.isEmpty()) {
            delay(2000)
            incomingRequests.add(PassengerReq("1", "Danna Cota", user1Loc, "Cerca de abarrotes El Molino"))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when(driverState) {
                            DriverState.OFFLINE -> "DESCONECTADO"
                            DriverState.ONLINE_IDLE -> "EN LÍNEA"
                            DriverState.EN_CAMINO_A_RECOGER -> "EN CAMINO"
                            DriverState.ON_TRIP -> "VIAJE EN CURSO"
                            DriverState.TRIP_FINISHED -> "RESUMEN"
                            DriverState.PANIC_MODE -> "EMERGENCIA SOS"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White) }
                },
                actions = {
                    if (driverState == DriverState.ONLINE_IDLE) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showRequestsDialog = true }) {
                                Icon(Icons.Filled.Notifications, "Solicitudes", tint = Color.White)
                            }
                            if (incomingRequests.isNotEmpty()) {
                                Box(modifier = Modifier.padding(top = 8.dp, end = 8.dp).size(10.dp).clip(CircleShape).background(Color.Red))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (driverState == DriverState.PANIC_MODE) Color.Red else NavyBlue
                )
            )
        },
        floatingActionButton = {
            if (driverState == DriverState.ON_TRIP || driverState == DriverState.EN_CAMINO_A_RECOGER) {
                FloatingActionButton(
                    onClick = { driverState = DriverState.PANIC_MODE },
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "Pánico", modifier = Modifier.size(32.dp))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                if (driverState != DriverState.OFFLINE) {
                    Marker(state = rememberMarkerState(position = driverLoc), title = "Mi Ubicación")

                    if (driverState == DriverState.ON_TRIP || driverState == DriverState.EN_CAMINO_A_RECOGER) {
                        activePassengers.forEach { p ->
                            if (!p.isPickedUp) {
                                Marker(state = rememberMarkerState(position = p.location), title = "Recoger a ${p.name}")
                                // Dibuja la ruta hacia el pasajero
                                Polyline(points = listOf(driverLoc, p.location), color = NavyBlue, width = 12f)
                            }
                        }
                        Marker(state = rememberMarkerState(position = itscc), title = "Destino Final (ITSCC)")

                        // Dibuja la ruta hacia el ITSCC
                        if (driverState == DriverState.ON_TRIP) {
                            Polyline(points = listOf(driverLoc, itscc), color = OrangePrimary, width = 12f)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (driverState) {
                        DriverState.OFFLINE -> {
                            Icon(Icons.Filled.PowerOff, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { driverState = DriverState.ONLINE_IDLE },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) { Text("CONECTARSE Y OFRECER VIAJES", fontWeight = FontWeight.Bold, color = Color.White) }
                        }

                        DriverState.ONLINE_IDLE -> {
                            Text("Esperando pasajeros o publica un viaje.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { showPublishRideSheet = true },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                                ) { Text("Publicar Viaje", color = Color.White) }

                                Button(
                                    onClick = { driverState = DriverState.OFFLINE },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)
                                ) { Text("Desconectarse") }
                            }
                        }

                        DriverState.EN_CAMINO_A_RECOGER -> {
                            val target = if (currentPassengerIndex < activePassengers.size) activePassengers[currentPassengerIndex] else null

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Icon(Icons.Filled.DirectionsCar, null, tint = NavyBlue, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Punto de encuentro:", color = Color.Gray, fontSize = 12.sp)
                                    Text(target?.meetingPoint ?: "Desconocido", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { /* TODO: Intent a WhatsApp */ },
                                    modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(40.dp)
                                ) { Icon(Icons.Filled.Chat, "WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp)) }
                            }

                            Button(
                                onClick = {
                                    if (target != null) {
                                        activePassengers[currentPassengerIndex].isPickedUp = true
                                        driverState = DriverState.ON_TRIP
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                            ) { Text("CONFIRMAR PASAJERO A BORDO", fontWeight = FontWeight.Bold, color = Color.White) }
                        }

                        DriverState.ON_TRIP -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Icon(Icons.Filled.Navigation, null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Próximo destino:", color = Color.Gray, fontSize = 12.sp)
                                    Text("ITSCC (Destino Final)", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 18.sp)
                                }
                            }

                            Button(
                                onClick = { driverState = DriverState.TRIP_FINISHED },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("FINALIZAR VIAJE", fontWeight = FontWeight.Bold, color = Color.White) }
                        }

                        DriverState.PANIC_MODE -> {
                            Text("⚠️ PROTOCOLO DE SEGURIDAD ACTIVADO", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("El servicio ha sido detenido. Tu ubicación exacta ha sido enviada a las autoridades institucionales.", color = Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))

                            Button(
                                onClick = { /* Llamar 911 o Seguridad ITSCC */ },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                            ) { Text("LLAMAR A SEGURIDAD AHORA", color = Color.White, fontWeight = FontWeight.Bold) }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { driverState = DriverState.OFFLINE }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Text("Falsa Alarma (Desconectarse)", color = Color.Gray)
                            }
                        }

                        DriverState.TRIP_FINISHED -> {
                            CalificacionFinal(
                                onFinalizar = {
                                    activePassengers.clear()
                                    currentPassengerIndex = 0
                                    driverState = DriverState.ONLINE_IDLE
                                }
                            )
                        }
                    }
                }
            }

            if (showPublishRideSheet) {
                ModalBottomSheet(onDismissRequest = { showPublishRideSheet = false }, containerColor = Color.White) {
                    var localidadAuto by remember { mutableStateOf("Ciudad Insurgentes (Ubicación Actual)") }
                    var puntoEncuentro by remember { mutableStateOf("") }
                    var destino by remember { mutableStateOf("ITSCC Tecnológico") }
                    var tiempo by remember { mutableStateOf("Saliendo en 15 min") }

                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Publicar Nuevo Viaje", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(value = localidadAuto, onValueChange = {}, label = { Text("Localidad (Detectada)") }, modifier = Modifier.fillMaxWidth(), readOnly = true, textStyle = textStyleDark, colors = textFieldColors)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = puntoEncuentro, onValueChange = { puntoEncuentro = it }, label = { Text("Especificar punto de encuentro (Ej. Parque)") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = destino, onValueChange = { destino = it }, label = { Text("Destino") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = tiempo, onValueChange = { tiempo = it }, label = { Text("Tiempo estimado de salida") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showPublishRideSheet = false
                                if(activePassengers.isNotEmpty()) driverState = DriverState.EN_CAMINO_A_RECOGER
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) { Text("PUBLICAR VIAJE", fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            if (showRequestsDialog) {
                AlertDialog(
                    onDismissRequest = { showRequestsDialog = false },
                    title = { Text("Peticiones de Usuarios", color = NavyBlue, fontWeight = FontWeight.Bold) },
                    text = {
                        if (incomingRequests.isEmpty()) {
                            Text("No hay peticiones nuevas.", color = Color.Gray)
                        } else {
                            LazyColumn {
                                items(incomingRequests) { req ->
                                    var meetingPt by remember { mutableStateOf("") }

                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8))) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(req.name, fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.weight(1f))
                                                IconButton(
                                                    onClick = { /* TODO: Intent WhatsApp */ },
                                                    modifier = Modifier.size(32.dp).background(Color(0xFF25D366), CircleShape)
                                                ) { Icon(Icons.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                            }
                                            Text("Ubicación: ${req.requestPoint}", fontSize = 12.sp, color = Color.DarkGray)

                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = meetingPt,
                                                onValueChange = { meetingPt = it },
                                                label = { Text("Indica dónde lo recogerás") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = textStyleDark,
                                                colors = textFieldColors
                                            )

                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = { incomingRequests.remove(req) }) { Text("Rechazar", color = Color.Gray) }
                                                Button(
                                                    onClick = {
                                                        req.meetingPoint = meetingPt.ifEmpty { req.requestPoint }
                                                        activePassengers.add(req)
                                                        incomingRequests.remove(req)
                                                        showRequestsDialog = false
                                                        driverState = DriverState.EN_CAMINO_A_RECOGER
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                                                ) { Text("Aceptar", color = Color.White) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showRequestsDialog = false }) { Text("Cerrar") } },
                    containerColor = Color.White
                )
            }
        }
    }
}

@Composable
fun CalificacionFinal(onFinalizar: () -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var comentarios by remember { mutableStateOf("") }

    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary,
        unfocusedLabelColor = Color.DarkGray, unfocusedBorderColor = Color.LightGray
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("¡Llegaste a tu destino!", color = NavyBlue, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Has finalizado el viaje con éxito.", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolunteerActivism, null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Donativo Recibido", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                Text("$15 MXN", color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Califica a tus pasajeros", fontWeight = FontWeight.Bold, color = NavyBlue)

        Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            for (i in 1..5) {
                Text(
                    text = "👑",
                    fontSize = 40.sp,
                    modifier = Modifier.clickable { rating = i }.padding(horizontal = 4.dp),
                    color = if (i <= rating) Color.Unspecified else Color.Gray.copy(alpha = 0.3f)
                )
            }
        }

        OutlinedTextField(
            value = comentarios,
            onValueChange = { comentarios = it },
            label = { Text("Comentarios (Opcional)") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 3,
            textStyle = textStyleDark,
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFinalizar,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
        ) { Text("ENVIAR Y VOLVER AL INICIO", fontWeight = FontWeight.Bold, color = Color.White) }
    }
}