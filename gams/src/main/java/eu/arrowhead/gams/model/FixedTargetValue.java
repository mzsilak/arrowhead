package eu.arrowhead.gams.model;

import java.util.Objects;
import java.util.StringJoiner;

public class FixedTargetValue implements Comparable<FixedTargetValue>, TargetValue
{
    private long value;

    private FixedTargetValue()
    {
        super();
    }

    public long getValue()
    {
        return value;
    }

    public void setValue(final long value)
    {
        this.value = value;
    }

    @Override
    public Long get()
    {
        return value;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final FixedTargetValue that = (FixedTargetValue) o;
        return value == that.value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value);
    }

    @Override
    public int compareTo(final FixedTargetValue other)
    {
        return Long.compare(this.value, other.value);
    }

    @Override
    public String toString()
    {
        return new StringJoiner(", ", FixedTargetValue.class.getSimpleName() + "[", "]")
                .add("value=" + value)
                .toString();
    }
}
