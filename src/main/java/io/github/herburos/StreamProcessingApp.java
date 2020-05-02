package io.github.herburos;

import io.github.herburos.kafka.KafkaStreamProcessor;
import io.github.herburos.twitter.TwitterStreamProducerVerticle;
import io.github.herburos.vertx.service.MetadataService;
import io.github.herburos.vertx.service.MetadataServiceImpl;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;

public class StreamProcessingApp {
    public static void main(String[] args) {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        System.setProperty(
                "vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.SLF4JLogDelegateFactory"
        );
        ConfigStoreOptions file = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "application.properties"));
        ConfigRetrieverOptions configOpts = new ConfigRetrieverOptions()
                .addStore(file);
        ConfigRetriever retriever = ConfigRetriever.create(Vertx.vertx(), configOpts);

        retriever.getConfig(ar -> {
            if(ar.succeeded()) {
                final JsonObject config = ar.result();
                ClusterManager mgr = new HazelcastClusterManager();

                VertxOptions options = new VertxOptions().setClusterManager(mgr).setHAEnabled(true);
                options.setWorkerPoolSize(16);
                Vertx.clusteredVertx(options, res -> {
                    if (res.succeeded()) {
                        Vertx vertx = res.result();
                        vertx.deployVerticle(new TwitterStreamProducerVerticle(config), new DeploymentOptions().setInstances(1));
                        createStreamProcessor(vertx, config);
                    } else {
                    }
                });
            }
        });
    }

    private static void createStreamProcessor(Vertx vertx, JsonObject config) {
        vertx.executeBlocking(promise -> {
            StreamsBuilder streamsBuilder = new StreamsBuilder();
            KafkaStreamProcessor builder = new KafkaStreamProcessor(config);
            builder.createStream(streamsBuilder);
            KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), builder.getConfig());
            streams.start();
            generateMetadataService(vertx, streams);
            promise.complete();
        }, a -> {

        });
    }

    private static void generateMetadataService(Vertx vertx, KafkaStreams streams) {
        MetadataService service = new MetadataServiceImpl(vertx, streams);
        ServiceBinder binder = new ServiceBinder(vertx);
        MessageConsumer<JsonObject> consumer = binder
                .setAddress("count-service")
                .register(MetadataService.class, service);

        ServiceProxyBuilder proxyBuilder = new ServiceProxyBuilder(vertx).setAddress("count-service");
        MetadataService serviceProxy = proxyBuilder.build(MetadataService.class);
    }
}
