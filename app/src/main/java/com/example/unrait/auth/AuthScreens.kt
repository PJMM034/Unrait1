package com.example.unrait.ui.screens.auth

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.unrait.network.LoginRequest
import com.example.unrait.network.RegistroRequest
import com.example.unrait.network.UnraitApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onNavigateToRegistro: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
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

        Column(modifier = Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bienvenido a UNRAIT", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
            Text("Ingresa con tu cuenta institucional", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(48.dp))

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

                if (!email.endsWith("@cdconstitucion.tecnm.mx", ignoreCase = true)) {
                    Toast.makeText(activity, "Acceso denegado: Usa tu correo institucional del TecNM", Toast.LENGTH_LONG).show()
                    firebaseAuth.signOut()
                    return@addOnSuccessListener
                }

                val numControl = email.substringBefore("@").replace("L", "", ignoreCase = true)

                scope.launch {
                    try {
                        val response = UnraitApi.retrofitService.loginUsuario(LoginRequest(firebaseUid))
                        if (response.success) {
                            Toast.makeText(activity, "¡Bienvenido, $nombreCompleto!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 404) {
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
    val numControl by remember { mutableStateOf(numControlInicial) }
    val nombre by remember { mutableStateOf(nombreInicial) }
    var telefono by remember { mutableStateOf("") }

    // --- NUEVO: MENÚ DE LOCALIDAD ---
    var localidadSeleccionada by remember { mutableStateOf("Ciudad Constitución") }
    var expandido by remember { mutableStateOf(false) }
    val opcionesLocalidad = listOf("Ciudad Constitución", "Ciudad Insurgentes", "Puerto San Carlos", "Villa Morelos", "Santo Domingo", "La Purísima", "San Isidro")

    var esConductor by remember { mutableStateOf(false) }
    var placas by remember { mutableStateOf("") }
    var modeloVehiculo by remember { mutableStateOf("") }

    var fotoAutoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fotoLicenciaBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tipoFotoActual by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium)

    val launcherAuto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            fotoAutoBitmap = bitmap
            Toast.makeText(context, "Foto del auto capturada", Toast.LENGTH_SHORT).show()
        }
    }

    val launcherLicencia = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            fotoLicenciaBitmap = bitmap
            Toast.makeText(context, "Foto de licencia capturada", Toast.LENGTH_SHORT).show()
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            scope.launch {
                delay(300)
                if (tipoFotoActual == "auto") launcherAuto.launch(null)
                else if (tipoFotoActual == "licencia") launcherLicencia.launch(null)
            }
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    fun manejarCargaDeFoto(tipo: String) {
        tipoFotoActual = tipo
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (tipo == "auto") launcherAuto.launch(null) else launcherLicencia.launch(null)
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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

            OutlinedTextField(
                value = numControl,
                onValueChange = {},
                label = { Text("Número de Control") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                textStyle = textStyleDark,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = {},
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                textStyle = textStyleDark,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono (Para WhatsApp)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = textStyleDark,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = OrangePrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- MENÚ DESPLEGABLE DE LOCALIDAD EN REGISTRO ---
            ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
                OutlinedTextField(
                    value = localidadSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Localidad Principal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = OrangePrimary) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    textStyle = textStyleDark,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandido,
                    onDismissRequest = { expandido = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    opcionesLocalidad.forEach { seleccion ->
                        DropdownMenuItem(
                            text = { Text(seleccion, color = NavyBlue) },
                            onClick = {
                                localidadSeleccionada = seleccion
                                expandido = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            textStyle = textStyleDark,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = modeloVehiculo,
                            onValueChange = { modeloVehiculo = it },
                            label = { Text("Modelo (Ej: Nissan Versa 2022)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = textStyleDark,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { manejarCargaDeFoto("auto") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (fotoAutoBitmap != null) Color(0xFF4CAF50) else NavyBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(if (fotoAutoBitmap != null) Icons.Filled.CheckCircle else Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(if (fotoAutoBitmap != null) "Auto Listo" else "Foto Auto", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            Button(
                                onClick = { manejarCargaDeFoto("licencia") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (fotoLicenciaBitmap != null) Color(0xFF4CAF50) else NavyBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(if (fotoLicenciaBitmap != null) Icons.Filled.CheckCircle else Icons.Filled.Badge, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(if (fotoLicenciaBitmap != null) "Licencia Lista" else "Licencia", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (telefono.isEmpty()) {
                        Toast.makeText(context, "El número de WhatsApp es obligatorio", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            val request = RegistroRequest(
                                firebase_uid = firebaseUid,
                                num_control = numControl,
                                nombre = nombre,
                                telefono = telefono,
                                es_conductor = esConductor,
                                placas = if (esConductor) placas else null,
                                modelo_vehiculo = if (esConductor) modeloVehiculo else null,
                                localidad = localidadSeleccionada // <-- SE ENVÍA LA LOCALIDAD
                            )

                            val response = UnraitApi.retrofitService.registrarUsuario(request)

                            if (response.success) {
                                Toast.makeText(context, "¡Perfil de UNRAIT guardado!", Toast.LENGTH_SHORT).show()
                                onNavigateToHome()
                            } else {
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: retrofit2.HttpException) {
                            Toast.makeText(context, "Error del servidor: Revisa tus datos", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error de red: Verifica tu IP o Servidor", Toast.LENGTH_LONG).show()
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