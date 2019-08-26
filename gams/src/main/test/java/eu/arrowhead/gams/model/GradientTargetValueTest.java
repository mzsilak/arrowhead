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
        assertEquals(99, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 45), ZoneOffset.UTC));
        assertEquals(198, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 60), ZoneOffset.UTC));
        assertEquals(396, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(3601), ZoneOffset.UTC));
        assertEquals(0L, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(60 * 65), ZoneOffset.UTC));
        assertEquals(33, (long) sut.get());
    }

    @Test
    void testMinutelyGradient()
    {
        final GradientTargetValue sut = setupTargetValue();
        sut.setInterval(Interval.MINUTELY);

        sut.setClock(Clock.fixed(instant, ZoneOffset.UTC));
        assertEquals(0L, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(10), ZoneOffset.UTC));
        assertEquals(66, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(20), ZoneOffset.UTC));
        assertEquals(99, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(30), ZoneOffset.UTC));
        assertEquals(99, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(40), ZoneOffset.UTC));
        assertEquals(165, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(50), ZoneOffset.UTC));
        assertEquals(264L, (long) sut.get());

        sut.setClock(Clock.fixed(instant.plusSeconds(65), ZoneOffset.UTC));
        assertEquals(33, (long) sut.get());
    }

    private GradientTargetValue setupTargetValue()
    {
        final Set<GradientFragment> gradients = new TreeSet<>();
        gradients.add(new GradientFragment(0, 0)); // 00
        gradients.add(new GradientFragment(10, 99)); // 15
        gradients.add(new GradientFragment(20, 99)); // 30
        gradients.add(new GradientFragment(30, 198)); // 45
        gradients.add(new GradientFragment(40, 396)); // 60

        final GradientTargetValue sut = new GradientTargetValue();
        sut.setInterpolate(true);
        sut.setIntervalStart(instant);
        sut.setMaximumTimeValue(40);
        sut.setGradient(gradients);
        sut.setInterval(Interval.HOURLY);
        return sut;
    }
}