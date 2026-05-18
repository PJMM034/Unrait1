package com.example.unrait.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. El paquete de datos que vamos a enviar al servidor
data class RegistroRequest(
    val num_control: String,
    val nombre: String,
    val password: String,
    val es_conductor: Boolean,
    val placas: String?,
    val modelo_vehiculo: String?
)

// 2. La respuesta que esperamos recibir del servidor
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)

// 3. Las "puertas" a las que tocaremos en Node.js
interface UnraitApiService {
    @POST("/api/auth/registro")
    suspend fun registrarUsuario(@Body request: RegistroRequest): AuthResponse
}

// 4. El motor de conexión (¡Aquí va tu IP!)
object UnraitApi {
    private const val BASE_URL = "http://192.168.1.88:3000"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val retrofitService: UnraitApiService by lazy {
        retrofit.create(UnraitApiService::class.java)
    }
}