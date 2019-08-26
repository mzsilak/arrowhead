package eu.arrowhead.gams.controller;

import eu.arrowhead.gams.model.TargetValue;

import java.util.function.Supplier;

/**
 * A BangBang controller which returns a positive number if the target value is not yet reached,
 * returns 0 if the value is equal and returns a negative number if the target value is exceeded.
 */
public class BangBangController implements TriggeredController
{
    private boolean inverse = false;

    public BangBangController()
    {
        super();
    }

    public BangBangController(final boolean inverse)
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
    public Long evaluate(final Long inputValue, final Supplier<Long> targetValue)
    {
        long value = (targetValue.get() - inputValue);

        if (inverse)
        { value = value * (-1); }

        return value;
    }
}
