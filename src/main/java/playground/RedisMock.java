package playground;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

import java.util.concurrent.atomic.AtomicLong;

public class RedisMock {
    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();
        NetServer server = vertx.createNetServer();

        server.connectHandler(socket -> {
            AtomicLong atomicLong = new AtomicLong();
            socket.handler(data -> {
                long iteration = atomicLong.getAndIncrement();

                System.out.println(new String(data.getBytes()));

                if (iteration == 0) {
                    socket.write("%1\r\n" +
                            "$6\r\n" +
                            "server\r\n" +
                            "$5\r\n" +
                            "redis\r\n");
                } else if (iteration == 1) {
                    socket.write("+OK\r\n");
                } else if (iteration == 2) {
                    vertx.setTimer(2_000, i -> {
                        socket.write("+XDXDXDXDXD\r\n");
                    });
                } else {
                  socket.write("+XDXDXDXDXD\r\n");
                }
            });

        });

        server.listen(6379, "127.0.0.1", res -> {
            if (res.succeeded()) {
                System.out.println("Server is now listening on actual port: " + server.actualPort());
            } else {
                System.out.println("Failed to bind!");
            }
        });

    }
}
