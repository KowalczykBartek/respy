package respy.core;

import java.util.Objects;

public class Resp3Object {
    private final Object value;
    private final Object attribute;

    public Resp3Object(Object value, Object attribute) {
        this.value = value;
        this.attribute = attribute;
    }

    public Object getValue() {
        return value;
    }

    public Object getAttribute() {
        return attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resp3Object that = (Resp3Object) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, attribute);
    }

    @Override
    public String toString() {
        return "Resp3Object{" +
                "value=" + value +
                ", attribute=" + attribute +
                '}';
    }
}
