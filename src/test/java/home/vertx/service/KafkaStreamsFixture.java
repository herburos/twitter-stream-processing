package home.vertx.service;

import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.StreamsMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KafkaStreamsFixture {

    public static List<StreamsMetadata> streamsMetadataList(){
        List<StreamsMetadata> metadataList = new ArrayList<>();
        metadataList.add(streamsMetadata());
        return metadataList;
    }

    public static StreamsMetadata streamsMetadata(){
        HostInfo localhost = new HostInfo("localhost", 12345);
        Set<String> storeNames = new HashSet<>();
        storeNames.add("simple-store");
        StreamsMetadata streamsMetadata = new StreamsMetadata(localhost, storeNames, null, null, null);
        return streamsMetadata;
    }
}
