package com.example.unrait.ui.screens.drawer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.unrait.network.PerfilUpdateRequest
import com.example.unrait.network.UnraitApi
import com.example.unrait.usuarioEsConductorGlobal
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

fun bitmapToBase64(bitmap: Bitmap?): String {
    if (bitmap == null) return ""
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
    val b = baos.toByteArray()
    return Base64.encodeToString(b, Base64.DEFAULT)
}

fun base64ToBitmap(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrEmpty()) return null
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val nombreReal = currentUser?.displayName ?: "Usuario UNRAIT"

    var nombre by remember { mutableStateOf(nombreReal) }
    var telefono by remember { mutableStateOf("") }
    var carrera by remember { mutableStateOf("Ingeniería en Sistemas Computacionales") }
    val numControl = "223110210"

    // --- NUEVO: ESTADOS DE LOCALIDAD ---
    var localidadSeleccionada by remember { mutableStateOf("Ciudad Constitución") }
    var expandido by remember { mutableStateOf(false) }
    val opcionesLocalidad = listOf("Ciudad Constitución", "Ciudad Insurgentes", "Puerto San Carlos", "Villa Morelos", "Santo Domingo", "La Purísima", "San Isidro")

    var esConductor by remember { mutableStateOf(false) }
    var placas by remember { mutableStateOf("") }
    var modeloVehiculo by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var fotoAutoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fotoLicenciaBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fotoPerfilBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tipoFotoActual by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val textStyleDark = TextStyle(color = NavyBlue, fontSize = 16.sp)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcherAuto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { fotoAutoBitmap = bitmap; Toast.makeText(context, "Foto del auto capturada", Toast.LENGTH_SHORT).show() }
    }

    val launcherLicencia = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { fotoLicenciaBitmap = bitmap; Toast.makeText(context, "Foto de licencia capturada", Toast.LENGTH_SHORT).show() }
    }

    val launcherPerfil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { fotoPerfilBitmap = bitmap; Toast.makeText(context, "Foto de perfil capturada", Toast.LENGTH_SHORT).show() }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            scope.launch {
                delay(300)
                if (tipoFotoActual == "auto") launcherAuto.launch(null)
                else if (tipoFotoActual == "licencia") launcherLicencia.launch(null)
                else if (tipoFotoActual == "perfil") launcherPerfil.launch(null)
            }
        } else { Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show() }
    }

    fun manejarCargaDeFoto(tipo: String) {
        tipoFotoActual = tipo
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (tipo == "auto") launcherAuto.launch(null)
            else if (tipo == "licencia") launcherLicencia.launch(null)
            else if (tipo == "perfil") launcherPerfil.launch(null)
        } else { permisoCamaraLauncher.launch(Manifest.permission.CAMERA) }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val res = UnraitApi.retrofitService.getPerfil(currentUser.uid)
                if (res.success && res.perfil != null) {
                    telefono = res.perfil.telefono ?: ""
                    esConductor = res.perfil.es_conductor == 1
                    placas = res.perfil.placas ?: ""
                    modeloVehiculo = res.perfil.modelo_vehiculo ?: ""
                    localidadSeleccionada = res.perfil.localidad ?: "Ciudad Constitución"
                    fotoAutoBitmap = base64ToBitmap(res.perfil.foto_auto)
                    fotoLicenciaBitmap = base64ToBitmap(res.perfil.foto_licencia)
                    fotoPerfilBitmap = base64ToBitmap(res.perfil.foto_perfil)
                    usuarioEsConductorGlobal.value = esConductor
                }
            } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Color.Gray, focusedLabelColor = OrangePrimary, unfocusedLabelColor = Color.DarkGray, cursorColor = OrangePrimary)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    TextButton(onClick = {
                        if (currentUser != null) {
                            if (telefono.isEmpty()) { Toast.makeText(context, "Por favor ingresa tu número de WhatsApp", Toast.LENGTH_SHORT).show(); return@TextButton }
                            scope.launch {
                                try {
                                    val req = PerfilUpdateRequest(currentUser.uid, telefono, esConductor, placas, modeloVehiculo, bitmapToBase64(fotoAutoBitmap), bitmapToBase64(fotoLicenciaBitmap), bitmapToBase64(fotoPerfilBitmap), localidadSeleccionada)
                                    val res = UnraitApi.retrofitService.actualizarPerfil(req)
                                    if (res.success) {
                                        usuarioEsConductorGlobal.value = esConductor
                                        Toast.makeText(context, "Perfil y documentos actualizados", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                } catch (e: Exception) { Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }) { Text("GUARDAR", color = OrangePrimary, fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = OrangePrimary) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White).border(2.dp, OrangePrimary, CircleShape).clickable { manejarCargaDeFoto("perfil") }, contentAlignment = Alignment.Center) {
                        if (fotoPerfilBitmap != null) {
                            Image(bitmap = fotoPerfilBitmap!!.asImageBitmap(), contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Filled.Person, null, modifier = Modifier.size(80.dp), tint = NavyBlue)
                        }
                    }
                    FloatingActionButton(onClick = { manejarCargaDeFoto("perfil") }, modifier = Modifier.size(40.dp), containerColor = OrangePrimary, contentColor = Color.White, shape = CircleShape) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Información Personal", fontWeight = FontWeight.Bold, color = NavyBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = numControl, onValueChange = {}, label = { Text("Número de Control") }, modifier = Modifier.fillMaxWidth(), readOnly = true, textStyle = textStyleDark, colors = textFieldColors, leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Color.Gray) }, supportingText = { Text("Para cambios contacte al administrador") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono (WhatsApp)") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors, leadingIcon = { Icon(Icons.Filled.Phone, null, tint = Color.Gray) })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = carrera, onValueChange = { carrera = it }, label = { Text("Carrera") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                        Spacer(modifier = Modifier.height(8.dp))

                        // --- MENÚ DESPLEGABLE DE LOCALIDAD ---
                        ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
                            OutlinedTextField(
                                value = localidadSeleccionada,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Localidad Principal") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                                leadingIcon = { Icon(Icons.Filled.Place, null, tint = OrangePrimary) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                textStyle = textStyleDark,
                                colors = textFieldColors
                            )
                            ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }, modifier = Modifier.background(Color.White)) {
                                opcionesLocalidad.forEach { seleccion ->
                                    DropdownMenuItem(text = { Text(seleccion, color = NavyBlue) }, onClick = { localidadSeleccionada = seleccion; expandido = false })
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column { Text("Aportar Servicio", fontWeight = FontWeight.Bold, color = NavyBlue); Text("Quiero ser conductor", fontSize = 12.sp, color = Color.Gray) }
                            Switch(checked = esConductor, onCheckedChange = { esConductor = it }, colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary))
                        }

                        if (esConductor) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            Text("Datos del Vehículo", fontWeight = FontWeight.SemiBold, color = NavyBlue, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = placas, onValueChange = { placas = it.uppercase() }, label = { Text("Número de Placas") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = modeloVehiculo, onValueChange = { modeloVehiculo = it }, label = { Text("Modelo (Ej: Nissan Versa 2020)") }, modifier = Modifier.fillMaxWidth(), textStyle = textStyleDark, colors = textFieldColors)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { manejarCargaDeFoto("auto") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (fotoAutoBitmap != null) Color(0xFF4CAF50) else NavyBlue), shape = RoundedCornerShape(8.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(if (fotoAutoBitmap != null) Icons.Filled.CheckCircle else Icons.Filled.DirectionsCar, null, tint = Color.White); Spacer(modifier = Modifier.height(4.dp)); Text(if (fotoAutoBitmap != null) "Auto Listo" else "Foto Auto", fontSize = 12.sp, color = Color.White) }
                                }
                                Button(onClick = { manejarCargaDeFoto("licencia") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (fotoLicenciaBitmap != null) Color(0xFF4CAF50) else NavyBlue), shape = RoundedCornerShape(8.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(if (fotoLicenciaBitmap != null) Icons.Filled.CheckCircle else Icons.Filled.Badge, null, tint = Color.White); Spacer(modifier = Modifier.height(4.dp)); Text(if (fotoLicenciaBitmap != null) "Licencia Lista" else "Licencia", fontSize = 12.sp, color = Color.White) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}