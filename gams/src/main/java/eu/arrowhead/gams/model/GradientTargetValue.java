package eu.arrowhead.gams.model;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class GradientTargetValue implements TargetValue
{
    /**
     * Interpolate the actual value between 2 points in time.
     */
    private boolean interpolate;

    /**
     * The start of the gradient.
     */
    private Instant intervalStart;

    /**
     * Defines the end value of the given interval relative to the given time values in the gradient.
     * No {@link TimeBasedTargetValue} may have a bigger time value bigger than this.
     */
    private Duration maximumTimeValue;

    /**
     * Which interval is used in the {@link #gradient}? i.e. when shall the pattern be repeated?
     */
    private Interval interval;

    /**
     * The actual gradient as a unique set of {@link TimeBasedTargetValue}s.
     */
    private Set<TimeBasedTargetValue> gradient;

    public GradientTargetValue()
    {
        this.gradient = new TreeSet<>();
        interpolate = true;
    }

    public GradientTargetValue(final Collection<TimeBasedTargetValue> gradient)
    {
        this();
        this.gradient.addAll(gradient);
    }

    public boolean isInterpolate()
    {
        return interpolate;
    }

    public void setInterpolate(final boolean interpolate)
    {
        this.interpolate = interpolate;
    }

    public Instant getIntervalStart()
    {
        return intervalStart;
    }

    public void setIntervalStart(final Instant intervalStart)
    {
        this.intervalStart = intervalStart;
    }

    public Duration getMaximumTimeValue()
    {
        return maximumTimeValue;
    }

    public void setMaximumTimeValue(final Duration maximumTimeValue)
    {
        this.maximumTimeValue = maximumTimeValue;
    }

    public Interval getInterval()
    {
        return interval;
    }

    public void setInterval(final Interval interval)
    {
        this.interval = interval;
    }

    public void setGradient(final Set<TimeBasedTargetValue> gradient)
    {
        this.gradient = gradient;
    }

    public Set<TimeBasedTargetValue> getGradient()
    {
        return Collections.unmodifiableSet(gradient);
    }

    @Override
    public Long getTargetValue()
    {
        if (gradient.isEmpty())
        {
            throw new IllegalStateException("No gradient values defined!");
        }

        final long secondsInInterval = interval.getSeconds();
        final long secondsSinceEpoch = Instant.now().getEpochSecond();
        final long intervalStartInSeconds = intervalStart.getEpochSecond();

        long secondsSinceIntervalStart = intervalStartInSeconds - secondsSinceEpoch;
        while (secondsSinceIntervalStart > secondsInInterval)
        {
            secondsSinceIntervalStart -= secondsInInterval;
        }

        final long maxTimeValueInSeconds = maximumTimeValue.get(ChronoUnit.SECONDS);
        final long targetTime = (maxTimeValueInSeconds * secondsSinceIntervalStart) / secondsInInterval;

        TimeBasedTargetValue current = null;
        TimeBasedTargetValue next = null;

        for (final TimeBasedTargetValue targetValue : gradient)
        {
            next = targetValue;

            if (Objects.isNull(current))
            {
                current = targetValue;
            }

            if (next.getTime() >= targetTime)
            {
                break;
            }
        }

        Objects.requireNonNull(current, "Unable to find previous gradient value");
        Objects.requireNonNull(next, "Unable to find next gradient value");

        if (interpolate)
        {
            final long relativeTargetTime = targetTime - current.getTime();
            final long durationBetweenGradient = next.getTime() - current.getTime();
            final long differenceInValue = next.getValue() - current.getValue();
            final long relativeValue = (differenceInValue * relativeTargetTime) / durationBetweenGradient;
            return current.getValue() + relativeValue;
        }
        else
        {
            return current.getValue();
        }
    }
}
