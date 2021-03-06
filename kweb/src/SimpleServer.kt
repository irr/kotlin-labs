import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.io.IOException
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.jvm.jvmName

internal interface EmbeddedAsyncServerCallback {
    fun completed(result: Any)
    fun failed(result: Any? = null)
}

internal object EmbeddedAsyncServer {
    val httpclient: CloseableHttpAsyncClient

    init {
        httpclient = HttpAsyncClients.createDefault()
        httpclient.start()
    }

    private fun capture(uri: String, callback: EmbeddedAsyncServerCallback) {
        val result = hashMapOf<String, String>()
        val request = HttpGet(uri)
        httpclient.execute(request, object : FutureCallback<HttpResponse> {
            override fun completed(httpResponse: HttpResponse) {
                result["ip"] = try {
                    IOUtils.toString(httpResponse.entity.content)
                } catch (e: IOException) {
                    return callback.failed(result)
                }
                callback.completed(result)
            }
            override fun failed(e: Exception) {
                callback.failed(e)
            }
            override fun cancelled() {
                callback.failed()
            }
        })
    }

    class EmbeddedAsyncServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val ctx = req.startAsync()
            ctx.start(object : Runnable {
                override fun run() {
                    val response = ctx.response as HttpServletResponse
                    response.status = HttpServletResponse.SC_OK
                    response.contentType = "application/json;charset=utf-8"
                    EmbeddedAsyncServer.capture("https://api.ipify.org", object : EmbeddedAsyncServerCallback {
                        override fun completed(result: Any) {
                            response.writer.println(Gson().toJson(hashMapOf("ipfy.org" to result)))
                            ctx.complete()
                        }
                        override fun failed(result: Any?) {
                            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                            response.writer.println(Gson().toJson(hashMapOf("error" to result)))
                            ctx.complete()
                        }

                    })
                }
            })
        }
    }
}

fun main(args: Array<String>) {
    BasicConfigurator.configure()
    Logger.getRootLogger().level = Level.INFO
    val threadPool = QueuedThreadPool(32, 16)
    val server = Server(threadPool)
    val context = ServletContextHandler()
    val connector = ServerConnector(server);
    connector.port = 8080
    server.connectors = arrayOf(connector)
    context.contextPath = "/"
    val asyncHolder = context.addServlet(EmbeddedAsyncServer.EmbeddedAsyncServlet::class.jvmName, "/")
    asyncHolder.isAsyncSupported = true
    server.handler = context
    server.start()
    server.join()
}

