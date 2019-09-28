package playground;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import respy.core.RedisConnection;

public class Main {
    private final static Logger LOG = LogManager.getLogger();

    public static void main(String... args) throws InterruptedException {
        RedisConnection redisConnection = new RedisConnection();

        redisConnection.setPushResponseHandler(resp3PushResponse -> LOG.info("Received PUSH notification {}", resp3PushResponse));

        redisConnection.query("HELLO 3\r\n")//
                .thenCompose(response -> {
                    LOG.info("response {}", response);
                    return redisConnection.query("HSET A B C\r\n");
                })
                .thenCompose(response -> {
                    return redisConnection.query("SUBSCRIBE first\r\n");
                })
                .thenAccept(response -> {
                    LOG.info("response {}", response);
                });
    }
}
