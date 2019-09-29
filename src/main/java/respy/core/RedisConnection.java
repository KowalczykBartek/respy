package respy.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wrapper for single connection to REDIS.
 */
public class RedisConnection {
    private final static Logger LOG = LogManager.getLogger();

    private final String host;
    private final int port;

    private EventLoopGroup eventLoopGroup;
    private Dispatcher dispatcher;
    private volatile Channel channel;
    private final Bootstrap bootstrap;

    private final ChannelFuture channelFuture;

    private boolean connected; // no need for volatile, always accessed from the same thread.

    public RedisConnection(String host, int port) {
        this.host = host;
        this.port = port;

        //NEVER GREATER THAN 1
        this.eventLoopGroup = new NioEventLoopGroup(1);
        this.dispatcher = new Dispatcher();
        Bootstrap b = new Bootstrap();

        //FIXME support TLS
        this.bootstrap = b.group(eventLoopGroup)//
                .channel(NioSocketChannel.class)//
                .option(ChannelOption.TCP_NODELAY, true)//
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                        ch.pipeline().addLast(new ParsingHandler());
                        ch.pipeline().addLast(dispatcher);
                    }
                });

        channelFuture = bootstrap.connect(host, port);
        doConnect(channelFuture);
    }

    private void scheduleReconnect() {
        eventLoopGroup.schedule(() -> {
            ChannelFuture channelFuture = bootstrap.connect(host, port);
            doConnect(channelFuture);
        }, 1, TimeUnit.SECONDS);
    }


    private void doConnect(ChannelFuture channelFuture) {
        channelFuture.addListener(result -> {
            if (result.isSuccess()) {
                channel = channelFuture.channel();
                setUpCloseFuture();
                doQuery(new CompletableFuture<>(), "HELLO 3\r\n")
                        .thenAccept(rsp -> {
                            //If we are here - for now it means OK and RESP3 is supported
                            LOG.info("Got response for HELLO3 message {}", rsp);
                            connected = true;
                        })
                        .exceptionally(ex -> {
                            LOG.error("HELLO message completed exceptionally {}", ex.getMessage());
                            scheduleReconnect();
                            return null;
                        });
            } else {
                LOG.error("Unable to connect REDIS instance on port {} and host {}", port, host);
                scheduleReconnect();
            }
        });
    }

    private void setUpCloseFuture() {
        channel.closeFuture().addListener(listener -> {
            connected = false;
            LOG.error("Connection closed - scheduling retry");
            scheduleReconnect();
        });
    }

    public void setPushResponseHandler(Consumer<Resp3PushResponse> pushResponseHandler) {
        eventLoopGroup.execute(() -> dispatcher.setPushResponseHandler(pushResponseHandler));
    }

    public CompletableFuture<Resp3SimpleResponse> query(String query) {
        final CompletableFuture<Resp3SimpleResponse> resultFuture = new CompletableFuture<>();

        eventLoopGroup.execute(() -> {
            if (!connected) {
                resultFuture.completeExceptionally(new RuntimeException("Not connected"));
                return;
            }
            doQuery(resultFuture, query);
        });

        return resultFuture;
    }

    private CompletableFuture<Resp3SimpleResponse> doQuery(CompletableFuture<Resp3SimpleResponse> resultFuture, String query) {
        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.heapBuffer().writeBytes(query.getBytes());
        channel.writeAndFlush(byteBuf)
                .addListener(writeHandler -> {
                    /*
                     * I have doubts about that but I think that for now on, it is safe enough.
                     * FIXME - check source-code and ensure that callback is always called in order.
                     */
                    if (writeHandler.isSuccess()) {
                        dispatcher.assignCallback(resultFuture);
                    } else {
                        resultFuture.completeExceptionally(writeHandler.cause());
                    }
                });

        return resultFuture;
    }
}
