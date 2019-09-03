package eu.arrowhead.gams.controller;

import eu.arrowhead.gams.model.TargetValue;

import java.util.function.Supplier;

@FunctionalInterface
public interface DoubleValueEvaluator
{
    Long evaluate(Long inputValue, Supplier<Long> upper, Supplier<Long> lower);
}
