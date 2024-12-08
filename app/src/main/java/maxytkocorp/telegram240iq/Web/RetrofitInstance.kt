package maxytkocorp.telegram240iq.Web

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitInstance {
    private const val BASE_URL = "https://faerytea.name:8008/"

    private val moshi = Moshi.Builder().add(MessageDataAdapter()).add(KotlinJsonAdapterFactory())
        .build()

    private val trustAllCertificates: TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAllCertificates), java.security.SecureRandom())
    }

    private val client = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCertificates as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .client(client)
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
