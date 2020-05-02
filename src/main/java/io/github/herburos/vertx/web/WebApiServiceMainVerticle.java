package io.github.herburos.vertx.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.serviceproxy.ServiceBinder;

public class WebApiServiceMainVerticle extends AbstractVerticle {
    HttpServer server;
    ServiceBinder serviceBinder;

    MessageConsumer<JsonObject> consumer;

    private void startMetadataService() {
        serviceBinder = new ServiceBinder(vertx);

        MetadataWebService metadataService = MetadataWebService.create();
        consumer = serviceBinder
                .setAddress("metadata_service.myapp")
                .register(MetadataWebService.class, metadataService);
    }

    private Future<Void> startHttpServer() {
        Promise<Void> promise = Promise.promise();
        OpenAPI3RouterFactory.create(this.vertx, "/metadata.json", openAPI3RouterFactoryAsyncResult -> {
            if (openAPI3RouterFactoryAsyncResult.succeeded()) {
                OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

                // Mount services on event bus based on extensions
                routerFactory.mountServicesFromExtensions();

                // Generate the router
                Router router = routerFactory.getRouter();
                server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
                server.requestHandler(router).listen(ar -> {
                    // Error starting the HttpServer
                    if (ar.succeeded()) promise.complete();
                    else promise.fail(ar.cause());
                });
            } else {
                // Something went wrong during router factory initialization
                promise.fail(openAPI3RouterFactoryAsyncResult.cause());
            }
        });
        return promise.future();
    }

    @Override
    public void start(Promise<Void> promise) {
        startMetadataService();
        startHttpServer().onComplete(promise);
    }

    @Override
    public void stop(){
        this.server.close();
        consumer.unregister();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new WebApiServiceMainVerticle());
    }
}
