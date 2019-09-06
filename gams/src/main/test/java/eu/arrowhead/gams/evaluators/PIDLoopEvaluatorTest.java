package eu.arrowhead.gams.evaluators;

import eu.arrowhead.gams.model.GradientTargetValue;
import eu.arrowhead.gams.model.Interval;
import eu.arrowhead.gams.model.GradientFragment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.TreeSet;

class PIDLoopEvaluatorTest
{

    private static Instant instant;

    @BeforeAll
    static void setupInstant()
    {
        instant = Instant.now();
    }

    @Test
    void testPID()
    {
        Instant currTime = instant;
        long elapsedMinutes = -1;
        long inputValue = 0;
        double factor = 1;

        final PIDLoopEvaluator controller = new PIDLoopEvaluator();
        final GradientTargetValue gradientTargetValue = setupTargetValue();
        gradientTargetValue.setClock(Clock.fixed(currTime, ZoneOffset.UTC));
        controller.setClock(Clock.fixed(currTime, ZoneOffset.UTC));
        controller.evaluate(inputValue, gradientTargetValue);

        for (int i = 0; i < (3600 * factor); i++)
        {
            currTime = currTime.plusMillis((long) (1000.0 / factor));
            gradientTargetValue.setClock(Clock.fixed(currTime, ZoneOffset.UTC));
            controller.setClock(Clock.fixed(currTime, ZoneOffset.UTC));

            long result = controller.evaluate(inputValue, gradientTargetValue);

            if ((currTime.getEpochSecond() - instant.getEpochSecond()) / 60 > elapsedMinutes)
            {
                elapsedMinutes = (currTime.getEpochSecond() - instant.getEpochSecond()) / 60 ;
                System.out.println("SEC:    " + elapsedMinutes);
                System.out.println("OLD:    " + inputValue);
                System.out.println("TARGET: " + gradientTargetValue.get());
                inputValue += result;
                System.out.println("NEW:    " + inputValue);
                System.out.println("DIFF:   " + result);
                System.out.println("================");
            }
            else
            {
                inputValue += result;
            }
        }
    }

    private GradientTargetValue setupTargetValue()
    {
        final Set<GradientFragment> gradients = new TreeSet<>();
        gradients.add(new GradientFragment(0, 100)); // 00
        gradients.add(new GradientFragment(15, 200)); // 15
        gradients.add(new GradientFragment(30, 100)); // 30
        gradients.add(new GradientFragment(45, 0)); // 45

        final GradientTargetValue sut = new GradientTargetValue();
        sut.setInterpolate(true);
        sut.setIntervalStart(instant);
        sut.setMaximumTimeValue(60);
        sut.setGradient(gradients);
        sut.setInterval(Interval.HOURLY);
        return sut;
    }
}