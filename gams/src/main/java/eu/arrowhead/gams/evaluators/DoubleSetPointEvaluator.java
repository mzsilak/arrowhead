package eu.arrowhead.gams.evaluators;

import java.util.function.Supplier;

/**
 * A Double-SetPoint controller is similar to the {@link BangBangEvaluator}, however it has two set points on which it switches the output.
 * It returns a positive number if the input is below the lower set point and a negative if the number exceeds the upper set point. It returns 0 in all other cases.
 */
public class DoubleSetPointEvaluator implements DoubleValueEvaluator
{
    private boolean inverse = false;

    public DoubleSetPointEvaluator()
    {
        super();
    }

    public DoubleSetPointEvaluator(final boolean inverse)
    {
        this.inverse = inverse;
    }

    public boolean isInverse()
    {
        return inverse;
    }

    public void setInverse(final boolean inverse)
    {
        this.inverse = inverse;
    }

    @Override
    public Long evaluate(final Long inputValue, final Supplier<Long> upper, final Supplier<Long> lower)
    {
        long upperValue = upper.get();
        long lowerValue = lower.get();

        if (lowerValue > upperValue)
        {
            long tmp = lowerValue;
            lowerValue = upperValue;
            upperValue = tmp;
        }

        long result;

        if (inputValue < lowerValue)
        {
            result = lowerValue - inputValue; // positive number
        }
        else if (inputValue > upperValue)
        {
            result = upperValue - inputValue; // negative number
        }
        else
        {
            result = 0; // 0
        }

        if (inverse)
        { result = result * (-1); }

        return result;
    }
}
