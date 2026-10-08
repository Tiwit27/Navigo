package com.example.nawigacja_wewnatrz_budynku.api

import com.example.nawigacja_wewnatrz_budynku.api.dataClasses.ApiResponse
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Account
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Beacon
import com.example.nawigacja_wewnatrz_budynku.dataClasses.adminChangesDataClasses.BeaconChangeData
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Connection
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Floor
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Institution
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Room
import com.example.nawigacja_wewnatrz_budynku.dataClasses.adminChangesDataClasses.RoomChangeData
import com.example.nawigacja_wewnatrz_budynku.otherScripts.SecurePrefs
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    //auths
    @FormUrlEncoded
    @POST("auths/register")
    suspend fun register(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("secretKey") secretKey : String
    ): ApiResponse<Account>

    @FormUrlEncoded
    @POST("auths/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("secretKey") secretKey : String
    ): ApiResponse<Account>

    @GET("auths/auto-login")
    suspend fun autoLogin(
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Account>


    //beacons
    @GET("beacons/get-beacons/{id}")
    suspend fun getBeacons(
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Array<Beacon>>

    @PATCH("beacons/change-beacon-data/{id}")
    suspend fun changeBeaconData(
        @Path("id") beaconId: Int,
        @Body beaconData: BeaconChangeData,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ) : ApiResponse<String>

    @DELETE("beacons/delete-beacon/{id}")
    suspend fun deleteBeacon(
        @Path("id") beaconId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ) : ApiResponse<String>

    @FormUrlEncoded
    @PATCH("beacons/update-beacon-battery-level/{mac}")
    suspend fun updateBeaconBatteryLevel(
        @Path("mac") beaconMac: String,
        @Field("batteryLevel") batteryLevel : Double,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ) : ApiResponse<String>


    //connections
    @GET("connections/get-connections/{id}")
    suspend fun getConnections(
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Array<Connection>>

    @PATCH("connections/change-connection-data/{id}")
    suspend fun changeConnectionData(
        @Path("id") connectionId: Int,
        @Body connectionData: Connection,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Int>

    @DELETE("connections/delete-connection/{id}")
    suspend fun deleteConnection(
        @Path("id") connectionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Int>


    //floors
    @GET("floors/get-floors/{id}")
    suspend fun getFloors(
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Array<Floor>>


    //institutions
    @GET("institutions/get-institution-name/{id}")
    suspend fun getInstitutionName(
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Institution>

    @GET("institutions/get-institution-logo/{id}")
    suspend fun getInstitutionLogo(
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): retrofit2.Response<ResponseBody>

    @GET("institutions/get-institution-id/{mac}")
    suspend fun getInstitutionId(
        @Path("mac") mac: String,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Int>

    @Multipart
    @PATCH("institutions/change-institution-data/{id}")
    suspend fun changeInstitutionData(
        @Part logo: MultipartBody.Part ?= null,
        @Part("name") name : RequestBody,
        @Part("short_name") shortName : RequestBody,
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<String>


    //rooms
    @GET("rooms/get-rooms/{id}")
    suspend fun getRooms(
        @Path("id") institutionId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Array<Room>>

    @PATCH("rooms/change-room-data/{id}")
    suspend fun changeRoomData(
        @Path("id") roomId: Int,
        @Body roomData: RoomChangeData,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Int>

    @DELETE("rooms/delete-room/{id}")
    suspend fun deleteRoom(
        @Path("id") roomId: Int,
        @Header("authorization") token : String ?= SecurePrefs.sharedPreferences.getString("auth_token", null)
    ): ApiResponse<Int>
}