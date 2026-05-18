package com.example.unrait.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider

val NavyBlue = Color(0xFF1B2A47)
val OrangePrimary = Color(0xFFE66A25)

val DiagonalShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(0f, size.height)
    close()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateToHome: () -> Unit, onNavigateToRegistro: () -> Unit) {
    var numControl by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // ENCABEZADO DIAGONAL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(DiagonalShape)
                .background(NavyBlue),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("UNRAIT", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // FORMULARIO
        Column(modifier = Modifier.padding(horizontal = 32.dp)) {
            Text("Bienvenido de vuelta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
            Text("Ingresa para continuar", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = numControl,
                onValueChange = { numControl = it },
                label = { Text("Número de Control") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = NavyBlue) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary,
                    cursorColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = NavyBlue) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary,
                    cursorColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "¿Olvidaste tu contraseña?",
                color = OrangePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("INICIAR SESIÓN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÓN DE MICROSOFT
            OutlinedButton(
                onClick = { signInWithMicrosoft(context as Activity, onNavigateToHome) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyBlue)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccountCircle, // Podrías usar un logo de Microsoft aquí
                        contentDescription = "Microsoft Logo",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continuar con Microsoft", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("¿No tienes cuenta? ", color = Color.Gray)
                Text(
                    "Regístrate",
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegistro() }
                )
            }
        }
    }
}

fun signInWithMicrosoft(activity: Activity, onSuccess: () -> Unit) {
    val provider = OAuthProvider.newBuilder("microsoft.com")
    // provider.addCustomParameter("tenant", "common") // Opcional

    val firebaseAuth = FirebaseAuth.getInstance()

    firebaseAuth
        .startActivityForSignInWithProvider(activity, provider.build())
        .addOnSuccessListener { authResult ->
            // Usuario logueado con éxito
            onSuccess()
        }
        .addOnFailureListener { e ->
            // Manejar error (puedes mostrar un Toast)
            e.printStackTrace()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(onNavigateToHome: () -> Unit, onNavigateToLogin: () -> Unit) {
    var numControl by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // --- NUEVAS VARIABLES PARA CONDUCTORES ---
    var esConductor by remember { mutableStateOf(false) }
    var placas by remember { mutableStateOf("") }
    var modeloVehiculo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()) // Permite deslizar si el form crece
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(DiagonalShape)
                .background(OrangePrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("Únete a UNRAIT", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 32.dp)) {
            OutlinedTextField(
                value = numControl,
                onValueChange = { numControl = it },
                label = { Text("Número de Control") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = OrangePrimary) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = OrangePrimary) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = OrangePrimary) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN: QUIERO SER CONDUCTOR ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Deseo ser conductor", fontWeight = FontWeight.Bold, color = NavyBlue)
                            Text("Ayuda a tus compañeros", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = esConductor,
                            onCheckedChange = { esConductor = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
                        )
                    }

                    // CAMPOS EXTRA SI ES CONDUCTOR
                    if (esConductor) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        OutlinedTextField(
                            value = placas,
                            onValueChange = { placas = it.uppercase() },
                            label = { Text("Número de Placas") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = modeloVehiculo,
                            onValueChange = { modeloVehiculo = it },
                            label = { Text("Modelo (Ej: Versa 2020)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { /* Lógica futura de cámara */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Subir Fotos (Licencia / Auto)")
                        }
                    }
                }
            }
            // -------------------------------------

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("CREAR CUENTA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Espacio extra abajo para que el scroll termine bien
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}