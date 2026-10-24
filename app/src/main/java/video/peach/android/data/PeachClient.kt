package video.peach.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Url
import java.io.IOException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class PeachError(val status: Int, message: String, val fields: JSONObject? = null) : IOException(message)

interface PeachTransport {
    @GET suspend fun get(@Url url: String): retrofit2.Response<ResponseBody>
    @POST suspend fun post(@Url url: String, @Body body: RequestBody): retrofit2.Response<ResponseBody>
}

class SessionCookies(private val id: String, private val base: HttpUrl, private val secrets: SessionStorage) : CookieJar {
    private var cookies = mutableListOf<Cookie>()
    private var restored = false
    @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (url.scheme != base.scheme || url.host != base.host || url.port != base.port) return emptyList()
        if (!restored) {
            val saved = runCatching { JSONArray(secrets.read("cookies:$id")) }.getOrElse { JSONArray() }
            cookies = (0 until saved.length()).mapNotNull { Cookie.parse(url, saved.getString(it)) }.toMutableList()
            restored = true
        }
        return cookies.filter { it.matches(url) && it.expiresAt > System.currentTimeMillis() }
    }
    @Synchronized override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (url.scheme != base.scheme || url.host != base.host || url.port != base.port) return
        loadForRequest(url)
        for (cookie in cookies) {
            this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > System.currentTimeMillis()) this.cookies.add(cookie)
        }
        secrets.write("cookies:$id", JSONArray(this.cookies.map { it.toString() }).toString())
    }
    @Synchronized fun clear() { cookies.clear(); restored = true; secrets.write("cookies:$id", "") }
}

class PeachClient(val profile: ServerProfile, secrets: SessionStorage) {
    val base: HttpUrl = normalizeAddress(profile.url).toHttpUrl()
    private val cookies = SessionCookies(profile.id, base, secrets)
    val http: OkHttpClient
    val readHttp: OkHttpClient
    private val transport: PeachTransport
    private val reads: PeachTransport
    init {
        val builder = OkHttpClient.Builder().cookieJar(cookies).retryOnConnectionFailure(false)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                // Cookie、Origin 仅用于当前服务器，媒体重定向不能携带服务器凭据。
                val same = request.url.host == base.host && request.url.port == base.port && request.url.scheme == base.scheme
                val adjusted = request.newBuilder()
                if (request.header("Accept") == null && request.header("Range") == null && !request.url.encodedPath.contains("stream")) {
                    adjusted.header("Accept", "application/json")
                }
                if (same && request.method !in listOf("GET", "HEAD")) adjusted.header("Origin", origin(base))
                chain.proceed(adjusted.build())
            }
        if (profile.caPem.isNotBlank()) {
            val certificates = CertificateFactory.getInstance("X.509").generateCertificates(profile.caPem.byteInputStream())
            require(certificates.isNotEmpty()) { "证书文件不包含有效的 X.509 CA" }
            val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
            certificates.forEachIndexed { index, certificate -> store.setCertificateEntry("server-ca-$index", certificate) }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(store) }
            val manager = factory.trustManagers.filterIsInstance<X509TrustManager>().single()
            val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(manager), null) }
            builder.sslSocketFactory(ssl.socketFactory, manager)
        }
        http = builder.build()
        readHttp = http.newBuilder().connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES)).retryOnConnectionFailure(true).build()
        transport = Retrofit.Builder().baseUrl(base).client(http).build().create(PeachTransport::class.java)
        reads = Retrofit.Builder().baseUrl(base).client(readHttp).build().create(PeachTransport::class.java)
    }
    fun resolve(path: String): String {
        val resolved = base.resolve(path) ?: throw IllegalArgumentException("无效的资源地址")
        require(resolved.scheme == base.scheme && resolved.host == base.host && resolved.port == base.port) { "服务器返回了不受信任的资源地址" }
        return resolved.toString()
    }
    suspend fun get(path: String, query: Map<String, String> = emptyMap()): JSONObject {
        val url = resolve(path).toHttpUrl().newBuilder().apply { query.forEach { (key, value) -> if (value.isNotBlank()) addQueryParameter(key, value) } }.build()
        return decode(reads.get(url.toString()))
    }
    suspend fun post(path: String, body: JSONObject = JSONObject()): JSONObject {
        val result = decode(transport.post(resolve(path), body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())))
        if (result.has("error") && !result.isNull("error") && result.optString("error").isNotBlank())
            throw PeachError(400, result.optString("error"), result.optJSONObject("errors"))
        return result
    }
    suspend fun login(password: String, days: Int = 30) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(resolve("/login")).post(FormBody.Builder().add("token", password).add("days", days.toString()).add("next", "/").build()).build()
        http.newBuilder().followRedirects(false).build().newCall(request).execute().use { response ->
            if (response.code != 303 && response.code != 200) throw PeachError(response.code, if (response.code == 401) "访问口令不正确" else "登录失败（${response.code}）")
        }
        get("/api/libraries")
    }
    suspend fun importHistory(name: String, bytes: ByteArray): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(resolve("/api/taste/import")).post(bytes.toRequestBody("application/octet-stream".toMediaType())).header("X-Peach-Filename", java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")).build()
        http.newCall(request).execute().use { response -> decodeText(response.code, response.body?.string().orEmpty()) }
    }
    fun logout() { cookies.clear() }
    private fun decode(response: retrofit2.Response<ResponseBody>): JSONObject = decodeText(response.code(), (response.body() ?: response.errorBody())?.string().orEmpty())
    companion object {
        fun normalizeAddress(raw: String): String {
            require(raw.trim().startsWith("http://") || raw.trim().startsWith("https://")) { "请输入以 http:// 或 https:// 开头的 Peach 地址" }
            val url = raw.trim().toHttpUrl()
            require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "请输入不含口令和查询参数的服务器地址" }
            require(url.encodedPath == "/") { "请使用 Peach 服务的根地址" }
            return url.toString()
        }
        fun origin(base: HttpUrl) = base.newBuilder().encodedPath("/").query(null).fragment(null).build().toString().trimEnd('/')
        fun decodeText(status: Int, text: String): JSONObject {
            val value = runCatching { JSONObject(text) }.getOrNull()
            if (status !in 200..299) {
                val reason = listOf("message", "detail", "error").mapNotNull { value?.opt(it) as? String }.firstOrNull { it.isNotBlank() }
                val fallback = when (status) {
                    401 -> "登录已过期，请重新连接服务器"
                    403 -> "服务器限制此操作，请在运行 Peach 的电脑上完成"
                    409 -> "数据已变化或服务器处于只读状态，请刷新后重试"
                    429 -> "请求较频繁，请稍后重试"
                    503 -> "媒体来源离线或服务暂不可用"
                    else -> "请求失败（$status）"
                }
                throw PeachError(status, reason?.let(MediaErrors::reason) ?: fallback, value?.optJSONObject("errors"))
            }
            return value ?: if (text.trim().startsWith("[")) JSONObject().put("items", JSONArray(text)) else throw PeachError(status, "服务器返回了非 JSON 内容，请检查地址与登录状态")
        }
    }
}
