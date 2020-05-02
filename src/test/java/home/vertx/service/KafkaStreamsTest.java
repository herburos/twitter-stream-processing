package home.vertx.service;

import io.github.herburos.kafka.KafkaStreamProcessor;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

@Ignore
public class KafkaStreamsTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<Long, String> inputTopic;
    private TestOutputTopic<Long, String> outputTopic;

    private final Instant recordBaseTime = Instant.parse("2019-06-01T10:00:00Z");
    private final Duration advance1Min = Duration.ofMinutes(1);

    private KafkaStreamProcessor streamBuilder;

    @Before
    public void setup() {
        final StreamsBuilder builder = new StreamsBuilder();
        streamBuilder = new KafkaStreamProcessor(null);
        streamBuilder.createStream(builder);
        testDriver = new TopologyTestDriver(builder.build(), streamBuilder.getConfig());
        inputTopic = testDriver.createInputTopic(
                streamBuilder.getInputTopic(),
                new LongSerializer(),
                new StringSerializer(),
                recordBaseTime, advance1Min);
        outputTopic = testDriver.createOutputTopic(
                streamBuilder.getOutputTopic(),
                new LongDeserializer(),
                new StringDeserializer());
    }

    @Test
    public void testOnlyValue() {
        inputTopic.pipeInput(9L, "Hello");
        Assertions.assertThat(outputTopic.readValue()).isEqualTo("Hello");
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    public void testReadFromEmptyTopic() {
        inputTopic.pipeInput(9L, "Hello");
        Assertions.assertThat(outputTopic.readValue()).isEqualTo("Hello");
        //Reading from empty topic generate Exception
        Assertions.assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> {
            outputTopic.readValue();
        }).withMessage("Empty topic: %s", streamBuilder.getOutputTopic());
    }

    @Test
    public void testKeyValue() {
        //Feed 9 as key and word "Hello" as value to inputTopic
        inputTopic.pipeInput(9L, "Hello");
        //Read KeyValue and validate it, timestamp is irrelevant in this case
        Assertions.assertThat(outputTopic.readKeyValue()).isEqualTo(new KeyValue<>(9L, "Hello"));
        //No more output in topic
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    public void testKeyValueTimestamp() {
        final Instant recordTime = Instant.parse("2019-06-01T10:00:00Z");
        //Feed 9 as key and word "Hello" as value to inputTopic with record timestamp
        inputTopic.pipeInput(9L, "Hello", recordTime);
        //Read TestRecord and validate it
        Assertions.assertThat(outputTopic.readRecord()).isEqualTo(new TestRecord<>(9L, "Hello", recordTime));
        //No more output in topic
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    public void testHeadersIgnoringTimestamp() {
        final Headers headers = new RecordHeaders(
                new Header[]{
                        new RecordHeader("foo", "value".getBytes())
                });
        //Feed 9 as key, word "Hello" as value, and header to inputTopic with record timestamp filled by processing
        inputTopic.pipeInput(new TestRecord<>(9L, "Hello", headers));
        //Using isEqualToIgnoringNullFields to ignore validating record time
        Assertions.assertThat(outputTopic.readRecord()).isEqualToIgnoringNullFields(new TestRecord<>(9L, "Hello", headers));
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    public void testKeyValueList() {
        final List<String> inputList = Arrays.asList("This", "is", "an", "example");
        final List<KeyValue<Long, String>> expected = new LinkedList<>();
        for (final String s : inputList) {
            //Expected list contains original values as keys
            expected.add(new KeyValue<>(null, s));
        }
        //Pipe in value list
        inputTopic.pipeValueList(inputList);
        Assertions.assertThat(outputTopic.readKeyValuesToList()).hasSameElementsAs(expected);
    }

    @Test
    public void testRecordList() {
        final List<String> inputList = Arrays.asList("This", "is", "an", "example");
        final List<KeyValue<Long, String>> input = new LinkedList<>();
        final List<TestRecord<Long, String>> expected = new LinkedList<>();
        long i = 1;
        Instant recordTime = recordBaseTime;
        for (final String s : inputList) {
            input.add(new KeyValue<>(i, s));
            //Expected entries have key and value swapped and recordTime advancing 1 minute in each
            expected.add(new TestRecord<>(i, s, recordTime));
            recordTime = recordTime.plus(advance1Min);
            i++;
        }
        //Pipe in KeyValue list
        inputTopic.pipeKeyValueList(input);
        Assertions.assertThat(outputTopic.readRecordsToList()).hasSameElementsAs(expected);
    }

    @Test
    public void testValueMap() {
        final List<String> inputList = Arrays.asList("a", "b", "c", "a", "b");
        final List<KeyValue<Long, String>> input = new LinkedList<>();
        long i = 1;
        for (final String s : inputList) {
            input.add(new KeyValue<>(i++, s));
        }
        //Pipe in KeyValue list
        inputTopic.pipeKeyValueList(input);
        //map contain the last index of each entry
        Assertions.assertThat(outputTopic.readKeyValuesToMap()).hasSize(5)
                .containsEntry(4L, "a")
                .containsEntry(5L, "b")
                .containsEntry(3L, "c")
                .containsEntry(1L, "a")
                .containsEntry(2L, "b");
    }
}
