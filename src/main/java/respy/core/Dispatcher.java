package respy.core;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@ChannelHandler.Sharable
public class Dispatcher extends SimpleChannelInboundHandler<Resp3Response> {

    private final static Logger LOG = LogManager.getLogger();
    private final Queue<CompletableFuture<Resp3SimpleResponse>> queue = new LinkedBlockingQueue<>();

    //always accessed from the same thread, no need for volatile
    private Consumer<Resp3PushResponse> pushResponseHandler = (resp3PushResponse) -> {
        LOG.warn("Push response received and no handler defined. Resp: {}", resp3PushResponse);
    };

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Resp3Response msg) {

        if (msg instanceof Resp3PushResponse) {
            //push works a little bit different
            pushResponseHandler.accept((Resp3PushResponse) msg);
            return;
        }

        CompletableFuture<Resp3SimpleResponse> resp = queue.poll();
        if (resp != null) {
            if (msg instanceof Resp3ErrorResponse) {
                String cause = ((Resp3ErrorResponse) msg).getMsg();
                resp.completeExceptionally(new RuntimeException(cause));
            } else {
                resp.complete((Resp3SimpleResponse) msg);
            }
        } else {
            LOG.error("No handler for received response");
        }
    }

    public void assignCallback(CompletableFuture<Resp3SimpleResponse> resultFuture) {
        queue.add(resultFuture);
    }

    public void setPushResponseHandler(Consumer<Resp3PushResponse> pushResponseHandler) {
        this.pushResponseHandler = pushResponseHandler;
    }
}
