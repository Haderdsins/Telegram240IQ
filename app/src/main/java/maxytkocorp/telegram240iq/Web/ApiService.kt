package maxytkocorp.telegram240iq.Web

import maxytkocorp.telegram240iq.Models.Message
import maxytkocorp.telegram240iq.Models.MessageRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("/addusr")
    suspend fun registerUser(@Field("name") username: String): Response<String>

    @POST("/login")
    suspend fun loginUser(@Body requestBody: Map<String, String>): Response<String>

    @GET("/inbox/{username}")
    suspend fun getInbox(
        @Path("username") username: String,
        @Header("X-Auth-Token") token: String,
        @Query("limit") limit: Int? = null,
        @Query("lastKnownId") lastKnownId: Int? = null,
        @Query("reverse") reverse: Boolean? = null,
    ): Response<List<Message>>

    @POST("/logout")
    suspend fun logoutUser(@Header("X-Auth-Token") token: String): Response<Unit>

    @GET("/channels")
    suspend fun getChannels(@Header("X-Auth-Token") token: String): Response<List<String>>

    @POST("/messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: MessageRequest,
    ): Response<Int>

    @Multipart
    @POST("/messages")
    suspend fun postMessage(
        @Header("X-Auth-Token") token: String,
        @Part("msg") textMessage: MessageRequest,
        @Part image: MultipartBody.Part,
    ): Response<Int>

    @GET("/channel/{name}")
    suspend fun getChannelMessages(
        @Path("name") name: String,
        @Header("X-Auth-Token") token: String,
        @Query("limit") limit: Int? = null,
        @Query("lastKnownId") lastKnownId: Int? = null,
        @Query("reverse") reverse: Boolean? = null,
    ): Response<List<Message>>
}
