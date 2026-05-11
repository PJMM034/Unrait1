package ui.screens.ride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorScreen(onBack: () -> Unit) {
    var isOnline by remember { mutableStateOf(false) }
    var showRequest by remember { mutableStateOf(false) }
    
    // Estados para las hojas modales
    var showProfileSheet by remember { mutableStateOf(false) }
    var showPassengersSheet by remember { mutableStateOf(false) }
    var showRouteSheet by remember { mutableStateOf(false) }

    val itscc = LatLng(25.044167, -111.639243)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(itscc, 15f)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isOnline) "EN LÍNEA" else "DESCONECTADO",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (isOnline) {
                            Text("Buscando viajes...", fontSize = 12.sp, color = OrangePrimary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = NavyBlue)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // MAPA DE FONDO
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true)
            )

            // PANEL SUPERIOR DE GANANCIAS
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$245.00", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                    Text(" 4.9", fontWeight = FontWeight.Bold, color = NavyBlue)
                }
            }

            // PANEL DE CONTROL INFERIOR (LOS 3 BOTONES SOLICITADOS)
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // FILA DE 3 BOTONES DE ACCIÓN
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 1. Información de Conductor y Vehículo
                        ActionIconButton(
                            icon = Icons.Default.Person,
                            label = "Vehículo",
                            onClick = { showProfileSheet = true }
                        )
                        // 2. Personas vinculadas (Pasajeros)
                        ActionIconButton(
                            icon = Icons.Default.Groups,
                            label = "Pasajeros",
                            onClick = { showPassengersSheet = true }
                        )
                        // 3. Ruta de viaje (Usuario y Destino)
                        ActionIconButton(
                            icon = Icons.Default.DirectionsCar,
                            label = "Ruta",
                            color = OrangePrimary,
                            onClick = { showRouteSheet = true }
                        )
                    }

                    Button(
                        onClick = { isOnline = !isOnline },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isOnline) Color(0xFFE53935) else OrangePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isOnline) "DESCONECTARSE" else "INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                    if (isOnline) {
                        TextButton(onClick = { showRequest = true }) { Text("Simular solicitud de viaje", color = NavyBlue) }
                    }
                }
            }

            // --- HOJAS MODALES (BOTTOM SHEETS) ---

            // 1. INFO CONDUCTOR Y VEHÍCULO
            if (showProfileSheet) {
                ModalBottomSheet(onDismissRequest = { showProfileSheet = false }, containerColor = Color.White) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Mi Perfil de Conductor", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(60.dp).background(BackgroundGray, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(50.dp), tint = NavyBlue)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Carlos Martínez", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Conductor Verificado", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Vehículo Registrado", fontWeight = FontWeight.Bold, color = OrangePrimary)
                        Text("Toyota Corolla 2022 • Blanco", fontSize = 16.sp)
                        Text("Placas: CZ-4521-B", fontWeight = FontWeight.Bold, color = NavyBlue)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // 2. PERSONAS VINCULADAS (PASAJEROS)
            if (showPassengersSheet) {
                ModalBottomSheet(onDismissRequest = { showPassengersSheet = false }, containerColor = Color.White) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Pasajeros en el Viaje", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        repeat(2) { index ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = OrangePrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(if(index == 0) "Juan Pérez" else "María García", fontWeight = FontWeight.Medium)
                                    Text("Punto: ITSCC", fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = NavyBlue) }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // 3. RUTA DE VIAJE
            if (showRouteSheet) {
                ModalBottomSheet(onDismissRequest = { showRouteSheet = false }, containerColor = Color.White) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Detalles de la Ruta", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Icon(Icons.Default.RadioButtonChecked, null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Recoger en (Usuario):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Entrada Principal ITSCC", color = Color.Gray)
                            }
                        }
                        Box(modifier = Modifier.padding(start = 11.dp).width(2.dp).height(30.dp).background(Color.LightGray))
                        Row {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Destino Final:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Plaza Constitución, Centro", color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { showRouteSheet = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)) {
                            Text("ENTENDIDO", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // SOLICITUD ENTRANTE (Simulada)
            AnimatedVisibility(visible = showRequest, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = NavyBlue), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("NUEVA SOLICITUD", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Juan Pérez", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { showRequest = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("RECHAZAR", color = Color.White) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(onClick = { showRequest = false; showRouteSheet = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) { Text("ACEPTAR", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = NavyBlue
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFFF5F6F8),
            contentColor = color,
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

private val BackgroundGray = Color(0xFFF5F6F8)
