package video.peach.android

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import video.peach.android.data.*
import okhttp3.tls.HeldCertificate
import okhttp3.tls.HandshakeCertificates

class PeachClientTest {
    private class Memory : SessionStorage {
        val values = mutableMapOf<String, String>()
        override fun read(name: String) = values[name].orEmpty()
        override fun write(name: String, value: String) { values[name] = value }
    }
    @Test fun errorSemanticsAndFieldValidation() {
        for (status in listOf(401, 403, 409, 429, 503)) {
            val failure = runCatching { PeachClient.decodeText(status, "{}") }.exceptionOrNull() as PeachError
            assertEquals(status, failure.status)
            assertTrue(failure.message!!.isNotBlank())
        }
        val field = runCatching { PeachClient.decodeText(400, """{"detail":"参数无效","errors":{"name":"不能为空"}}""") }.exceptionOrNull() as PeachError
        assertEquals("不能为空", field.fields!!.getString("name"))
        assertFalse(PeachClient.decodeText(200, "{}").has("missing_optional_field"))
    }
    @Test fun login303AndAuthenticatedMutation() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(303).addHeader("Location", "/").addHeader("Set-Cookie", "peach=session; Path=/; HttpOnly"))
            server.enqueue(MockResponse().setBody("{\"libraries\":[]}"))
            server.enqueue(MockResponse().setBody("{\"ok\":true}"))
            server.start()
            val client = PeachClient(ServerProfile("a", "测试", server.url("/").toString()), Memory())
            client.login("secret")
            client.post("/api/watch-later", JSONObject().put("id", 1))
            val login = server.takeRequest()
            assertEquals("/login", login.path)
            assertTrue(login.body.readUtf8().contains("token=secret"))
            val read = server.takeRequest()
            assertEquals("peach=session", read.getHeader("Cookie"))
            val mutation = server.takeRequest()
            assertEquals(server.url("/").toString().trimEnd('/'), mutation.getHeader("Origin"))
            assertEquals(1, JSONObject(mutation.body.readUtf8()).getInt("id"))
        }
    }
    @Test fun cookiesNeverCrossServerPorts() = runBlocking {
        MockWebServer().use { first -> MockWebServer().use { second ->
            first.start(); second.start()
            first.enqueue(MockResponse().addHeader("Set-Cookie", "peach=private; Path=/").setBody("{}"))
            first.enqueue(MockResponse().setResponseCode(302).addHeader("Location", second.url("/outside")))
            second.enqueue(MockResponse().setBody("{}"))
            val client = PeachClient(ServerProfile("a", "测试", first.url("/").toString()), Memory())
            client.get("/api/libraries"); client.get("/api/items")
            assertNull(second.takeRequest().getHeader("Cookie"))
        } }
    }
    @Test fun writesAreNotRetriedOnConflict() = runBlocking {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setResponseCode(409).setBody("{\"detail\":\"版本冲突\"}"))
            val client = PeachClient(ServerProfile("a", "测试", server.url("/").toString()), Memory())
            val failure = runCatching { client.post("/api/activity", JSONObject().put("delta", 10)) }.exceptionOrNull()
            assertTrue(failure is PeachError)
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun businessErrorInSuccessfulHttpResponseIsStillAFailedWrite() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"error":"任务失败"}"""))
            server.start()
            val client = PeachClient(ServerProfile("error", "测试", server.url("/").toString()), Memory())
            val failure = runCatching { client.post("/api/activity", JSONObject().put("id", 1)) }.exceptionOrNull()
            assertTrue(failure is PeachError)
            assertEquals("任务失败", failure!!.message)
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun rejectsCredentialsAndUntrustedResourceOrigins() {
        assertTrue(runCatching { PeachClient.normalizeAddress("https://user:password@example.com") }.isFailure)
        val client = PeachClient(ServerProfile("a", "测试", "https://example.com/"), Memory())
        assertTrue(runCatching { client.resolve("https://another.example/video") }.isFailure)
    }
    @Test fun privateCaDoesNotDisableHostnameValidation() = runBlocking {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        MockWebServer().use { server ->
            server.useHttps(serverTls.sslSocketFactory(), false)
            server.start()
            server.enqueue(MockResponse().setBody("{\"ok\":true}"))
            val trusted = PeachClient(ServerProfile("a", "测试", server.url("/").newBuilder().host("localhost").build().toString(), certificate.certificatePem()), Memory())
            assertTrue(trusted.get("/healthz").getBoolean("ok"))
            val mismatch = PeachClient(ServerProfile("b", "测试", server.url("/").newBuilder().host("127.0.0.1").build().toString(), certificate.certificatePem()), Memory())
            assertTrue(runCatching { mismatch.get("/healthz") }.isFailure)
            val unknown = PeachClient(ServerProfile("c", "测试", server.url("/").toString()), Memory())
            assertTrue(runCatching { unknown.get("/healthz") }.isFailure)
        }
    }
}
