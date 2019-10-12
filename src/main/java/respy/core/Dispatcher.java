package respy.core;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@ChannelHandler.Sharable
public class Dispatcher extends SimpleChannelInboundHandler<Resp3Response> {

    private final static Logger LOG = LogManager.getLogger();
    private final Queue<RedisQueryRequest> queue = new LinkedBlockingQueue<>();
    private EventLoopGroup eventLoopGroup;

    //always accessed from the same thread, no need for volatile
    private Consumer<Resp3PushResponse> pushResponseHandler = (resp3PushResponse) -> {
        LOG.warn("Push response received and no handler defined. Resp: {}", resp3PushResponse);
    };

    /**
     * Construct Dispatcher object.
     *
     * @param eventLoopGroup - it never ever can have more than one thread !
     */
    public Dispatcher(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        setupTimeoutJob();
    }

    /*
     * Because no other thread will access stuff in Dispatcher - we can do whatever we want with no locking.
     * All parameters related to timeout handling are hardcoded.
     */
    private void setupTimeoutJob() {
        eventLoopGroup.scheduleAtFixedRate(() -> {

            /*
             * We cannot wait for the request infinitely, but we also cannot remove the object
             * from the queue - FIXME in case of situation when queue of waiting objects grow to X we have to
             * brake connection.
             */

            Iterator<RedisQueryRequest> iterator = queue.iterator();
            int i = 0;
            long timeNow = System.currentTimeMillis();
            /*
             * we are tracking 'i' because we want to do only some work, not all the work.
             * we cannot take too much time in timeout checking.
             */

            while (iterator.hasNext() && i < 100) {
                RedisQueryRequest current = iterator.next();
                if (current.isTimeouted()) {
                    //already been here.
                    continue;
                }
                long whenRequestStarted = current.getRequestTimeStart();
                long requestTimeUntilNow = timeNow - whenRequestStarted;
                if (requestTimeUntilNow >= 1000) {
                    LOG.error("Timeouted request detected");
                    current.markTimeouted();
                    current.getCompletableFuture().completeExceptionally(new TimeoutException("Timeout occurred."));
                }

                i++;
            }

        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Resp3Response msg) {

        if (msg instanceof Resp3PushResponse) {
            //push works a little bit different
            pushResponseHandler.accept((Resp3PushResponse) msg);
            return;
        }

        RedisQueryRequest resp = queue.poll();
        if (resp != null) {

            if (resp.isTimeouted()) {
                //request already timeouted, just ignore.
                return;
            }

            CompletableFuture<Resp3SimpleResponse> requestFuture = resp.getCompletableFuture();
            if (msg instanceof Resp3ErrorResponse) {
                String cause = ((Resp3ErrorResponse) msg).getMsg();
                requestFuture.completeExceptionally(new RuntimeException(cause));
            } else {
                requestFuture.complete((Resp3SimpleResponse) msg);
            }
        } else {
            LOG.error("No handler for received response");
        }
    }

    public void assignCallback(CompletableFuture<Resp3SimpleResponse> resultFuture) {
        RedisQueryRequest redisQueryRequest = new RedisQueryRequest(System.currentTimeMillis(), resultFuture);
        queue.add(redisQueryRequest);
    }

    public void setPushResponseHandler(Consumer<Resp3PushResponse> pushResponseHandler) {
        this.pushResponseHandler = pushResponseHandler;
    }
}
