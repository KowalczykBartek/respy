package respy.core;

import java.util.Objects;

public class Resp3ErrorResponse implements Resp3Response {
    private final String msg;

    public Resp3ErrorResponse(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resp3ErrorResponse that = (Resp3ErrorResponse) o;
        return Objects.equals(msg, that.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msg);
    }

    @Override
    public String toString() {
        return "Resp3ErrorResponse{" +
                "msg=" + msg +
                '}';
    }
}
