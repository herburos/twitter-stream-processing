package home.vertx.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class VerticleTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void beforeAll(TestContext context) {
        Vertx vertx = rule.vertx();

        int port = 12345;

        // Get host
        String host = "localhost";
        context.put("host", host);

        // Setup server
        Async async = context.async();
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(req -> {
            req.response().setStatusCode(200).end();
        });
        server.listen(port, host, ar -> {
            context.assertTrue(ar.succeeded());
            context.put("port", port);
            async.complete();
        });
    }

    @Test
    public void testVerticle2(TestContext context) {
        Vertx vertx = rule.vertx();
        // Get the shared state
        int port = context.get("port");
        String host = context.get("host");

        // Do request
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest req = client.get(port, host, "/resource");
        Async async = context.async();
        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());
            async.complete();
        });
        req.end();
    }

    @Test
    public void testVerticle(TestContext context) {
        Vertx vertx = rule.vertx();
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start(Promise<Void> startPromise) throws Exception {
                startPromise.complete();
            }
        }, context.asyncAssertSuccess());
    }
}
