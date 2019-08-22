package eu.arrowhead.gams.model;

import java.time.Clock;
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
    private long maximumTimeValue;

    /**
     * Which interval is used in the {@link #gradient}? i.e. when shall the pattern be repeated?
     */
    private Interval interval;

    /**
     * The actual gradient as a unique set of {@link TimeBasedTargetValue}s.
     */
    private Set<TimeBasedTargetValue> gradient;

    private Clock clock;

    public GradientTargetValue()
    {
        this.gradient = new TreeSet<>();
        this.interpolate = true;
        this.clock = Clock.systemUTC();
    }

    public GradientTargetValue(final Clock clock)
    {
        this.gradient = new TreeSet<>();
        this.interpolate = true;
        this.clock = clock;
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

    public long getMaximumTimeValue()
    {
        return maximumTimeValue;
    }

    public void setMaximumTimeValue(final long maximumTimeValue)
    {
        this.maximumTimeValue = maximumTimeValue;
    }

    public Clock getClock()
    {
        return clock;
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
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
        this.gradient.clear();
        this.gradient.addAll(gradient);
    }

    public Set<TimeBasedTargetValue> getGradient()
    {
        return Collections.unmodifiableSet(gradient);
    }

    @Override
    public Long get()
    {
        if (gradient.isEmpty())
        {
            throw new IllegalStateException("No gradient values defined!");
        }

        final long secondsInInterval = interval.getSeconds();
        final long secondsSinceEpoch = Instant.now(clock).getEpochSecond();
        final long intervalStartInSeconds = intervalStart.getEpochSecond();

        long secondsSinceIntervalStart = secondsSinceEpoch - intervalStartInSeconds;
        // in case we are in the past somehow
        while (secondsSinceIntervalStart < 0)
        {
            secondsSinceIntervalStart += secondsInInterval;
        }
        while (secondsSinceIntervalStart > secondsInInterval)
        {
            secondsSinceIntervalStart -= secondsInInterval;
        }

        // proportion calculation of target time. where are we based on the whole interval duration
        final double targetTime = (maximumTimeValue * secondsSinceIntervalStart) / (double) secondsInInterval;

        TimeBasedTargetValue current = null;
        TimeBasedTargetValue next = null;

        // find the 2 target values which are before and after our target time
        for (final TimeBasedTargetValue targetValue : gradient)
        {
            next = targetValue;

            if (Objects.isNull(current))
            {
                current = targetValue;
            }

            if (next.getTime() > targetTime)
            {
                break;
            }
        }

        Objects.requireNonNull(current, "Unable to find previous gradient value");
        Objects.requireNonNull(next, "Unable to find next gradient value");

        // if the target time is between to target values, we make a linear interpolation
        if (interpolate && current != next)
        {
            final double relativeTargetTime = targetTime - current.getTime();
            final long durationBetweenGradient = next.getTime() - current.getTime();
            final long differenceInValue = next.getValue() - current.getValue();
            final double relativeValue = (differenceInValue * relativeTargetTime) / durationBetweenGradient;
            return current.getValue() + (long) relativeValue;
        }

        return current.getValue();
    }
}
