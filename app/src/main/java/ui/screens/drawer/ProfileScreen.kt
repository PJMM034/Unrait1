package com.example.unrait.ui.screens.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    // Datos editables (Cargados con la info por defecto)
    var nombre by remember { mutableStateOf("Carlos Armando Montelongo Orozco") }
    var carrera by remember { mutableStateOf("Ingeniería en Sistemas Computacionales") }
    var institucion by remember { mutableStateOf("ITSCC") }
    val numControl = "223110210" // No editable

    // Estado de Conductor
    var esConductor by remember { mutableStateOf(false) }
    var placas by remember { mutableStateOf("") }
    var modeloVehiculo by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // --- ESTILO FORZADO PARA LOS CAMPOS DE TEXTO ---
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = OrangePrimary,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = OrangePrimary,
        unfocusedLabelColor = Color.DarkGray,
        cursorColor = OrangePrimary
    )
    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { /* Guardar cambios */ }) {
                        Text("GUARDAR", color = OrangePrimary, fontWeight = FontWeight.Bold)
                    }
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
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECCIÓN FOTO DE PERFIL
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, OrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = NavyBlue
                    )
                }
                FloatingActionButton(
                    onClick = { /* Abrir Galería */ },
                    modifier = Modifier.size(40.dp),
                    containerColor = OrangePrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DATOS ESCOLARES
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Información Académica", fontWeight = FontWeight.Bold, color = NavyBlue)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Número de Control (Bloqueado)
                    OutlinedTextField(
                        value = numControl,
                        onValueChange = {},
                        label = { Text("Número de Control") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        textStyle = textStyleDark,
                        colors = textFieldColors,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray) },
                        supportingText = { Text("Para cambios contacte al administrador") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = textStyleDark,
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = carrera,
                        onValueChange = { carrera = it },
                        label = { Text("Carrera") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = textStyleDark,
                        colors = textFieldColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN SERVICIO DE TRANSPORTE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Aportar Servicio", fontWeight = FontWeight.Bold, color = NavyBlue)
                            Text("Quiero ser conductor", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = esConductor,
                            onCheckedChange = { esConductor = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
                        )
                    }

                    if (esConductor) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text("Datos del Vehículo", fontWeight = FontWeight.SemiBold, color = NavyBlue, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = placas,
                            onValueChange = { placas = it.uppercase() },
                            label = { Text("Número de Placas") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = textStyleDark,
                            colors = textFieldColors
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = modeloVehiculo,
                            onValueChange = { modeloVehiculo = it },
                            label = { Text("Modelo (Ej: Nissan Versa 2020)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = textStyleDark,
                            colors = textFieldColors
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // BOTONES PARA FOTOS (Corrección de colores aquí)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { /* Cámara */ },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Foto Auto", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            Button(
                                onClick = { /* Cámara */ },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Licencia", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}