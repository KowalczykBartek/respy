package playground;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import respy.core.RedisConnection;

public class Main {
    private final static Logger LOG = LogManager.getLogger();

    public static void main(String... args) throws InterruptedException {
        RedisConnection redisConnection = new RedisConnection("127.0.0.1", 6379);
        Thread.sleep(1000);
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
