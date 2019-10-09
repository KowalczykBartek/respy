package playground;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import respy.cache.RedisQueryCache;
import respy.core.ConnectionState;
import respy.core.RedisConnection;

import java.util.List;

public class HelloServer {
    private final static Logger LOG = LogManager.getLogger();

    public static void main(String... args) {

        RedisConnection redisConnection = new RedisConnection("127.0.0.1", 6379);
        RedisQueryCache cache = new RedisQueryCache();

        redisConnection.setPushResponseHandler(resp3PushResponse -> {
            LOG.info("Received PUSH notification {}", resp3PushResponse);
            try {
                List<Object> array = (List<Object>) resp3PushResponse.getObject();

                //i assume that everything will go well here.
                if ("invalidate".equals(array.get(0))) {
                    //we got invalidate message.
                    Long slot = (Long) array.get(1);
                    if (slot != null) cache.invalidate(slot);
                }
            } catch (Exception ex) {
                LOG.error("huh something bad happened...", ex);
            }
        });

        redisConnection.setConnectionStateChangeHandler(state -> {
            if (state == ConnectionState.CONNECTED) {
                redisConnection.query("CLIENT TRACKING on\r\n")
                        .thenAccept(result -> LOG.info("client tracking result {}", result));
            }
        });

        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();

        server.requestHandler(request -> {
            Context context = Vertx.currentContext();

            String value = (String) cache.get("hellomessage");

            if (value != null) {
                LOG.info("Got cache hit, with value {}", value);
                request.response().setStatusCode(200).end("{\"value\":\"" + value + "\"}");
            } else {
                redisConnection.query("GET hellomessage\r\n")
                        .thenAccept(resp -> {
                            context.runOnContext((no) -> {
                                if (resp.getObject() != null) {
                                    LOG.info("saving received value {} into cache", resp.getObject());
                                    cache.put("hellomessage", resp.getObject());
                                }
                                request.response().setStatusCode(200).end("{\"value\":\"" + resp.getObject() + "\"}");
                            });
                        });
            }
        });

        server.listen(8080, "localhost", res -> {
            if (res.succeeded()) {
                LOG.info("Example HTTP server is now listening!");
            } else {
                LOG.error("Example HTTP server failed to bind!");
            }
        });
    }
}
