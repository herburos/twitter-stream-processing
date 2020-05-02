package io.github.herburos.vertx.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.StreamsMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class MetadataServiceImpl implements MetadataService {

    private final Vertx vertx;
    private final KafkaStreams streams;

    public MetadataServiceImpl(Vertx vertx, KafkaStreams streams) {
        this.vertx = vertx;
        this.streams = streams;
        vertx.eventBus();

    }

    @Override
    public void streamsMetadata(Handler<AsyncResult<List<HostStoreInfo>>> handler) {
        List<HostStoreInfo> infos = streams.allMetadata()
                .stream()
                .map(MetadataServiceImpl::toHostStoreInfo)
                .collect(Collectors.toList());
        handler.handle(Future.succeededFuture(infos));
    }

    @Override
    public void streamsMetadataForStore(String store, Handler<AsyncResult<List<HostStoreInfo>>> handler) {
        List<HostStoreInfo> infos = streams.allMetadataForStore(store)
                .stream()
                .map(MetadataServiceImpl::toHostStoreInfo)
                .collect(Collectors.toList());
        handler.handle(Future.succeededFuture(infos));
    }

    @Override
    public void streamsMetadataForStoreAndKey(String store
            , String key
            , Handler<AsyncResult<HostStoreInfo>> handler) {
        try {
            HostStoreInfo hostStoreInfo = Optional.of(streams.queryMetadataForKey(store, key, new StringSerializer()))
                    .map(MetadataServiceImpl::toHostStoreInfo)
                    .orElseThrow(NoSuchElementException::new);
            handler.handle(Future.succeededFuture(hostStoreInfo));
        } catch (NoSuchElementException ex) {
            handler.handle(ServiceException.fail(404, "ElementNotFoundException"));
        }
    }

    private static HostStoreInfo toHostStoreInfo(final StreamsMetadata metadata) {
        return new HostStoreInfo(metadata.host()
                , metadata.port()
                , new ArrayList<>(metadata.stateStoreNames()));

    }

    private static HostStoreInfo toHostStoreInfo(final KeyQueryMetadata metadata) {
        return new HostStoreInfo(metadata.getActiveHost().host()
                , metadata.getActiveHost().port());
    }
}
