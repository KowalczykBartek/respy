package respy.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RedisConnection {
    private EventLoopGroup eventLoopGroup;
    private Dispatcher dispatcher;
    private volatile Channel channel;

    public RedisConnection() throws InterruptedException {
        eventLoopGroup = new NioEventLoopGroup();

        dispatcher = new Dispatcher();

        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup)//
                .channel(NioSocketChannel.class)//
                .option(ChannelOption.TCP_NODELAY, true)//
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                        ch.pipeline().addLast(new ParsingHandler());
                        ch.pipeline().addLast(dispatcher);
                    }
                });


        ChannelFuture channelFuture = b.connect("127.0.0.1", 6379);
        channelFuture.addListener(result -> channel = channelFuture.channel());

        channelFuture.sync().await();
    }

    public void setPushResponseHandler(Consumer<Resp3PushResponse> pushResponseHandler) {
        channel.eventLoop().execute(() -> {
            dispatcher.setPushResponseHandler(pushResponseHandler);
        });
    }

    public CompletableFuture<Resp3SimpleResponse> query(String query) {
        final CompletableFuture<Resp3SimpleResponse> resultFuture =
                new CompletableFuture<>();

        channel.eventLoop().execute(() -> {
            ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.heapBuffer().writeBytes(query.getBytes());
            dispatcher.assignCallback(resultFuture);
            channel.writeAndFlush(byteBuf);
        });

        return resultFuture;
    }
}
