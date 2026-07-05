package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// --- Custom Backend Models ---
@JsonClass(generateAdapter = true)
data class PaystackInitializeRequest(
    val email: String,
    val amount: Double,
    val studentId: Int
)

@JsonClass(generateAdapter = true)
data class PaystackInitializeResponse(
    val success: Boolean,
    val authorization_url: String?,
    val reference: String?,
    val access_code: String?,
    val error: String?
)

@JsonClass(generateAdapter = true)
data class PaystackVerifyResponse(
    val success: Boolean,
    val status: String?, // "success", "failed", "abandoned", etc.
    val reference: String?,
    val amount: Double?,
    val studentId: Int?,
    val error: String?
)

// --- Direct Paystack API Models ---
@JsonClass(generateAdapter = true)
data class DirectPaystackInitRequest(
    val email: String,
    val amount: Int, // in pesewas
    val currency: String = "GHS"
)

@JsonClass(generateAdapter = true)
data class DirectPaystackInitData(
    val authorization_url: String?,
    val access_code: String?,
    val reference: String?
)

@JsonClass(generateAdapter = true)
data class DirectPaystackInitResponse(
    val status: Boolean,
    val message: String?,
    val data: DirectPaystackInitData?
)

@JsonClass(generateAdapter = true)
data class DirectPaystackVerifyData(
    val status: String?, // "success", "failed", etc.
    val reference: String?,
    val amount: Int?,
    val gateway_response: String?
)

@JsonClass(generateAdapter = true)
data class DirectPaystackVerifyResponse(
    val status: Boolean,
    val message: String?,
    val data: DirectPaystackVerifyData?
)

@JsonClass(generateAdapter = true)
data class BackendVerifyRequest(
    val paymentReference: String,
    val userId: String
)

@JsonClass(generateAdapter = true)
data class BackendVerifyResponse(
    val success: Boolean,
    val premium: Boolean,
    val message: String? = null,
    val error: String? = null
)

interface PaystackApi {
    // Custom Backend Endpoints
    @POST("payments/initialize")
    suspend fun initializeTransaction(
        @Body request: PaystackInitializeRequest
    ): PaystackInitializeResponse

    @GET("payments/verify/{reference}")
    suspend fun verifyTransaction(
        @Path("reference") reference: String
    ): PaystackVerifyResponse

    @POST("payments/verify")
    suspend fun verifyTransactionWithBackend(
        @Body request: BackendVerifyRequest
    ): BackendVerifyResponse

    @GET("premium/status")
    suspend fun checkPremiumStatusWithBackend(
        @Header("x-user-id") userId: String
    ): BackendVerifyResponse

    // Direct Paystack API Endpoints
    @POST("transaction/initialize")
    suspend fun directInitializeTransaction(
        @Header("Authorization") authHeader: String,
        @Body request: DirectPaystackInitRequest
    ): DirectPaystackInitResponse

    @GET("transaction/verify/{reference}")
    suspend fun directVerifyTransaction(
        @Header("Authorization") authHeader: String,
        @Path("reference") reference: String
    ): DirectPaystackVerifyResponse

    companion object {
        fun create(baseUrl: String): PaystackApi {
            // Add trailing slash if missing
            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(PaystackApi::class.java)
        }
    }
}
