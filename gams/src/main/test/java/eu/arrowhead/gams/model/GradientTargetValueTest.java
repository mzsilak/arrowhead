package eu.arrowhead.gams.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradientTargetValueTest
{

    private static Instant instant;

    @BeforeAll
    static void setupInstant()
    {
        instant = Instant.now();
    }

    @Test
    void testHourlyGradient()
    {
        final GradientTargetValue sut = setupTargetValue();

        sut.setClock(Clock.fixed(instant, ZoneOffset.UTC));
        assertEquals(0L, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 15), ZoneOffset.UTC));
        assertEquals(99, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 30), ZoneOffset.UTC));
        assertEquals(198, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 45), ZoneOffset.UTC));
        assertEquals(297, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 60), ZoneOffset.UTC));
        assertEquals(396, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(3601), ZoneOffset.UTC));
        assertEquals(0L, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 65), ZoneOffset.UTC));
        assertEquals(33, (long) sut.get());
    }

    private GradientTargetValue setupTargetValue()
    {
        final Set<TimeBasedTargetValue> gradients = new TreeSet<>();
        gradients.add(new TimeBasedTargetValue(0, 0)); // 00
        gradients.add(new TimeBasedTargetValue(10, 99)); // 15
        gradients.add(new TimeBasedTargetValue(20, 198)); // 30
        gradients.add(new TimeBasedTargetValue(30, 297)); // 45
        gradients.add(new TimeBasedTargetValue(40, 396)); // 60

        final GradientTargetValue sut = new GradientTargetValue();
        sut.setInterpolate(true);
        sut.setIntervalStart(instant);
        sut.setMaximumTimeValue(40);
        sut.setGradient(gradients);
        sut.setInterval(Interval.HOURLY);
        return sut;
    }
}