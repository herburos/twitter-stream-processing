package home.twitter;

import io.reactivex.Observable;
import org.junit.Test;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterStreamTest {
    @Test
    public void testTwitterStream() throws InterruptedException {
        Pattern pattern = Pattern.compile(".*(#\\w+).*");
        twitterObservable()
                .map(Status::getText)
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .subscribe(System.out::println);

        Thread.sleep(100000);
    }

    public Observable<Status> twitterObservable() {
        return Observable.create(subscriber -> {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey("")
                    .setOAuthConsumerSecret("")
                    .setOAuthAccessToken("")
                    .setOAuthAccessTokenSecret("");
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
}
