package respy.core;

import java.util.Objects;

public class Resp3PushResponse implements Resp3Response {
    private final Resp3Object object;

    public Resp3PushResponse(Resp3Object object) {
        this.object = object;
    }

    public Resp3Object getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resp3PushResponse that = (Resp3PushResponse) o;
        return Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }

    @Override
    public String toString() {
        return "Resp3PushResponse{" +
                "object=" + object +
                '}';
    }
}
