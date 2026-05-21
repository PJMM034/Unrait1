package com.example.unrait.ui.screens.auth

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.unrait.network.UnraitApi
import com.example.unrait.network.LoginRequest
import com.example.unrait.network.RegistroRequest
import androidx.compose.ui.text.style.TextAlign

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
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegistro: (String, String, String) -> Unit // Recibe UID, Nombre y Control de Microsoft
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

        Spacer(modifier = Modifier.height(60.dp))

        // FORMULARIO DE ACCESO ÚNICO
        Column(modifier = Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bienvenido a UNRAIT", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
            Text("Ingresa con tu cuenta institucional", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(48.dp))

            // BOTÓN DE MICROSOFT CONECTADO
            OutlinedButton(
                onClick = {
                    signInWithMicrosoft(
                        activity = context as Activity,
                        scope = scope,
                        onSuccess = onNavigateToHome,
                        onNavigateToRegistro = onNavigateToRegistro
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyBlue)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Microsoft Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continuar con Microsoft", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Acceso seguro restringido para la comunidad del ITSCC",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun signInWithMicrosoft(
    activity: Activity,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onNavigateToRegistro: (String, String, String) -> Unit
) {
    val provider = OAuthProvider.newBuilder("microsoft.com")
    val firebaseAuth = FirebaseAuth.getInstance()

    firebaseAuth
        .startActivityForSignInWithProvider(activity, provider.build())
        .addOnSuccessListener { authResult ->
            val user = authResult.user
            if (user != null) {
                val firebaseUid = user.uid
                val email = user.email ?: ""
                val nombreCompleto = user.displayName ?: ""

                // 1. FILTRO DE SEGURIDAD INSTITUCIONAL EXACTO
                if (!email.endsWith("@cdconstitucion.tecnm.mx", ignoreCase = true)) {
                    Toast.makeText(activity, "Acceso denegado: Usa tu correo institucional del TecNM", Toast.LENGTH_LONG).show()
                    firebaseAuth.signOut()
                    return@addOnSuccessListener
                }

                // 2. EXTRACCIÓN DEL NÚMERO DE CONTROL (Elimina la L de la matrícula)
                val numControl = email.substringBefore("@").replace("L", "", ignoreCase = true)

                // 3. VERIFICACIÓN EN BASE DE DATOS MYSQL
                scope.launch {
                    try {
                        val response = UnraitApi.retrofitService.loginUsuario(LoginRequest(firebaseUid))
                        if (response.success) {
                            Toast.makeText(activity, "¡Bienvenido, $nombreCompleto!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 404) {
                            // Si da 404, el usuario está en Firebase pero no en MySQL -> Se va a Registro
                            Toast.makeText(activity, "Por favor completa tu perfil", Toast.LENGTH_SHORT).show()
                            onNavigateToRegistro(firebaseUid, nombreCompleto, numControl)
                        } else {
                            Toast.makeText(activity, "Error en el servidor local", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(activity, "Error de conexión con el backend", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(activity, "Autenticación cancelada", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    firebaseUid: String,
    nombreInicial: String,
    numControlInicial: String,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Inicializamos con los valores obtenidos de la API de Microsoft
    val numControl by remember { mutableStateOf(numControlInicial) }
    val nombre by remember { mutableStateOf(nombreInicial) }

    var esConductor by remember { mutableStateOf(false) }
    var placas by remember { mutableStateOf("") }
    var modeloVehiculo by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(DiagonalShape)
                .background(OrangePrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("Completa tu Perfil", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 32.dp)) {

            // Número de Control (Lectura Bloqueada por Seguridad)
            OutlinedTextField(
                value = numControl,
                onValueChange = {},
                label = { Text("Número de Control") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre Completo (Lectura Bloqueada por Seguridad)
            OutlinedTextField(
                value = nombre,
                onValueChange = {},
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN CONDUCTOR ---
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
                            Text("Comparte tu auto con el Tec", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = esConductor,
                            onCheckedChange = { esConductor = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
                        )
                    }

                    if (esConductor) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)

                        OutlinedTextField(
                            value = placas,
                            onValueChange = { placas = it.uppercase() },
                            label = { Text("Número de Placas") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = modeloVehiculo,
                            onValueChange = { modeloVehiculo = it },
                            label = { Text("Modelo (Ej: Nissan Versa 2022)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { /* Próximamente: Subir archivos a Firebase Storage */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Subir Fotos (Licencia / Auto)", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÓN GUARDAR EN MYSQL LOCAL VIA RETROFIT
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val request = RegistroRequest(
                                firebase_uid = firebaseUid,
                                num_control = numControl,
                                nombre = nombre,
                                es_conductor = esConductor,
                                placas = if (esConductor) placas else null,
                                modelo_vehiculo = if (esConductor) modeloVehiculo else null
                            )

                            val response = UnraitApi.retrofitService.registrarUsuario(request)

                            if (response.success) {
                                Toast.makeText(context, "¡Perfil de UNRAIT guardado!", Toast.LENGTH_SHORT).show()
                                onNavigateToHome()
                            } else {
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error guardando datos localmente", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("FINALIZAR REGISTRO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}