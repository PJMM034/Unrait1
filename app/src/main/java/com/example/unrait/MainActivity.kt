package com.example.unrait

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.unrait.ui.theme.UnraitTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// Colores de la app
val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)
val WhiteBackground = Color(0xFFF5F5F5)

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

@Composable
fun HomeScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    when (mostrarPantalla.value) {
        "main" -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                // DESACTIVAR GESTOS SI EL DRAWER ESTÁ CERRADO
                // Esto permite mover el mapa sin abrir el menú lateral
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    DrawerContent(
                        alCerrarDrawer = { scope.launch { drawerState.close() } }
                    )
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = NavyBlue,
                    topBar = {
                        TopSection(onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        })
                    },
                    bottomBar = { BottomNavSection() }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val laPaz = remember { LatLng(-16.4897, -68.1193) }
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(laPaz, 13f)
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState
                        ) {
                            Marker(
                                state = rememberMarkerState(position = laPaz),
                                title = "La Paz",
                                snippet = "Centro de la ciudad"
                            )
                        }
                    }
                }
            }
        }
        "login" -> LoginScreen()
        "registro" -> RegistroScreen()
    }
}

@Composable
fun DrawerContent(alCerrarDrawer: () -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Foto de perfil",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Carlos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Estudiante - Ing. en Sistemas", color = Color.LightGray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DrawerMenuItem(icon = Icons.Filled.People, text = "Amigos")
        DrawerMenuItem(icon = Icons.Filled.Place, text = "Lugares")
        DrawerMenuItem(icon = Icons.Filled.History, text = "Historial")
        DrawerMenuItem(icon = Icons.Filled.DirectionsBus, text = "Viajes")
        DrawerMenuItem(icon = Icons.Filled.CheckCircle, text = "Disponibles")
        DrawerMenuItem(icon = Icons.Filled.LocationCity, text = "Localidades")

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    alCerrarDrawer()
                    mostrarPantalla.value = "login"
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Login,
                contentDescription = "Iniciar Sesión",
                tint = OrangePrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Iniciar Sesión",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = NavyBlue
            )
        }
    }
}

@Composable
fun DrawerMenuItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = OrangePrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = NavyBlue
        )
    }
}

@Composable
fun TopSection(onOpenDrawer: () -> Unit) {
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
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Perfil",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "UNRAIT",
                color = OrangePrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Ajustes", color = NavyBlue) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Filled.Settings, tint = Color.Gray, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Comentarios", color = NavyBlue) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Filled.Comment, tint = Color.Gray, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var searchQuery by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                placeholder = { Text("Buscar destino en La Paz...") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = OrangePrimary,
                    cursorColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun BottomNavSection() {
    var selectedItem by remember { mutableIntStateOf(1) }
    var showModal by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 8.dp)
            .height(70.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedNavItem(
            icon = Icons.Filled.Home,
            isSelected = selectedItem == 1,
            isCenter = true
        ) {
            selectedItem = 1
        }

        AnimatedNavItem(
            icon = Icons.Filled.Search,
            isSelected = selectedItem == 1,
            isCenter = true
        ){
            selectedItem = 1
            showModal = true
        }

        AnimatedNavItem(
            icon = Icons.Filled.DirectionsBus,
            isSelected = selectedItem == 2
        ) {
            selectedItem = 2
        }
    }

    if (showModal) {
        SearchModal(onClose = { showModal = false })
    }
}

@Composable
fun SearchModal(onClose: () -> Unit) {
    var origen by remember { mutableStateOf("Mi ubicación") }
    var destino by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "¿A dónde vamos?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = origen,
                    onValueChange = { origen = it },
                    label = { Text("Punto de partida") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = destino,
                    onValueChange = { destino = it },
                    label = { Text("Destino") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Buscar")
                }
            }
        }
    )
}

@Composable
fun AnimatedNavItem(
    icon: ImageVector,
    isSelected: Boolean,
    isCenter: Boolean = false,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "scale"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(if (isCenter) 60.dp else 48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isSelected && isCenter) OrangePrimary else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected && isCenter) Color.White
            else if (isSelected) OrangePrimary
            else Color.Gray,
            modifier = Modifier.size(if (isCenter) 32.dp else 28.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    UnraitTheme {
        HomeScreen()
    }
}
