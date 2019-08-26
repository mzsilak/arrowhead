package eu.arrowhead.gams.model;

import java.util.Objects;
import java.util.StringJoiner;

public class GradientFragment implements Comparable<GradientFragment>
{
    private long time;
    private long value;

    public GradientFragment()
    {
        super();
    }

    public GradientFragment(final long time, final long value)
    {
        this.time = time;
        this.value = value;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(final long time)
    {
        this.time = time;
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
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final GradientFragment that = (GradientFragment) o;
        return this.time == that.time &&
                this.value == that.value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(time, value);
    }

    @Override
    public int compareTo(final GradientFragment other)
    {
        return Long.compare(this.time, other.time);
    }

    @Override
    public String toString()
    {
        return new StringJoiner(", ", GradientFragment.class.getSimpleName() + "[", "]")
                .add("time=" + time)
                .add("value=" + value)
                .toString();
    }
}
