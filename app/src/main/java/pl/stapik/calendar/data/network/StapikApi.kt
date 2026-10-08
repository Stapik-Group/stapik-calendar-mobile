package pl.stapik.calendar.data.network

import pl.stapik.calendar.data.model.DocumentResponse
import pl.stapik.calendar.data.model.DocumentWriteRequest
import pl.stapik.calendar.data.model.MeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface StapikApi {
    @GET("documents/{slotKey}")
    suspend fun getDocument(
        @Path("slotKey") slotKey: String,
        @Header("x-api-key") apiKey: String
    ): DocumentResponse

    @PUT("documents/{slotKey}")
    suspend fun writeDocument(
        @Path("slotKey") slotKey: String,
        @Header("x-api-key") apiKey: String,
        @Body body: DocumentWriteRequest
    ): DocumentResponse

    @GET("me")
    suspend fun getMe(
        @Header("x-api-key") apiKey: String
    ): MeResponse
}
