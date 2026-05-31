package com.example.unrait

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.google.firebase.auth.FirebaseAuth

import com.example.unrait.network.UnraitApi
import com.example.unrait.network.Viaje
import com.example.unrait.network.SolicitarViajeRequest
import com.example.unrait.network.PeticionPasajero

val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val WhiteBackground = Color(0xFFF5F5F5)
val DarkGrayText = Color(0xFF4A4A4A)

var mostrarPantalla = mutableStateOf("main")
var usuarioEsConductorGlobal = mutableStateOf(false)

fun base64ToBitmapGlobal(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrEmpty()) return null
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) { null }
}

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

    var showComentariosModal by remember { mutableStateOf(false) }
    var showSearchModal by remember { mutableStateOf(false) }

    var firebaseUidTmp by remember { mutableStateOf("") }
    var nombreTmp by remember { mutableStateOf("") }
    var numControlTmp by remember { mutableStateOf("") }

    var peticionesActivasMain by remember { mutableStateOf<List<PeticionPasajero>>(emptyList()) }
    var fotoPerfilGlobal by remember { mutableStateOf<Bitmap?>(null) }

    // --- ESTADO PARA CONTROLAR LA PANTALLA DE CARGA ---
    var showSplash by remember { mutableStateOf(true) }
    val splashScale = remember { Animatable(0f) }

    // --- LÓGICA DEL SPLASH SCREEN ---
    LaunchedEffect(Unit) {
        // Efecto de rebote
        splashScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = { OvershootInterpolator(1.5f).getInterpolation(it) })
        )
        // Mantenemos el logo 1.5 segundos
        delay(1500)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            mostrarPantalla.value = "login"
        } else {
            mostrarPantalla.value = "main"
        }
        showSplash = false // Quitamos la pantalla de carga
    }

    LaunchedEffect(mostrarPantalla.value) {
        if (mostrarPantalla.value == "main" && !showSplash) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                try {
                    val res = UnraitApi.retrofitService.getPerfil(currentUser.uid)
                    if (res.success && res.perfil != null) {
                        fotoPerfilGlobal = base64ToBitmapGlobal(res.perfil.foto_perfil)
                        usuarioEsConductorGlobal.value = res.perfil.es_conductor == 1
                    }
                } catch (e: Exception) { }
            }
        }
    }

    BackHandler(enabled = mostrarPantalla.value != "main" && mostrarPantalla.value != "login" && !showSplash) {
        when (mostrarPantalla.value) {
            "detalles_viaje" -> mostrarPantalla.value = "pedir_raite"
            "registro" -> mostrarPantalla.value = "login"
            "conductor" -> mostrarPantalla.value = "main"
            else -> mostrarPantalla.value = "main"
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    val defaultLocation = LatLng(25.044167, -111.639243)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    var mapCentered by remember { mutableStateOf(false) }

    // --- AQUÍ EVITAMOS QUE PIDA PERMISOS MIENTRAS CARGA ---
    LaunchedEffect(hasLocationPermission, showSplash) {
        if (!showSplash) {
            if (!hasLocationPermission) {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else if (!mapCentered) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f))
                            mapCentered = true
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(usuarioEsConductorGlobal.value, mostrarPantalla.value, showSplash) {
        if (usuarioEsConductorGlobal.value && mostrarPantalla.value == "main" && !showSplash) {
            while (true) {
                try {
                    val res = UnraitApi.retrofitService.getPeticionesActivas()
                    if (res.success) {
                        peticionesActivasMain = res.peticiones
                    }
                } catch (e: Exception) {}
                delay(5000)
            }
        }
    }

    // --- CAPA DE LA APLICACIÓN ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (!showSplash) {
            when (mostrarPantalla.value) {
                "main" -> {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = drawerState.isOpen,
                        drawerContent = { DrawerContent(alCerrarDrawer = { scope.launch { drawerState.close() } }, fotoPerfil = fotoPerfilGlobal) }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = NavyBlue,
                            topBar = {
                                TopSection(
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onNotificationClick = { mostrarPantalla.value = "seguimiento_viaje" },
                                    onOpenAjustes = { mostrarPantalla.value = "ajustes" },
                                    onOpenComentarios = { showComentariosModal = true },
                                    onOpenSearch = { showSearchModal = true },
                                    fotoPerfil = fotoPerfilGlobal
                                )
                            },
                            bottomBar = { BottomNavSection() }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(32.dp)).background(Color.White)
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
                                )

                                AnimatedVisibility(
                                    visible = usuarioEsConductorGlobal.value && peticionesActivasMain.isNotEmpty(),
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { mostrarPantalla.value = "conductor" },
                                        colors = CardDefaults.cardColors(containerColor = NavyBlue),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("¡Pasajeros esperando!", fontWeight = FontWeight.ExtraBold, color = OrangePrimary, fontSize = 16.sp)
                                                Text("Hay ${peticionesActivasMain.size} compañeros buscando raite ahora mismo.", color = Color.White, fontSize = 12.sp)
                                            }
                                            Box(
                                                modifier = Modifier.background(OrangePrimary, CircleShape).padding(12.dp)
                                            ) {
                                                Icon(Icons.Filled.DirectionsCar, contentDescription = "Ir", tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showComentariosModal) { ComentariosModal(onClose = { showComentariosModal = false }) }
                    if (showSearchModal) { SearchModal(onClose = { showSearchModal = false }) }
                }
                "login" -> LoginScreen(onNavigateToHome = { mostrarPantalla.value = "main" }, onNavigateToRegistro = { uid, nombre, control -> firebaseUidTmp = uid; nombreTmp = nombre; numControlTmp = control; mostrarPantalla.value = "registro" })
                "registro" -> RegistroScreen(firebaseUid = firebaseUidTmp, nombreInicial = nombreTmp, numControlInicial = numControlTmp, onNavigateToHome = { mostrarPantalla.value = "main" }, onNavigateToLogin = { mostrarPantalla.value = "login" })
                "pedir_raite" -> PedirRaiteScreen(onBack = { mostrarPantalla.value = "main" }, onSolicitudEnviada = { mostrarPantalla.value = "seguimiento_viaje" })
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

        // --- CAPA DE LA PANTALLA DE CARGA ---
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyBlue),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(2.dp, OrangePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar, // Se verá excelente cuando pongas tu logo
                            contentDescription = "Logo",
                            tint = OrangePrimary,
                            modifier = Modifier
                                .size(60.dp)
                                .scale(splashScale.value)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "UNRAIT",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp,
                        modifier = Modifier.scale(splashScale.value)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Comunidad en Movimiento",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.scale(splashScale.value)
                    )
                }
            }
        }
    }
}

