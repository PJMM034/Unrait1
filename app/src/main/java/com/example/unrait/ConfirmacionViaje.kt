package com.example.unrait

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionViajeScreen() {
    val detalle = viajeDetalle.value ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Viaje", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { mostrarPantalla.value = "viajes" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = WhiteBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card del Conductor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TU CONDUCTOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = NavyBlue.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = NavyBlue
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mario Gomez",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyBlue
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                                Text(" 4.9 (120 viajes)", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                        
                        IconButton(
                            onClick = { /* Llamar */ },
                            modifier = Modifier.background(OrangePrimary, CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("VEHÍCULO", fontSize = 11.sp, color = Color.Gray)
                            Text("Toyota Corolla", fontWeight = FontWeight.Bold, color = NavyBlue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PLACA", fontSize = 11.sp, color = Color.Gray)
                            Text("5432-XYZ", fontWeight = FontWeight.Bold, color = NavyBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card de Ruta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("PUNTO DE ORIGEN", fontSize = 11.sp, color = Color.Gray)
                            Text(detalle.origen, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NavyBlue)
                        }
                    }
                    
                    Box(modifier = Modifier.padding(start = 9.dp).height(30.dp).width(2.dp).background(Color.LightGray))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("DESTINO FINAL", fontSize = 11.sp, color = Color.Gray)
                            Text(detalle.destino, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NavyBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card de Pago
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NavyBlue)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(detalle.iconoVehiculo, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(detalle.vehiculoNombre, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(detalle.precio, color = OrangePrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    mostrarPantalla.value = "main"
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("CANCELAR VIAJE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
