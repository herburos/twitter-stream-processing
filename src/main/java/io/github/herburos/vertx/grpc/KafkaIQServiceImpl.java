package io.github.herburos.vertx.grpc;

import com.google.protobuf.ByteString;
import io.github.herburos.vertx.iq.KafkaIQServiceGrpc;
import io.github.herburos.vertx.iq.KafkaIQServiceRPC;
import io.github.herburos.vertx.service.HostStoreInfo;
import io.github.herburos.vertx.service.MetadataService;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;

import java.util.Objects;

public class KafkaIQServiceImpl extends KafkaIQServiceGrpc.KafkaIQServiceImplBase {
    private final KafkaStreams stream;
    private final String localAddress;
    private final int localPort;
    private final Vertx vertx;
    private final String metadataService;

    public KafkaIQServiceImpl(KafkaStreams stream, Vertx vertx, String localAddress, int localPort, String metadataService) {
        this.stream = stream;
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.vertx = vertx;
        this.metadataService = metadataService;
    }

    @Override
    public void getKV(KafkaIQServiceRPC.GetKVRequest request, StreamObserver<KafkaIQServiceRPC.KVResponse> responseObserver) {
        getMetadataService().streamsMetadataForStoreAndKey(request.getStoreName(), request.getKey(), event -> {
            HostStoreInfo result = event.result();
            if(checkSameHost(result.getHost(), result.getPort())){
                Long count = stream.store(StoreQueryParameters.fromNameAndType(request.getStoreName(), QueryableStoreTypes.<String, Long>keyValueStore()))
                        .get(request.getKey());
                responseObserver.onNext(KafkaIQServiceRPC.KVResponse.newBuilder().setKey(request.getKey()).setValue(count).build());
                responseObserver.onCompleted();
            } else
                getStubFor(result.getHost(), result.getPort()).getKV(request, event1 -> {
                    responseObserver.onNext(event1.result());
                    responseObserver.onCompleted();
                });

        });
    }

    @Override
    public void rangeKV(KafkaIQServiceRPC.RangeKVRequest request, StreamObserver<KafkaIQServiceRPC.KVResponse> responseObserver) {
        super.rangeKV(request, responseObserver);
    }

    @Override
    public void allKV(KafkaIQServiceRPC.AllKVRequest request, StreamObserver<KafkaIQServiceRPC.KVResponse> responseObserver) {
        super.allKV(request, responseObserver);
    }

    @Override
    public void approximateNumEntriesKV(KafkaIQServiceRPC.ApproximateNumEntriesKVRequest request, StreamObserver<KafkaIQServiceRPC.ApproximateNumEntriesKVResponse> responseObserver) {
        getMetadataService().streamsMetadataForStore(request.getStoreName(), event -> {
            HostStoreInfo result = event.result().get(0);
            if(checkSameHost(result.getHost(), result.getPort())){
                long approximation = stream.store(StoreQueryParameters.fromNameAndType(request.getStoreName(), QueryableStoreTypes.<String, Long>keyValueStore()))
                        .approximateNumEntries();
                responseObserver.onNext(KafkaIQServiceRPC.ApproximateNumEntriesKVResponse.newBuilder().setCount(approximation).build());
                responseObserver.onCompleted();
            } else
                getStubFor(result.getHost(), result.getPort()).approximateNumEntriesKV(request, event1 -> {
                    responseObserver.onNext(event1.result());
                    responseObserver.onCompleted();
                });

        });
    }


    private KafkaIQServiceGrpc.KafkaIQServiceVertxStub getStubFor(String ip, int port) {
        ManagedChannel channel = VertxChannelBuilder
                .forAddress(vertx, ip, port)
                .usePlaintext(true)
                .build();
        return KafkaIQServiceGrpc.newVertxStub(channel);
    }

    private MetadataService getMetadataService() {
        ServiceProxyBuilder proxyBuilder = new ServiceProxyBuilder(vertx).setAddress(metadataService);
        return proxyBuilder.build(MetadataService.class);
    }

    public boolean checkSameHost(String ip, int port){
        return Objects.equals(ip, this.localAddress) && port == this.localPort;
    }

}
