package io.github.herburos.vertx.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

@VertxGen
@ProxyGen
public interface MetadataService {

    void streamsMetadata(Handler<AsyncResult<List<HostStoreInfo>>> handler);

    void streamsMetadataForStore(String store, Handler<AsyncResult<List<HostStoreInfo>>> handler);

    void streamsMetadataForStoreAndKey(String store
            , String key
            , Handler<AsyncResult<HostStoreInfo>> handler);

}
