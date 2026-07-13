package pl.stapik.calendar.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface StapikApi {
    @GET("read")
    suspend fun read(
        @Query("filename") filename: String,
        @Header("x-api-key") apiKey: String
    ): ResponseBody
}
