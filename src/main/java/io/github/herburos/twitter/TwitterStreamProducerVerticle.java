package io.github.herburos.twitter;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterStreamProducerVerticle extends AbstractVerticle {

    private final JsonObject config;
    private KafkaProducer<String,String> kafkaProducer;
    private Disposable disposable;

    public TwitterStreamProducerVerticle(JsonObject config) {
        this.config = config;

    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        kafkaProducer = createKafkaProducer(config);
        Pattern pattern = Pattern.compile(".*(#\\w+).*");
        disposable = twitterObservable(config)
                .map(Status::getText)
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .subscribe(tag -> {
                    KafkaProducerRecord<String, String> record =
                            KafkaProducerRecord.create("input-topic", "hashtags", tag);
                    kafkaProducer.send(record);
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        disposable.dispose();
    }

    private Observable<Status> twitterObservable(JsonObject config) {
        return Observable.create(subscriber -> {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(config.getString("twitter.oauth.consumer.key"))
                    .setOAuthConsumerSecret(config.getString("twitter.oauth.consumer.secret"))
                    .setOAuthAccessToken(config.getString("twitter.oauth.access.token"))
                    .setOAuthAccessTokenSecret(config.getString("twitter.oauth.access.token.secret"));
            TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
            TwitterStream twitterStream = tf.getInstance();
            twitterStream.addListener(new StatusAdapter() {
                public void onStatus(Status status) {
                    subscriber.onNext(status);
                }
                public void onException(Exception ex) {
                    subscriber.onError(ex);
                }
            });
            twitterStream.sample();
            subscriber.setCancellable(twitterStream::cleanUp);
        });
    }

    private KafkaProducer<String,String> createKafkaProducer(JsonObject jsonConfig) {
        HashMap<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "127.0.0.1:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "1");

        return KafkaProducer.createShared(vertx, "sharedProducer",config, String.class, String.class);
    }
}
