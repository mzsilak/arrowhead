package eu.arrowhead.gams.evaluators;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A PID controller employing feedback that is widely used in industrial control systems and a variety of other applications requiring continuously modulated control.
 * A PID controller continuously calculates an error value e(t) as the difference between a desired {@link eu.arrowhead.gams.model.TargetValue} and a measured process variable (PV)
 * and applies a correction based on proportional, integral, and derivative terms (denoted P, I, and D respectively), hence the name. The maximum supported frequency is 1000 Hz.
 */
public class PIDLoopEvaluator implements SingleSetPointEvaluator
{
    private long lastTime = 0;
    private long lastError = 0;
    private double integral = 0;

    private double proportionalTerm = 1.0;
    private double integralTerm = 0.5;
    private double derivativeTerm = 0.01;

    private Clock clock;

    public PIDLoopEvaluator()
    {
        super();
    }

    public double getProportionalTerm()
    {
        return proportionalTerm;
    }

    public void setProportionalTerm(final double proportionalTerm)
    {
        this.proportionalTerm = proportionalTerm;
    }

    public double getIntegralTerm()
    {
        return integralTerm;
    }

    public void setIntegralTerm(final double integralTerm)
    {
        this.integralTerm = integralTerm;
    }

    public double getDerivativeTerm()
    {
        return derivativeTerm;
    }

    public void setDerivativeTerm(final double derivativeTerm)
    {
        this.derivativeTerm = derivativeTerm;
    }

    public Clock getClock()
    {
        return clock;
    }

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    @Override
    public Long evaluate(final Long inputValue, final Supplier<Long> targetValue)
    {
        final long setPoint = targetValue.get();
        final long currTime = clock.millis();
        if (lastTime == 0)
        {
            lastTime = currTime;
            lastError = setPoint - inputValue;

            return lastError;
        }

        double dt = (double) (currTime - lastTime) / TimeUnit.SECONDS.toMillis(1);

        if (dt <= 0.0)
        {
            return 0L;
        }

        long error = setPoint - inputValue;
        double derivative = (error - lastError) / dt;

        integral += error * dt;
        lastTime = currTime;
        lastError = error;

        return (long) ((proportionalTerm * error) + (integralTerm * integral) + (derivativeTerm * derivative));
    }
}
