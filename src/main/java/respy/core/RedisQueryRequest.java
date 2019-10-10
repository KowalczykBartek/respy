package respy.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * We need something to wrap CompletableFuture waiting for Redis's response in order to enable stuff like
 * timeout handling.
 */
public class RedisQueryRequest {

    /*
     * Because Redis has no request id that can identify waiting object, we cannot remove object from queue
     * even if timeouted.
     */
    private volatile /*This volatile is probably unnecessary*/ boolean timeouted = false;
    private final long requestTimeStart;
    private final CompletableFuture<Resp3SimpleResponse> completableFuture;

    public RedisQueryRequest(final long requestTimeStart, final CompletableFuture<Resp3SimpleResponse> completableFuture) {
        this.requestTimeStart = requestTimeStart;
        this.completableFuture = completableFuture;
    }

    public CompletableFuture<Resp3SimpleResponse> getCompletableFuture() {
        return completableFuture;
    }

    public long getRequestTimeStart() {
        return requestTimeStart;
    }

    public boolean isTimeouted() {
        return timeouted;
    }

    public void markTimeouted() {
        timeouted = true;
    }

    @Override
    public String toString() {
        return "RedisQueryRequest{" +
                "timeouted=" + timeouted +
                ", requestTimeStart=" + requestTimeStart +
                ", completableFuture=" + completableFuture +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisQueryRequest that = (RedisQueryRequest) o;
        return timeouted == that.timeouted &&
                requestTimeStart == that.requestTimeStart &&
                Objects.equals(completableFuture, that.completableFuture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeouted, requestTimeStart, completableFuture);
    }
}
