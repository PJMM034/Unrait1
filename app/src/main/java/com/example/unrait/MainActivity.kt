package com.example.unrait

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Imports de Google Maps
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

// IMPORTS DE TUS OTRAS PANTALLAS
import com.example.unrait.ui.screens.auth.LoginScreen
import com.example.unrait.ui.screens.auth.RegistroScreen
import com.example.unrait.ui.screens.ride.PedirRaiteScreen
import com.example.unrait.ui.screens.ride.DetallesViajeScreen
import com.example.unrait.ui.screens.ride.SeguimientoViajeScreen
import com.example.unrait.ui.screens.ride.ConductorScreen
import com.example.unrait.ui.theme.UnraitTheme

import com.example.unrait.ui.screens.drawer.AmigosScreen
import com.example.unrait.ui.screens.drawer.DisponiblesScreen
import com.example.unrait.ui.screens.drawer.HistorialScreen
import com.example.unrait.ui.screens.drawer.LocalidadesScreen
import com.example.unrait.ui.screens.drawer.LugaresScreen
import com.example.unrait.ui.screens.drawer.ProfileScreen
import com.example.unrait.ui.screens.drawer.AjustesScreen

// Colores de la app
val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val WhiteBackground = Color(0xFFF5F5F5)
val DarkGrayText = Color(0xFF4A4A4A)

// Variable para controlar qué pantalla mostrar
var mostrarPantalla = mutableStateOf("main")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnraitTheme {
                HomeScreen()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Estado para el cuadro emergente de comentarios
    var showComentariosModal by remember { mutableStateOf(false) }

    // --- VARIABLES DE PASO TEMPORAL PARA AUTENTICACIÓN INSTITUCIONAL ---
    var firebaseUidTmp by remember { mutableStateOf("") }
    var nombreTmp by remember { mutableStateOf("") }
    var numControlTmp by remember { mutableStateOf("") }

    // --- INTERCEPTAR EL BOTÓN FÍSICO DE ATRÁS ---
    BackHandler(enabled = mostrarPantalla.value != "main" && mostrarPantalla.value != "login") {
        when (mostrarPantalla.value) {
            "detalles_viaje" -> mostrarPantalla.value = "pedir_raite"
            "registro" -> mostrarPantalla.value = "login"
            "conductor" -> mostrarPantalla.value = "main"
            else -> mostrarPantalla.value = "main"
        }
    }

    // --- PERMISOS ---
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    // --- ESTADO DEL MAPA ---
    val defaultLocation = LatLng(25.044167, -111.639243) // ITSCC por defecto
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // --- CONDUCTORES DE PRUEBA ---
    val conductoresCercanos = listOf(
        LatLng(25.0460, -111.6400),
        LatLng(25.0420, -111.6350),
        LatLng(25.0480, -111.6450)
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                    }
                }
            }
        }
    }

    when (mostrarPantalla.value) {
        "main" -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = { DrawerContent(alCerrarDrawer = { scope.launch { drawerState.close() } }) }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = NavyBlue,
                    topBar = {
                        TopSection(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNotificationClick = { mostrarPantalla.value = "seguimiento_viaje" },
                            onOpenAjustes = { mostrarPantalla.value = "ajustes" },
                            onOpenComentarios = { showComentariosModal = true }
                        )
                    },
                    bottomBar = { BottomNavSection() }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White)
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
                        ) {
                            conductoresCercanos.forEachIndexed { index, pos ->
                                Marker(
                                    state = rememberMarkerState(position = pos),
                                    title = "Conductor Disponible",
                                    snippet = "Destino: Centro"
                                )
                            }
                        }
                    }
                }
            }

            // --- MODAL DE COMENTARIOS ---
            if (showComentariosModal) {
                ComentariosModal(onClose = { showComentariosModal = false })
            }
        }

        // --- AQUÍ ESTÁ LA NUEVA CONEXIÓN DE LOGIN/REGISTRO ---
        "login" -> LoginScreen(
            onNavigateToHome = { mostrarPantalla.value = "main" },
            onNavigateToRegistro = { uid, nombre, control ->
                firebaseUidTmp = uid
                nombreTmp = nombre
                numControlTmp = control
                mostrarPantalla.value = "registro"
            }
        )

        "registro" -> RegistroScreen(
            firebaseUid = firebaseUidTmp,
            nombreInicial = nombreTmp,
            numControlInicial = numControlTmp,
            onNavigateToHome = { mostrarPantalla.value = "main" },
            onNavigateToLogin = { mostrarPantalla.value = "login" }
        )

        // --- EL RESTO DE TUS PANTALLAS ---
        "pedir_raite" -> PedirRaiteScreen(onBack = { mostrarPantalla.value = "main" })
        "detalles_viaje" -> DetallesViajeScreen(onBack = { mostrarPantalla.value = "pedir_raite" })
        "seguimiento_viaje" -> SeguimientoViajeScreen(onFinalizar = { mostrarPantalla.value = "main" })
        "perfil" -> ProfileScreen(onBack = { mostrarPantalla.value = "main" })
        "amigos" -> AmigosScreen(onBack = { mostrarPantalla.value = "main" })
        "lugares" -> LugaresScreen(onBack = { mostrarPantalla.value = "main" })
        "historial" -> HistorialScreen(onBack = { mostrarPantalla.value = "main" })
        "disponibles" -> DisponiblesScreen(onBack = { mostrarPantalla.value = "main" })
        "localidades" -> LocalidadesScreen(onBack = { mostrarPantalla.value = "main" })
        "ajustes" -> AjustesScreen(onBack = { mostrarPantalla.value = "main" })
        "conductor" -> ConductorScreen(onBack = { mostrarPantalla.value = "main" })
    }
}

