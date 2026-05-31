package com.example.unrait.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class RegistroRequest(
    val firebase_uid: String,
    val num_control: String,
    val nombre: String,
    val telefono: String,
    val es_conductor: Boolean,
    val placas: String?,
    val modelo_vehiculo: String?,
    val localidad: String
)
data class LoginRequest(val firebase_uid: String)
data class PublicarViajeRequest(val firebase_uid: String, val origen: String, val destino: String, val punto_encuentro: String, val hora_salida: String, val cupos_disponibles: Int)
data class AuthResponse(val success: Boolean, val message: String, val token: String? = null)
data class Viaje(val id_viaje: Int, val origen: String, val destino: String, val punto_encuentro: String, val hora_salida: String, val cupos_disponibles: Int, val nombre_conductor: String, val modelo_vehiculo: String, val placas: String, val telefono_conductor: String?)
data class ViajesResponse(val success: Boolean, val viajes: List<Viaje>)

data class PerfilUpdateRequest(val firebase_uid: String, val telefono: String, val es_conductor: Boolean, val placas: String?, val modelo_vehiculo: String?, val foto_auto: String?, val foto_licencia: String?, val foto_perfil: String?, val localidad: String?)
data class PerfilUsuario(val telefono: String?, val es_conductor: Int, val placas: String?, val modelo_vehiculo: String?, val foto_auto: String?, val foto_licencia: String?, val foto_perfil: String?, val localidad: String?)
data class PerfilResponse(val success: Boolean, val perfil: PerfilUsuario?)

data class PeticionPasajeroRequest(val firebase_uid: String, val origen: String, val destino: String, val punto_encuentro: String)
data class PeticionPasajero(val id_peticion: Int, val origen: String, val destino: String, val punto_encuentro: String, val nombre_pasajero: String, val telefono_pasajero: String?)
data class PeticionesActivasResponse(val success: Boolean, val peticiones: List<PeticionPasajero>)
data class SolicitarViajeRequest(val firebase_uid: String, val id_viaje: Int)
data class SolicitudPendiente(val id_solicitud: Int, val pasajero: String, val destino: String, val telefono_pasajero: String?)
data class SolicitudesResponse(val success: Boolean, val solicitudes: List<SolicitudPendiente>)
data class AceptarSolicitudRequest(val id_solicitud: Int)
data class EstadoPasajeroResponse(val success: Boolean, val estado: String, val conductor: String?, val telefono_conductor: String?, val foto_auto: String?)
data class AceptarPeticionMapaRequest(val id_peticion: Int, val firebase_uid_conductor: String)
data class FinalizarViajeRequest(val firebase_uid: String, val donativo: String = "0")
data class HistorialItem(val tipo: String?, val destino: String?, val conductor: String?, val estado: String?, val donativo: String?)
data class HistorialResponse(val success: Boolean, val historial: List<HistorialItem>)

// --- DATAS CLASSES DE COMUNIDAD AUTOMATIZADA ---
data class LocalidadItem(val nombre: String, val cantidad: Int)
data class LocalidadesResponse(val success: Boolean, val localidades: List<LocalidadItem>)

data class LugarFrecuenteItem(val nombre: String, val cantidad: Int)
data class LugaresResponse(val success: Boolean, val lugares: List<LugarFrecuenteItem>)

data class ConfianzaItem(val nombre: String, val telefono: String?, val foto_perfil: String?, val viajes_juntos: Int, val rol: String)
data class ConfianzaResponse(val success: Boolean, val confianza: List<ConfianzaItem>)

interface UnraitApiService {
    @POST("/api/auth/registro") suspend fun registrarUsuario(@Body request: RegistroRequest): AuthResponse
    @POST("/api/auth/login") suspend fun loginUsuario(@Body request: LoginRequest): AuthResponse
    @POST("/api/viajes/publicar") suspend fun publicarViaje(@Body request: PublicarViajeRequest): AuthResponse
    @GET("/api/viajes/disponibles") suspend fun getViajesDisponibles(): ViajesResponse
    @POST("/api/viajes/solicitar") suspend fun solicitarViaje(@Body request: SolicitarViajeRequest): AuthResponse
    @GET("/api/viajes/mis-solicitudes/{uid}") suspend fun getMisSolicitudes(@Path("uid") uid: String): SolicitudesResponse
    @PUT("/api/viajes/actualizar-perfil") suspend fun actualizarPerfil(@Body request: PerfilUpdateRequest): AuthResponse
    @GET("/api/viajes/perfil/{uid}") suspend fun getPerfil(@Path("uid") uid: String): PerfilResponse
    @POST("/api/viajes/solicitar-conductor") suspend fun solicitarConductor(@Body request: PeticionPasajeroRequest): AuthResponse
    @GET("/api/viajes/peticiones-activas") suspend fun getPeticionesActivas(): PeticionesActivasResponse
    @PUT("/api/viajes/aceptar-solicitud") suspend fun aceptarSolicitud(@Body request: AceptarSolicitudRequest): AuthResponse
    @GET("/api/viajes/estado-pasajero/{uid}") suspend fun getEstadoPasajero(@Path("uid") uid: String): EstadoPasajeroResponse
    @PUT("/api/viajes/aceptar-peticion-mapa") suspend fun aceptarPeticionMapa(@Body request: AceptarPeticionMapaRequest): AuthResponse
    @PUT("/api/viajes/finalizar-viaje") suspend fun finalizarViajeConductor(@Body request: FinalizarViajeRequest): AuthResponse
    @PUT("/api/viajes/pasajero-finaliza") suspend fun finalizarViajePasajero(@Body request: FinalizarViajeRequest): AuthResponse
    @GET("/api/viajes/historial/{uid}") suspend fun getHistorial(@Path("uid") uid: String): HistorialResponse

    // --- RUTAS DE COMUNIDAD ---
    @GET("/api/viajes/comunidad/localidades") suspend fun getLocalidades(): LocalidadesResponse
    @GET("/api/viajes/comunidad/frecuentes/{uid}") suspend fun getLugaresFrecuentes(@Path("uid") uid: String): LugaresResponse
    @GET("/api/viajes/comunidad/confianza/{uid}") suspend fun getConfianza(@Path("uid") uid: String): ConfianzaResponse
}

object UnraitApi {
    private const val BASE_URL = "http://192.168.1.154:3000"
    private val retrofit = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    val retrofitService: UnraitApiService by lazy { retrofit.create(UnraitApiService::class.java) }
}