@Composable
fun TopSection(onOpenDrawer: () -> Unit, onNotificationClick: () -> Unit, onOpenAjustes: () -> Unit, onOpenComentarios: () -> Unit, onOpenSearch: () -> Unit, fotoPerfil: Bitmap?) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onOpenDrawer() },
                contentAlignment = Alignment.Center
            ) {
                if (fotoPerfil != null) {
                    Image(
                        bitmap = fotoPerfil.asImageBitmap(),
                        contentDescription = "Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil", tint = NavyBlue, modifier = Modifier.size(36.dp))
                }
            }

            Text("UNRAIT", color = OrangePrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = Color.White) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color.White)) {
                    DropdownMenuItem(text = { Text("Ajustes", color = NavyBlue) }, onClick = { showMenu = false; onOpenAjustes() }, leadingIcon = { Icon(Icons.Filled.Settings, tint = Color.Gray, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Comentarios", color = NavyBlue) }, onClick = { showMenu = false; onOpenComentarios() }, leadingIcon = { Icon(Icons.Filled.Comment, tint = Color.Gray, contentDescription = null) })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .background(Color.White, RoundedCornerShape(25.dp))
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = NavyBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿A dónde vas?", color = Color.Gray, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onNotificationClick) { Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = Color.White, modifier = Modifier.size(28.dp)) }
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(OrangePrimary).align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
fun BottomNavSection() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp).height(70.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        AnimatedNavItem(icon = Icons.Filled.Home, isSelected = true) {}
        AnimatedNavItem(icon = Icons.Filled.History, isSelected = false) { mostrarPantalla.value = "historial" }
        AnimatedNavItem(icon = Icons.Filled.DirectionsBus, isSelected = false) { mostrarPantalla.value = "pedir_raite" }
    }
}

@Composable
fun SearchModal(onClose: () -> Unit) {
    var destino by remember { mutableStateOf("") }
    var viajesList by remember { mutableStateOf<List<Viaje>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Buscar Viajes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyBlue)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray) }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = destino,
                    onValueChange = { destino = it },
                    label = { Text("Destino (Ej. ITSCC, Insurgentes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    trailingIcon = {
                        if (destino.isNotEmpty()) {
                            IconButton(onClick = { destino = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Borrar", tint = NavyBlue) }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isSearching = true
                        scope.launch {
                            try {
                                val response = UnraitApi.retrofitService.getViajesDisponibles()
                                if (response.success) {
                                    viajesList = response.viajes.filter { it.destino.contains(destino, ignoreCase = true) }
                                    hasSearched = true
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error conectando al servidor", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSearching = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    enabled = destino.isNotEmpty() && !isSearching
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Buscar Conductores", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (hasSearched) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (viajesList.isEmpty()) {
                        Text("No se encontraron conductores hacia '$destino'.", color = Color.Red, fontSize = 14.sp)
                    } else {
                        Text("Resultados:", fontWeight = FontWeight.Bold, color = NavyBlue)
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp).padding(top = 8.dp)) {
                            items(viajesList) { viaje ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(viaje.nombre_conductor, fontWeight = FontWeight.Bold, color = NavyBlue)
                                        Text("Saliendo a las: ${viaje.hora_salida}", fontSize = 12.sp, color = Color.Gray)
                                        Text("Destino exacto: ${viaje.destino}", fontSize = 12.sp, color = Color.DarkGray)

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
                                                                onClose()
                                                                mostrarPantalla.value = "seguimiento_viaje"
                                                            }
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error al solicitar", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) { Text("Solicitar Raite") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun DrawerContent(alCerrarDrawer: () -> Unit, fotoPerfil: Bitmap?) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val nombreMostrar = currentUser?.displayName ?: "Usuario UNRAIT"
    val email = currentUser?.email ?: ""

    ModalDrawerSheet(drawerContainerColor = Color.White, modifier = Modifier.width(300.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(NavyBlue).clickable { alCerrarDrawer(); mostrarPantalla.value = "perfil" }.padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fotoPerfil != null) {
                            Image(
                                bitmap = fotoPerfil.asImageBitmap(),
                                contentDescription = "Perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Foto de perfil", tint = NavyBlue, modifier = Modifier.size(72.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Filled.Edit, contentDescription = "Editar Perfil", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(nombreMostrar, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(email, color = Color.LightGray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("MODO PASAJERO", modifier = Modifier.padding(start = 24.dp, top = 8.dp), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        DrawerMenuItem(icon = Icons.Filled.DirectionsBus, text = "Viajes Disponibles") { alCerrarDrawer(); mostrarPantalla.value = "disponibles" }
        DrawerMenuItem(icon = Icons.Filled.History, text = "Mi Historial de Viajes") { alCerrarDrawer(); mostrarPantalla.value = "historial" }

        HorizontalDivider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

        Text("COMUNIDAD", modifier = Modifier.padding(start = 24.dp, top = 8.dp), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        DrawerMenuItem(icon = Icons.Filled.People, text = "Mis Choferes de Confianza") { alCerrarDrawer(); mostrarPantalla.value = "amigos" }
        DrawerMenuItem(icon = Icons.Filled.Place, text = "Lugares Frecuentes") { alCerrarDrawer(); mostrarPantalla.value = "lugares" }
        DrawerMenuItem(icon = Icons.Filled.LocationCity, text = "Localidades UNRAIT") { alCerrarDrawer(); mostrarPantalla.value = "localidades" }

        HorizontalDivider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

        if (usuarioEsConductorGlobal.value) {
            Text("ÁREA DE CONDUCTOR", modifier = Modifier.padding(start = 24.dp, top = 8.dp), color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            DrawerMenuItem(icon = Icons.Filled.DriveEta, text = "Conectarse (Ofrecer Viajes)") { alCerrarDrawer(); mostrarPantalla.value = "conductor" }
        }
        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                FirebaseAuth.getInstance().signOut()
                alCerrarDrawer()
                mostrarPantalla.value = "login"
            }.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Login, contentDescription = "Cerrar Sesión", tint = Color.Red, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
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

@Composable
fun ComentariosModal(onClose: () -> Unit) {
    var tema by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                                        delay(2000)
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