// =====================================================================
// COMPONENTE: MODAL DE COMENTARIOS ANIMADO
// =====================================================================
@Composable
fun ComentariosModal(onClose: () -> Unit) {
    var tema by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Animación de la palomita
    val scale by animateFloatAsState(
        targetValue = if (enviado) 1.5f else 0f,
        animationSpec = tween(durationMillis = 500, easing = { androidx.compose.animation.core.FastOutSlowInEasing.transform(it) }),
        label = "escala_palomita"
    )

    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp)

    AlertDialog(
        onDismissRequest = { if (!enviado) onClose() },
        confirmButton = {},
        containerColor = Color.White,
        title = {
            if (!enviado) Text("Envíanos tus comentarios", color = NavyBlue, fontWeight = FontWeight.Bold)
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (!enviado) {
                    Column {
                        Text("Tu opinión nos ayuda a mejorar UNRAIT.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

                        OutlinedTextField(
                            value = tema,
                            onValueChange = { tema = it },
                            label = { Text("Tema") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = textStyleDark,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                focusedLabelColor = OrangePrimary,
                                unfocusedLabelColor = Color.DarkGray,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5,
                            textStyle = textStyleDark,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                focusedLabelColor = OrangePrimary,
                                unfocusedLabelColor = Color.DarkGray,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onClose) { Text("Cancelar", color = Color.Gray) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    enviado = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        onClose()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                enabled = tema.isNotEmpty() && descripcion.isNotEmpty()
                            ) {
                                Text("Enviar", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 32.dp)) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Éxito",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(60.dp).scale(scale)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¡Gracias por tu opinión!", color = NavyBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Lo tomaremos en cuenta para mejorar.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    )
}
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscadorComondu(
    valorActual: String,
    onCambioValor: (String) -> Unit,
    placeholder: String,
    label: String? = null,
    esTransparente: Boolean = false,
    modifier: Modifier = Modifier
) {
    val localidades = listOf("ITSCC (Tecnológico)", "Ciudad Constitución", "Ciudad Insurgentes", "Puerto San Carlos", "Villa Morelos", "Santo Domingo", "La Purísima", "San Isidro", "San Juanico", "Ignacio Zaragoza", "Francisco Villa", "Palo Bola")
    var expandido by remember { mutableStateOf(false) }
    val opcionesFiltradas = localidades.filter { it.contains(valorActual, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expandido && opcionesFiltradas.isNotEmpty() && valorActual.isNotEmpty(),
        onExpandedChange = { expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valorActual,
            onValueChange = {
                onCambioValor(it)
                expandido = it.isNotEmpty()
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = TextStyle(fontSize = 15.sp, color = NavyBlue),
            placeholder = { Text(text = placeholder, color = DarkGrayText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            label = if (label != null) { { Text(label, color = DarkGrayText) } } else null,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NavyBlue) },
            trailingIcon = {
                if (valorActual.isNotEmpty()) {
                    IconButton(onClick = { onCambioValor(""); expandido = false }) { Icon(Icons.Filled.Clear, contentDescription = "Borrar todo", tint = NavyBlue) }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                unfocusedBorderColor = if (esTransparente) Color.Transparent else Color.LightGray,
                focusedBorderColor = OrangePrimary, cursorColor = OrangePrimary
            ),
            singleLine = true
        )

        if (opcionesFiltradas.isNotEmpty() && valorActual.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false },
                modifier = Modifier.background(Color.White)
            ) {
                opcionesFiltradas.forEach { localidad ->
                    DropdownMenuItem(
                        text = { Text(localidad, color = NavyBlue, fontWeight = FontWeight.Medium) },
                        onClick = { onCambioValor(localidad); expandido = false },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, tint = OrangePrimary, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopSection(
    onOpenDrawer: () -> Unit,
    onNotificationClick: () -> Unit,
    onOpenAjustes: () -> Unit,
    onOpenComentarios: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text("UNRAIT", color = OrangePrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = Color.White)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color.White)) {
                    DropdownMenuItem(
                        text = { Text("Ajustes", color = NavyBlue) },
                        onClick = { showMenu = false; onOpenAjustes() },
                        leadingIcon = { Icon(Icons.Filled.Settings, tint = Color.Gray, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Comentarios", color = NavyBlue) },
                        onClick = { showMenu = false; onOpenComentarios() },
                        leadingIcon = { Icon(Icons.Filled.Comment, tint = Color.Gray, contentDescription = null) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            var searchQuery by remember { mutableStateOf("") }

            BuscadorComondu(
                valorActual = searchQuery,
                onCambioValor = { searchQuery = it },
                placeholder = "¿A dónde vas?",
                esTransparente = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(OrangePrimary).align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
fun BottomNavSection() {
    var showModal by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 8.dp)
            .height(70.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedNavItem(icon = Icons.Filled.Home, isSelected = true) {}
        AnimatedNavItem(icon = Icons.Filled.Search, isSelected = false) { showModal = true }
        AnimatedNavItem(icon = Icons.Filled.DirectionsBus, isSelected = false) { mostrarPantalla.value = "pedir_raite" }
    }

    if (showModal) {
        SearchModal(onClose = { showModal = false })
    }
}

@Composable
fun SearchModal(onClose: () -> Unit) {
    var origen by remember { mutableStateOf("ITSCC (Tecnológico)") }
    var destino by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "¿A dónde vamos?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray) }
                }
                Spacer(modifier = Modifier.height(10.dp))
                BuscadorComondu(valorActual = origen, onCambioValor = { origen = it }, label = "Punto de partida", placeholder = "Origen...")
                Spacer(modifier = Modifier.height(12.dp))
                BuscadorComondu(valorActual = destino, onCambioValor = { destino = it }, label = "Destino", placeholder = "Destino...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onClose(); mostrarPantalla.value = "pedir_raite" },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = origen.isNotEmpty() && destino.isNotEmpty()
                ) { Text("Buscar Conductores", fontWeight = FontWeight.Bold) }
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun DrawerContent(alCerrarDrawer: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = Color.White, modifier = Modifier.width(300.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
                .clickable { alCerrarDrawer(); mostrarPantalla.value = "perfil" }
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Foto de perfil", tint = Color.White, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Carlos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Estudiante - Ing. en Sistemas", color = Color.LightGray, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        DrawerMenuItem(icon = Icons.Filled.People, text = "Amigos") { alCerrarDrawer(); mostrarPantalla.value = "amigos" }
        DrawerMenuItem(icon = Icons.Filled.Place, text = "Lugares") { alCerrarDrawer(); mostrarPantalla.value = "lugares" }
        DrawerMenuItem(icon = Icons.Filled.History, text = "Historial") { alCerrarDrawer(); mostrarPantalla.value = "historial" }
        DrawerMenuItem(icon = Icons.Filled.DirectionsBus, text = "Viajes / Disponibles") { alCerrarDrawer(); mostrarPantalla.value = "disponibles" }
        DrawerMenuItem(icon = Icons.Filled.LocationCity, text = "Localidades") { alCerrarDrawer(); mostrarPantalla.value = "localidades" }
        DrawerMenuItem(icon = Icons.Filled.DriveEta, text = "Modo Conductor") { alCerrarDrawer(); mostrarPantalla.value = "conductor" }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().clickable { alCerrarDrawer(); mostrarPantalla.value = "login" }.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Login, contentDescription = "Iniciar Sesión", tint = OrangePrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NavyBlue)
        }
    }
}

@Composable
fun DrawerMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = text, tint = OrangePrimary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NavyBlue)
    }
}

@Composable
fun AnimatedNavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1.0f, animationSpec = tween(durationMillis = 300), label = "scale")
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier.size(56.dp).scale(scale).clip(CircleShape).background(if (isSelected) OrangePrimary else Color.Transparent).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color.White else OrangePrimary, modifier = Modifier.size(28.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() { UnraitTheme { HomeScreen() } }