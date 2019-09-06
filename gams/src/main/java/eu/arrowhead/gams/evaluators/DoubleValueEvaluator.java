package eu.arrowhead.gams.evaluators;

import java.util.function.Supplier;

@FunctionalInterface
public interface DoubleValueEvaluator
{
    Long evaluate(Long inputValue, Supplier<Long> upper, Supplier<Long> lower);
}
