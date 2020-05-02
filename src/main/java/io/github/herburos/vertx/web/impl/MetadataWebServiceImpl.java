package io.github.herburos.vertx.web.impl;


import io.github.herburos.vertx.web.HelloWorld;
import io.github.herburos.vertx.web.MetadataWebService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;

public class MetadataWebServiceImpl implements MetadataWebService {
    @Override
    public void hello(String from, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(
                OperationResponse.completedWithJson(new HelloWorld("Hello From " + from).toJson())
        ));
    }
}
