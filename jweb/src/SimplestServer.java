import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

interface EmbeddedAsyncServerCallback {
    void complete(Object result);
}

class EmbeddedAsyncServer {
    static final CloseableHttpAsyncClient httpclient;

    static {
        httpclient = HttpAsyncClients.createDefault();
        httpclient.start();
    }

    private static void capture(final String uri, final EmbeddedAsyncServerCallback callback) {
        final HashMap<String, Object> result = new HashMap();
        final HttpGet request = new HttpGet(uri);
        httpclient.execute(request, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                result.put("status", httpResponse.getStatusLine().getStatusCode());
                try {
                    final String body = IOUtils.toString(httpResponse.getEntity().getContent());
                    result.put("body", body);
                    result.put("size", (body != null) ? body.length() : 0);
                    callback.complete(result);
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.complete(result);
                }
            }

            @Override
            public void failed(Exception e) {
                callback.complete(result);
            }

            @Override
            public void cancelled() {
                callback.complete(result);
            }
        });
    }

    public static class EmbeddedAsyncServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final AsyncContext ctx = req.startAsync();
            ctx.start(new Runnable() {
                @Override
                public void run() {
                    final HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json;charset=utf-8");
                    EmbeddedAsyncServer.capture("https://api.ipify.org", new EmbeddedAsyncServerCallback() {
                        @Override
                        public void complete(Object result) {
                            try {
                                response.getWriter().println(new Gson().toJson(result));
                            } catch (IOException e) {
                                e.printStackTrace();
                                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            }
                            ctx.complete();
                        }
                    });
                }
            });
        }
    }
}

public class SimplestServer {
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        final QueuedThreadPool threadPool = new QueuedThreadPool(32, 16);
        final Server server = new Server(threadPool);
        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        final ServletHolder asyncHolder = context.addServlet(EmbeddedAsyncServer.EmbeddedAsyncServlet.class, "/");
        asyncHolder.setAsyncSupported(true);
        server.setHandler(context);
        server.start();
        server.join();
    }
}

