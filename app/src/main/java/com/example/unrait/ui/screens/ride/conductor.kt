package com.example.unrait.ui.screens.ride

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

import com.example.unrait.network.AceptarPeticionMapaRequest
import com.example.unrait.network.AceptarSolicitudRequest
import com.example.unrait.network.PeticionPasajero
import com.google.firebase.auth.FirebaseAuth
import com.example.unrait.network.UnraitApi
import com.example.unrait.network.PublicarViajeRequest
import com.example.unrait.network.FinalizarViajeRequest

enum class DriverState { OFFLINE, ONLINE_IDLE, EN_CAMINO_A_RECOGER, ON_TRIP, TRIP_FINISHED, PANIC_MODE }

data class PassengerReq(
    val id: String,
    val name: String,
    val telefono: String,
    val location: LatLng,
    val requestPoint: String,
    var meetingPoint: String = "",
    var isPickedUp: Boolean = false
)

fun reproducirSonidoNotificacion(context: Context) {
    try {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, notification)
        r.play()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun abrirWhatsApp(context: Context, telefono: String) {
    if (telefono.isEmpty()) {
        Toast.makeText(context, "El usuario no registró su teléfono", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val numero = if (telefono.startsWith("+")) telefono else "+52$telefono"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$numero")
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorScreen(onBack: () -> Unit) {
    var driverState by remember { mutableStateOf(DriverState.OFFLINE) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showPublishRideSheet by remember { mutableStateOf(false) }

    var peticionesPasajerosReal by remember { mutableStateOf<List<PeticionPasajero>>(emptyList()) }
    var peticionesCampanaAnteriores by remember { mutableIntStateOf(0) }
    var peticionesMapaAnteriores by remember { mutableIntStateOf(0) }

    val incomingRequests = remember { mutableStateListOf<PassengerReq>() }
    val activePassengers = remember { mutableStateListOf<PassengerReq>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var driverLoc by remember { mutableStateOf(LatLng(25.044167, -111.639243)) }
    var nombreLocalidad by remember { mutableStateOf("Buscando tu ubicación...") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(driverLoc, 14f)
    }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    driverLoc = LatLng(location.latitude, location.longitude)
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(driverLoc, 15f))
                    }
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            nombreLocalidad = addresses[0].locality ?: addresses[0].subAdminArea ?: "Ubicación Actual"
                        } else {
                            nombreLocalidad = "Ubicación detectada por GPS"
                        }
                    } catch (e: Exception) {
                        nombreLocalidad = "Ubicación detectada por GPS"
                    }
                }
            }
        }
    }

    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = OrangePrimary,
        focusedLabelColor = OrangePrimary,
        unfocusedLabelColor = Color.DarkGray,
        unfocusedBorderColor = Color.LightGray
    )

    LaunchedEffect(driverState) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (driverState == DriverState.ONLINE_IDLE && currentUser != null) {
            while (true) {
                try {
                    val resMapa = UnraitApi.retrofitService.getPeticionesActivas()
                    if (resMapa.success) {
                        if (resMapa.peticiones.size > peticionesMapaAnteriores) {
                            reproducirSonidoNotificacion(context)
                        }
                        peticionesMapaAnteriores = resMapa.peticiones.size
                        peticionesPasajerosReal = resMapa.peticiones
                    }

                    if (!showRequestsDialog) {
                        val resCampana = UnraitApi.retrofitService.getMisSolicitudes(currentUser.uid)
                        if (resCampana.success) {
                            if (resCampana.solicitudes.size > peticionesCampanaAnteriores) {
                                reproducirSonidoNotificacion(context)
                            }
                            peticionesCampanaAnteriores = resCampana.solicitudes.size

                            incomingRequests.clear()
                            resCampana.solicitudes.forEach { sol ->
                                incomingRequests.add(
                                    PassengerReq(
                                        id = sol.id_solicitud.toString(),
                                        name = sol.pasajero,
                                        telefono = sol.telefono_pasajero ?: "",
                                        location = LatLng(driverLoc.latitude + 0.001, driverLoc.longitude - 0.001),
                                        requestPoint = sol.destino
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(4000)
            }
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White) } },
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
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            ) {
                if (driverState != DriverState.OFFLINE) {
                    Marker(state = rememberMarkerState(position = driverLoc), title = "Mi Ubicación")

                    peticionesPasajerosReal.forEachIndexed { index, peticion ->
                        val dispersiones = listOf(
                            Pair(0.002, 0.003), Pair(-0.003, 0.001), Pair(0.001, -0.004), Pair(-0.002, -0.002), Pair(0.004, -0.001)
                        )
                        val offset = dispersiones[index % dispersiones.size]
                        val latDesplazada = driverLoc.latitude + offset.first
                        val lngDesplazada = driverLoc.longitude + offset.second
                        val posEstudiante = LatLng(latDesplazada, lngDesplazada)

                        Marker(
                            state = rememberMarkerState(position = posEstudiante),
                            title = peticion.nombre_pasajero,
                            snippet = "Va a: ${peticion.destino}",
                        )
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
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { showPublishRideSheet = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                                Text("Publicar Viaje Programado")
                            }
                        }

                        DriverState.ONLINE_IDLE -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { showPublishRideSheet = true },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                                ) { Text("Publicar", color = Color.White) }

                                Button(
                                    onClick = { driverState = DriverState.OFFLINE },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)
                                ) { Text("Desconectar") }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Pasajeros buscando viaje ahora:", fontWeight = FontWeight.Bold, color = NavyBlue, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (peticionesPasajerosReal.isEmpty()) {
                                Text("Nadie buscando en el mapa por el momento.", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                    items(peticionesPasajerosReal) { peticion ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8))) {
                                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(peticion.nombre_pasajero, fontWeight = FontWeight.Bold, color = NavyBlue)
                                                    Text("Hacia: ${peticion.destino}", fontSize = 12.sp, color = Color.Gray)
                                                }
                                                Button(
                                                    onClick = {
                                                        val currentUser = FirebaseAuth.getInstance().currentUser
                                                        if (currentUser != null) {
                                                            scope.launch {
                                                                try {
                                                                    // --- SOLUCIÓN: ENVIAR TU UID AL MAPA ---
                                                                    val req = AceptarPeticionMapaRequest(peticion.id_peticion, currentUser.uid)
                                                                    val res = UnraitApi.retrofitService.aceptarPeticionMapa(req)
                                                                    if (res.success) {
                                                                        activePassengers.add(PassengerReq(
                                                                            id = peticion.id_peticion.toString(),
                                                                            name = peticion.nombre_pasajero,
                                                                            telefono = peticion.telefono_pasajero ?: "",
                                                                            location = driverLoc,
                                                                            requestPoint = peticion.destino,
                                                                            meetingPoint = peticion.punto_encuentro
                                                                        ))
                                                                        driverState = DriverState.EN_CAMINO_A_RECOGER
                                                                        Toast.makeText(context, "¡Vamos por ${peticion.nombre_pasajero}!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Error al aceptar", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Text("Ir por él", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DriverState.EN_CAMINO_A_RECOGER -> {
                            val target = activePassengers.lastOrNull()

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Icon(Icons.Filled.DirectionsCar, null, tint = NavyBlue, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Punto de encuentro:", color = Color.Gray, fontSize = 12.sp)
                                    Text(target?.meetingPoint?.ifEmpty { "Ubicación compartida GPS" } ?: "Desconocido", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                                    Text("Pasajero: ${target?.name}", color = OrangePrimary, fontWeight = FontWeight.Medium)
                                }
                                IconButton(
                                    onClick = { if (target != null) abrirWhatsApp(context, target.telefono) },
                                    modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(48.dp)
                                ) { Icon(Icons.Filled.Phone, "WhatsApp", tint = Color.White, modifier = Modifier.size(24.dp)) }
                            }

                            Button(
                                onClick = {
                                    if (target != null) {
                                        activePassengers.last().isPickedUp = true
                                        driverState = DriverState.ON_TRIP
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                            ) { Text("CONFIRMAR PASAJERO A BORDO", fontWeight = FontWeight.Bold, color = Color.White) }
                        }

                        DriverState.ON_TRIP -> {
                            val target = activePassengers.lastOrNull()

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Icon(Icons.Filled.Navigation, null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Próximo destino:", color = Color.Gray, fontSize = 12.sp)
                                    Text(target?.requestPoint ?: "Destino Final", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 18.sp)
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
                                    driverState = DriverState.ONLINE_IDLE
                                }
                            )
                        }
                    }
                }

                if (showPublishRideSheet) {
                    ModalBottomSheet(onDismissRequest = { showPublishRideSheet = false }, containerColor = Color.White) {
                        var localidadAuto by remember { mutableStateOf(nombreLocalidad) }
                        var puntoEncuentro by remember { mutableStateOf("") }
                        var destino by remember { mutableStateOf("") }
                        var tiempo by remember { mutableStateOf("") }

                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                            Text("Publicar Nuevo Viaje", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(value = localidadAuto, onValueChange = { localidadAuto = it }, label = { Text("Origen (Detectado)") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = puntoEncuentro, onValueChange = { puntoEncuentro = it }, label = { Text("Punto de encuentro (Ej. Parque, Parada)") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = destino, onValueChange = { destino = it }, label = { Text("Destino Final") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = tiempo, onValueChange = { tiempo = it }, label = { Text("Tiempo estimado de salida (Ej. 15 min)") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Error: No has iniciado sesión", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (destino.isEmpty() || tiempo.isEmpty() || puntoEncuentro.isEmpty()) {
                                        Toast.makeText(context, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    scope.launch {
                                        try {
                                            val request = PublicarViajeRequest(
                                                firebase_uid = currentUser.uid,
                                                origen = localidadAuto,
                                                destino = destino,
                                                punto_encuentro = puntoEncuentro,
                                                hora_salida = tiempo,
                                                cupos_disponibles = 3
                                            )

                                            val response = UnraitApi.retrofitService.publicarViaje(request)

                                            if (response.success) {
                                                Toast.makeText(context, "¡Viaje publicado con éxito!", Toast.LENGTH_SHORT).show()
                                                showPublishRideSheet = false
                                                driverState = DriverState.ONLINE_IDLE
                                            } else {
                                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error al publicar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                Text("PUBLICAR VIAJE", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                if (showRequestsDialog) {
                    AlertDialog(
                        onDismissRequest = { showRequestsDialog = false },
                        title = { Text("Peticiones Directas a ti", color = NavyBlue, fontWeight = FontWeight.Bold) },
                        text = {
                            if (incomingRequests.isEmpty()) {
                                Text("No hay peticiones en tu campana.", color = Color.Gray)
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
                                                        onClick = { abrirWhatsApp(context, req.telefono) },
                                                        modifier = Modifier.size(32.dp).background(Color(0xFF25D366), CircleShape)
                                                    ) { Icon(Icons.Filled.Phone, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                                }
                                                Text("Destino: ${req.requestPoint}", fontSize = 12.sp, color = Color.DarkGray)

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
                                                            scope.launch {
                                                                try {
                                                                    val reqAccept = AceptarSolicitudRequest(req.id.toInt())
                                                                    val res = UnraitApi.retrofitService.aceptarSolicitud(reqAccept)

                                                                    if (res.success) {
                                                                        req.meetingPoint = meetingPt.ifEmpty { req.requestPoint }
                                                                        activePassengers.add(req)
                                                                        incomingRequests.remove(req)
                                                                        showRequestsDialog = false
                                                                        driverState = DriverState.EN_CAMINO_A_RECOGER
                                                                        Toast.makeText(context, "Pasajero Aceptado", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Error al aceptar", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
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
}

@Composable
fun CalificacionFinal(onFinalizar: () -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var comentarios by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    Text("Donativo Sugerido Recibido", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                Text("$15 MXN", color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Califica a tus pasajeros", fontWeight = FontWeight.Bold, color = NavyBlue)

        Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            for (i in 1..5) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Calificación",
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { rating = i }
                        .padding(horizontal = 4.dp),
                    tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.3f)
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
            onClick = {
                scope.launch {
                    try {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            UnraitApi.retrofitService.finalizarViajeConductor(FinalizarViajeRequest(currentUser.uid, "0"))
                        }
                    } catch (e: Exception) { }
                    onFinalizar()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
        ) { Text("ENVIAR Y VOLVER AL INICIO", fontWeight = FontWeight.Bold, color = Color.White) }
    }
}