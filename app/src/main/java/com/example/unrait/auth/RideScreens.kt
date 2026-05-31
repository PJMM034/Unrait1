package com.example.unrait.ui.screens.ride

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.unrait.network.UnraitApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
import com.example.unrait.network.FinalizarViajeRequest

val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val BackgroundGray = Color(0xFFF5F6F8)

enum class PassengerTripState { ESPERANDO_CONDUCTOR, CONDUCTOR_LLEGO, EN_VIAJE, PANIC_MODE, FINALIZADO }

fun reproducirSonido(context: Context) {
    try {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, notification)
        r.play()
    } catch (e: Exception) { e.printStackTrace() }
}

fun abrirWhatsAppPasajero(context: Context, telefono: String) {
    if (telefono.isEmpty()) { Toast.makeText(context, "El conductor no registró su teléfono", Toast.LENGTH_SHORT).show(); return }
    try {
        val numero = if (telefono.startsWith("+")) telefono else "+52$telefono"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$numero")
        context.startActivity(intent)
    } catch (e: Exception) { Toast.makeText(context, "WhatsApp no instalado", Toast.LENGTH_SHORT).show() }
}

fun base64ToBitmap(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrEmpty()) return null
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallesViajeScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Detalles del Viaje", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Regresar", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TU CONDUCTOR", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE0E5EC)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null, tint = NavyBlue, modifier = Modifier.size(40.dp)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { Text("Mario Gomez", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue); Row { Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp)); Text(" 4.9 ", fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) { Text("CANCELAR VIAJE", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoViajeScreen(onFinalizar: () -> Unit) {
    var tripState by remember { mutableStateOf(PassengerTripState.ESPERANDO_CONDUCTOR) }
    var telefonoConductor by remember { mutableStateOf("") }
    var nombreConductor by remember { mutableStateOf("Conductor Designado") }
    var fotoAutoConductor by remember { mutableStateOf<Bitmap?>(null) } // <-- ESTADO PARA LA FOTO

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var miPosicion by remember { mutableStateOf(LatLng(25.044167, -111.639243)) }
    var destinoFinal by remember { mutableStateOf(LatLng(25.048000, -111.642000)) }

    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(miPosicion, 14f) }

    var hasLocationPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms -> hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }
        else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    miPosicion = LatLng(location.latitude, location.longitude)
                    destinoFinal = LatLng(25.044167, -111.639243)
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(miPosicion, 15f)) }
                }
            }
        }
    }

    LaunchedEffect(tripState) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (tripState == PassengerTripState.ESPERANDO_CONDUCTOR && currentUser != null) {
            while (true) {
                try {
                    val response = UnraitApi.retrofitService.getEstadoPasajero(currentUser.uid)
                    if (response.success && (response.estado == "aceptada" || response.estado == "en_curso")) {
                        nombreConductor = response.conductor ?: "Conductor Asignado"
                        telefonoConductor = response.telefono_conductor ?: "6130000000"
                        fotoAutoConductor = base64ToBitmap(response.foto_auto) // <-- RECIBE LA FOTO

                        tripState = PassengerTripState.CONDUCTOR_LLEGO
                        reproducirSonido(context)
                        break
                    }
                } catch (e: Exception) { e.printStackTrace() }
                delay(3000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = when(tripState) { PassengerTripState.ESPERANDO_CONDUCTOR -> "Buscando..."; PassengerTripState.CONDUCTOR_LLEGO -> "¡Conductor asignado!"; PassengerTripState.EN_VIAJE -> "Viaje en curso"; PassengerTripState.PANIC_MODE -> "EMERGENCIA SOS"; PassengerTripState.FINALIZADO -> "Viaje finalizado" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (tripState == PassengerTripState.ESPERANDO_CONDUCTOR) { Text("Espera por favor...", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    }
                },
                navigationIcon = { IconButton(onClick = {
                    scope.launch { try { val currentUser = FirebaseAuth.getInstance().currentUser; if (currentUser != null) { UnraitApi.retrofitService.finalizarViajePasajero(FinalizarViajeRequest(currentUser.uid, "0")) } } catch (e: Exception) { } }
                    onFinalizar()
                }) { Icon(Icons.Filled.ArrowBack, "Regresar", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (tripState == PassengerTripState.PANIC_MODE) Color.Red else NavyBlue)
            )
        },
        floatingActionButton = {
            if(tripState == PassengerTripState.EN_VIAJE || tripState == PassengerTripState.CONDUCTOR_LLEGO) {
                FloatingActionButton(onClick = { tripState = PassengerTripState.PANIC_MODE }, containerColor = Color.Red, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(64.dp)) { Icon(Icons.Filled.Warning, "Pánico", modifier = Modifier.size(32.dp)) }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, properties = MapProperties(isMyLocationEnabled = hasLocationPermission)) {
                Marker(state = rememberMarkerState(position = miPosicion), title = "Tú (GPS Real)")
                if (tripState == PassengerTripState.EN_VIAJE || tripState == PassengerTripState.CONDUCTOR_LLEGO) {
                    Marker(state = rememberMarkerState(position = destinoFinal), title = "Destino")
                    Polyline(points = listOf(miPosicion, destinoFinal), color = NavyBlue, width = 12f)
                }
            }

            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), shadowElevation = 24.dp, color = Color.White) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (tripState == PassengerTripState.PANIC_MODE) {
                        Text("⚠️ PROTOCOLO DE SEGURIDAD", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Ubicación enviada a contactos de emergencia.", color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
                        Button(onClick = {  }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("LLAMAR A SEGURIDAD", color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { tripState = PassengerTripState.EN_VIAJE }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Falsa Alarma", color = Color.Gray) }
                    } else {
                        when (tripState) {
                            PassengerTripState.ESPERANDO_CONDUCTOR -> {
                                Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(16.dp)); Text("Buscando conductor...", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue) }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    scope.launch { try { val currentUser = FirebaseAuth.getInstance().currentUser; if (currentUser != null) { UnraitApi.retrofitService.finalizarViajePasajero(FinalizarViajeRequest(currentUser.uid, "0")) } } catch (e: Exception) { } }
                                    onFinalizar()
                                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("CANCELAR SOLICITUD") }
                            }
                            PassengerTripState.CONDUCTOR_LLEGO, PassengerTripState.EN_VIAJE -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // --- MUESTRA LA FOTO DEL AUTO ---
                                        if (fotoAutoConductor != null) {
                                            Image(
                                                bitmap = fotoAutoConductor!!.asImageBitmap(),
                                                contentDescription = "Auto del conductor",
                                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE0E5EC)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null, tint = NavyBlue, modifier = Modifier.size(32.dp)) }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column { Text(nombreConductor, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue); if (tripState == PassengerTripState.CONDUCTOR_LLEGO) { Text("Vehículo en camino...", color = Color.Gray, fontSize = 14.sp) } }
                                    }
                                    IconButton(onClick = { abrirWhatsAppPasajero(context, telefonoConductor) }, modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(40.dp)) { Icon(Icons.Filled.Phone, "WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp)) }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                if (tripState == PassengerTripState.CONDUCTOR_LLEGO) {
                                    Button(onClick = { tripState = PassengerTripState.EN_VIAJE }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)) { Text("YA ESTOY A BORDO", color = Color.White, fontWeight = FontWeight.Bold) }
                                } else {
                                    Button(onClick = { tripState = PassengerTripState.FINALIZADO }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) { Text("SIMULAR LLEGADA AL DESTINO", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }
                            PassengerTripState.FINALIZADO -> {
                                var montoDonativo by remember { mutableStateOf("") }
                                var dioDonativo by remember { mutableStateOf(false) }
                                Text("¡Llegaste a tu destino!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = NavyBlue)
                                Text("¿Realizaste algún donativo al conductor?", color = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) { Text("Sí, aporté:", fontWeight = FontWeight.Medium, color = NavyBlue); Spacer(modifier = Modifier.width(8.dp)); Switch(checked = dioDonativo, onCheckedChange = { dioDonativo = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)) }
                                if (dioDonativo) { Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = montoDonativo, onValueChange = { montoDonativo = it }, label = { Text("Monto (Ej. 15)") }, leadingIcon = { Text("$") }, modifier = Modifier.fillMaxWidth()) }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val currentUser = FirebaseAuth.getInstance().currentUser
                                                if (currentUser != null) {
                                                    val montoFinal = if (dioDonativo && montoDonativo.isNotEmpty()) montoDonativo else "0"
                                                    UnraitApi.retrofitService.finalizarViajePasajero(FinalizarViajeRequest(currentUser.uid, montoFinal))
                                                }
                                            } catch (e: Exception) { }
                                            Toast.makeText(context, "¡Gracias por usar UNRAIT!", Toast.LENGTH_SHORT).show()
                                            onFinalizar()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("FINALIZAR", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}