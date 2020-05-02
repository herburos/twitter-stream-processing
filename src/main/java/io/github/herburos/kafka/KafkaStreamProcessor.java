package io.github.herburos.kafka;

import io.vertx.core.json.JsonObject;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Arrays;
import java.util.Properties;

public class KafkaStreamProcessor {
    private final String inputTopic;
    private final String outputTopic;
    private Properties config;

    public KafkaStreamProcessor(JsonObject appConfig) {
        this.inputTopic = appConfig.getString("stream.input_topic");
        this.outputTopic = appConfig.getString("stream.output_topic");
        config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, appConfig.getString("stream.app_id"));
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getString("stream.bootstrap_servers"));
        config.put(StreamsConfig.APPLICATION_SERVER_CONFIG, appConfig.getString("stream.application_server_config"));
        config.put(StreamsConfig.CLIENT_ID_CONFIG, appConfig.getString("stream.client_id"));

    }

    public void createStream(StreamsBuilder builder){
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        builder
                .stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()))
                .flatMapValues(sentence -> Arrays.asList(sentence.split("\\W+")))
                .groupBy((key, value) -> value, Grouped.with(Serdes.String(), Serdes.String()))
                .count(Materialized.<String,Long, KeyValueStore<Bytes, byte[]>>as("wordCount").withKeySerde(Serdes.String()).withValueSerde(Serdes.Long()))
                .toStream()
                .to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
    }

    public Properties getConfig(){
        return config;
    }

    public String getInputTopic() {
        return inputTopic;
    }

    public String getOutputTopic() {
        return outputTopic;
    }

}
