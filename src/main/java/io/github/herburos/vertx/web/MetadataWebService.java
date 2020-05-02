package io.github.herburos.vertx.web;

import io.github.herburos.vertx.web.impl.MetadataWebServiceImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;

@WebApiServiceGen
public interface MetadataWebService {
    static MetadataWebService create() {
        return new MetadataWebServiceImpl();
    }

    void hello(
            String from,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);
}
