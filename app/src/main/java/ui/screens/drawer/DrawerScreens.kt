package com.example.unrait.ui.screens.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // <-- Soluciona el error de clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // <-- Soluciona el error de remember y mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores de la app
val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val BackgroundGray = Color(0xFFF5F6F8)

// ==========================================
// 1. PANTALLA DE AMIGOS (Choferes de confianza)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmigosScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Choferes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, tint = Color.White, contentDescription = "Regresar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(3) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE0E5EC)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(if (index == 0) "Mario Gomez" else "Ana Ruiz", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                                Text(if (index == 0) "Ing. en Sistemas" else "Administración", color = Color.Gray, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp)) }
                                    Text(" 5.0", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { }, modifier = Modifier.background(BackgroundGray, CircleShape).size(36.dp)) {
                                Icon(Icons.Filled.Chat, contentDescription = "Chat", tint = NavyBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { }, modifier = Modifier.background(OrangePrimary, CircleShape).size(36.dp)) {
                                Icon(Icons.Filled.Phone, contentDescription = "Llamar", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. PANTALLA DE LUGARES FRECUENTES
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LugaresScreen(onBack: () -> Unit) {
    val lugares = listOf("ITSCC (Campus)", "Bimart Villa Morelos", "Mi Casa", "Plaza Comercial")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lugares Frecuentes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, tint = Color.White, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(lugares.size) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (index == 2) Icons.Filled.Home else Icons.Filled.LocationCity, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(lugares[index], fontWeight = FontWeight.Bold, color = NavyBlue)
                                Text("Visitado ${5 - index} veces esta semana", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. PANTALLA DE HISTORIAL (Con Botón de Pánico)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Viajes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, tint = Color.White, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Ejemplo Normal
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ayer, 14:30 hrs", color = Color.Gray, fontSize = 12.sp)
                            Text("Completado", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ITSCC → Cd. Insurgentes", fontWeight = FontWeight.Bold, color = NavyBlue)
                        Text("Chofer: Mario Gomez", fontSize = 14.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row { repeat(5) { Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp)) } }
                            Text("Donativo: $20 MXN", fontWeight = FontWeight.Bold, color = OrangePrimary)
                        }
                    }
                }
            }
            // Ejemplo Botón de Pánico
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).border(1.dp, Color.Red, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alerta de Seguridad Activada", fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                        Text("Hace 2 semanas - Viaje a Villa Morelos", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                        Text("El protocolo institucional fue activado. Viaje cancelado.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PANTALLA DE DISPONIBLES (Viajes Activos)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisponiblesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choferes Disponibles", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, tint = Color.White, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(4) { index ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(OrangePrimary), contentAlignment = Alignment.Center) {
                                    Text("C", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Carlos M.", fontWeight = FontWeight.Bold, color = NavyBlue)
                                    Text("Destino: Cd. Constitución", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Badge(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)) {
                                Text("Saliendo en 10m", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Vehículo", fontSize = 10.sp, color = Color.Gray)
                                Text("Nissan Versa Gris", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = NavyBlue), modifier = Modifier.height(36.dp)) {
                                Text("Pedir Raite", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. PANTALLA DE LOCALIDADES
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalidadesScreen(onBack: () -> Unit) {
    val locs = listOf("Cd. Constitución" to 340, "Cd. Insurgentes" to 125, "Puerto San Carlos" to 45, "Villa Morelos" to 22)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidades UNRAIT", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, tint = Color.White, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(locs.size) { index ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(locs[index].first, fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${locs[index].second}", fontWeight = FontWeight.ExtraBold, color = OrangePrimary, fontSize = 18.sp)
                            Text("Usuarios activos", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. PANTALLA DE AJUSTES
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onBack: () -> Unit) {
    var notifViajes by remember { mutableStateOf(true) }
    var notifMensajes by remember { mutableStateOf(true) }
    var compartirUbicacion by remember { mutableStateOf(true) }
    var modoAhorroDatos by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, tint = Color.White, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // SECCIÓN 1: NOTIFICACIONES
            Text("Notificaciones", fontWeight = FontWeight.Bold, color = NavyBlue, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Alertas de viajes disponibles", color = Color.DarkGray)
                        Switch(checked = notifViajes, onCheckedChange = { notifViajes = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
                    }
                    HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mensajes del conductor/pasajero", color = Color.DarkGray)
                        Switch(checked = notifMensajes, onCheckedChange = { notifMensajes = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN 2: SEGURIDAD Y PRIVACIDAD
            Text("Seguridad y Privacidad", fontWeight = FontWeight.Bold, color = NavyBlue, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Compartir ubicación en tiempo real", color = Color.DarkGray)
                        Switch(checked = compartirUbicacion, onCheckedChange = { compartirUbicacion = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
                    }
                    HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Contactos de emergencia (SOS)", color = Color.DarkGray)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN 3: APLICACIÓN
            Text("Aplicación", fontWeight = FontWeight.Bold, color = NavyBlue, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Ahorro de datos en mapas", color = Color.DarkGray)
                        Switch(checked = modoAhorroDatos, onCheckedChange = { modoAhorroDatos = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
                    }
                    HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Términos y Condiciones", color = Color.DarkGray)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text("UNRAIT v1.0.0", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}