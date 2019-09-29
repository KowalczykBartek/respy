package playground;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import respy.core.RedisConnection;

import java.util.concurrent.CountDownLatch;

public class Main {
    private final static Logger LOG = LogManager.getLogger();

    public static void main(String... args) throws InterruptedException {
        RedisConnection redisConnection = new RedisConnection("127.0.0.1", 6379);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        redisConnection.setConnectionStateChangeHandler(t -> countDownLatch.countDown());
        countDownLatch.await();

        redisConnection.setPushResponseHandler(resp3PushResponse -> LOG.info("Received PUSH notification {}", resp3PushResponse));

        redisConnection.query("SUBSCRIBE first\r\n")//
                .thenCompose(response -> {
                    LOG.info("response {}", response);
                    return redisConnection.query("HSET A B C\r\n");
                })
                .thenAccept(response -> {
                    LOG.info("response {}", response);
                })
                .exceptionally(err -> {
                    LOG.error("Error received when querying redis.", err);
                    return null;
                });

    }
}
