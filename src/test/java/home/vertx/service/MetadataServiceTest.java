package home.vertx.service;


import io.github.herburos.vertx.service.HostStoreInfo;
import io.github.herburos.vertx.service.MetadataService;
import io.github.herburos.vertx.service.MetadataServiceImpl;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import org.apache.kafka.streams.KafkaStreams;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(VertxUnitRunner.class)
public class MetadataServiceTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Mock
    KafkaStreams kafkaStreams;

    @Before
    public void beforeAll(TestContext context) {
        MockitoAnnotations.initMocks(this);

        Vertx vertx = rule.vertx();
        ServiceBinder binder = new ServiceBinder(vertx);
        MessageConsumer<JsonObject> consumer = binder
                .setAddress("metadata-service")
                .register(MetadataService.class, new MetadataServiceImpl(vertx, kafkaStreams));
        Mockito.when(kafkaStreams.allMetadata()).thenReturn(KafkaStreamsFixture.streamsMetadataList());
    }

    @Test
    public void testGetAllMetadataService(TestContext context) {
        Async async = context.async();
        Vertx vertx = rule.vertx();
        ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress("metadata-service");
        MetadataService serviceProxy = builder.build(MetadataService.class);
        serviceProxy.streamsMetadata(ar -> {
            if (ar.succeeded()) {
                List<HostStoreInfo> result = ar.result();
                Assertions.assertThat(result).isNotNull();
                Assertions.assertThat(result).contains(new HostStoreInfo("localhost", 12345));
                async.complete();
            } else {
                context.fail();
            }
        });
    }
}
