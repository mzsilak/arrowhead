package eu.arrowhead.gams.model;

import java.util.concurrent.TimeUnit;

public enum Interval
{
    MINUTELY(TimeUnit.MINUTES.toSeconds(1)),
    HOURLY(TimeUnit.HOURS.toSeconds(1)),
    DAILY(TimeUnit.DAYS.toSeconds(1)),
    WEEKLY(TimeUnit.DAYS.toSeconds(7)),
    FORTNIGHTLY(TimeUnit.DAYS.toSeconds(14)),
    FOUR_WEEKLY(TimeUnit.DAYS.toSeconds(28));

    private final long seconds;

    Interval(final long seconds)
    {
        this.seconds = seconds;
    }

    public long getSeconds()
    {
        return seconds;
    }
}
