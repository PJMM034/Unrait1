package com.example.unrait.ui.screens.drawer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.unrait.network.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val BackgroundGray = Color(0xFFF5F6F8)

fun base64ToBitmapDrawer(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrEmpty()) return null
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) { null }
}

fun abrirWhatsAppDrawer(context: Context, telefono: String?) {
    if (telefono.isNullOrEmpty()) { Toast.makeText(context, "El usuario no registró su teléfono", Toast.LENGTH_SHORT).show(); return }
    try {
        val numero = if (telefono.startsWith("+")) telefono else "+52$telefono"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$numero")
        context.startActivity(intent)
    } catch (e: Exception) { Toast.makeText(context, "WhatsApp no instalado", Toast.LENGTH_SHORT).show() }
}

// ===============================================
// 1. PANTALLA: COMUNIDAD DE CONFIANZA
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmigosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var listaConfianza by remember { mutableStateOf<List<ConfianzaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val res = UnraitApi.retrofitService.getConfianza(currentUser.uid)
                if (res.success) listaConfianza = res.confianza
            } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mi Red de Confianza", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = OrangePrimary)
            } else if (listaConfianza.isEmpty()) {
                Text("Aún no tienes viajes finalizados para crear tu red.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(listaConfianza) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val bitmap = base64ToBitmapDrawer(item.foto_perfil)
                                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE0E5EC)), contentAlignment = Alignment.Center) {
                                        if (bitmap != null) { Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                        else { Icon(Icons.Filled.Person, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(30.dp)) }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item.nombre, fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                                        Text(item.rol, color = Color.Gray, fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                            Text(" Viajaron ${item.viajes_juntos} veces", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                        }
                                    }
                                }
                                IconButton(onClick = { abrirWhatsAppDrawer(context, item.telefono) }, modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(40.dp)) {
                                    Icon(Icons.Filled.Phone, contentDescription = "Llamar", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================================
// 2. PANTALLA: LUGARES FRECUENTES
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LugaresScreen(onBack: () -> Unit) {
    var lugaresList by remember { mutableStateOf<List<LugarFrecuenteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val res = UnraitApi.retrofitService.getLugaresFrecuentes(currentUser.uid)
                if (res.success) lugaresList = res.lugares
            } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Lugares Frecuentes", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = OrangePrimary)
            } else if (lugaresList.isEmpty()) {
                Text("Realiza viajes para descubrir tus lugares.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    itemsIndexed(lugaresList) { index, lugar ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (index == 0) Icons.Filled.Star else Icons.Filled.Place, contentDescription = null, tint = OrangePrimary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(lugar.nombre, fontWeight = FontWeight.Bold, color = NavyBlue)
                                        Text("Has ido ${lugar.cantidad} veces", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================================
// 3. PANTALLA: LOCALIDADES
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalidadesScreen(onBack: () -> Unit) {
    var locsList by remember { mutableStateOf<List<LocalidadItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val res = UnraitApi.retrofitService.getLocalidades()
            if (res.success) locsList = res.localidades
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Comunidades UNRAIT", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = OrangePrimary)
            } else if (locsList.isEmpty()) {
                Text("No hay localidades registradas.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(locsList) { loc ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Map, contentDescription = null, tint = OrangePrimary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(loc.nombre, fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${loc.cantidad}", fontWeight = FontWeight.ExtraBold, color = OrangePrimary, fontSize = 18.sp)
                                    Text("Usuarios", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================================
// 4. PANTALLA: HISTORIAL
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onBack: () -> Unit) {
    var historialList by remember { mutableStateOf<List<HistorialItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val res = UnraitApi.retrofitService.getHistorial(currentUser.uid)
                if (res.success) { historialList = res.historial }
            } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de Viajes", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = OrangePrimary)
            } else if (historialList.isEmpty()) { Text("Aún no tienes viajes finalizados.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(historialList) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.tipo ?: "Registro de Viaje", color = Color.Gray, fontSize = 12.sp)
                                    Text(item.estado?.uppercase() ?: "FINALIZADO", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Destino: ${item.destino ?: "No especificado"}", fontWeight = FontWeight.Bold, color = NavyBlue)
                                Text("Chofer: ${item.conductor ?: "Desconocido"}", fontSize = 14.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row { repeat(5) { Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp)) } }
                                    if (item.donativo != null && item.donativo != "0") {
                                        Text("Aporte: $${item.donativo} MXN", fontWeight = FontWeight.Bold, color = OrangePrimary, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================================
// 5. PANTALLA: VIAJES DISPONIBLES
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisponiblesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viajesList by remember { mutableStateOf<List<Viaje>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { val response = UnraitApi.retrofitService.getViajesDisponibles(); if (response.success) viajesList = response.viajes
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Viajes Disponibles", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = OrangePrimary)
            } else if (viajesList.isEmpty()) { Text("No hay viajes disponibles en este momento.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(viajesList) { viaje ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Conductor: ${viaje.nombre_conductor}", fontWeight = FontWeight.Bold, color = NavyBlue)
                                Text("Destino: ${viaje.destino}", fontSize = 14.sp)
                                Text("Saliendo a las: ${viaje.hora_salida}", fontSize = 14.sp)
                                Text("Cupos: ${viaje.cupos_disponibles}", fontSize = 14.sp, color = OrangePrimary)

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val currentUser = FirebaseAuth.getInstance().currentUser
                                        if (currentUser != null) {
                                            scope.launch {
                                                try {
                                                    val req = SolicitarViajeRequest(currentUser.uid, viaje.id_viaje)
                                                    val res = UnraitApi.retrofitService.solicitarViaje(req)
                                                    if (res.success) {
                                                        Toast.makeText(context, "Solicitud enviada a ${viaje.nombre_conductor}", Toast.LENGTH_SHORT).show()
                                                        com.example.unrait.mostrarPantalla.value = "seguimiento_viaje"
                                                    }
                                                } catch (e: Exception) { Toast.makeText(context, "Error al solicitar", Toast.LENGTH_SHORT).show() }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Solicitar Raite", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================================
// 6. PANTALLA: AJUSTES
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onBack: () -> Unit) {
    var notifViajes by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes", color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)) },
        containerColor = BackgroundGray
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Notificaciones", fontWeight = FontWeight.Bold, color = NavyBlue, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Alertas de viajes disponibles", color = Color.DarkGray)
                        Switch(checked = notifViajes, onCheckedChange = { notifViajes = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}