package eu.arrowhead.gams.controller;

import eu.arrowhead.gams.model.TargetValue;

import java.util.function.Supplier;

@FunctionalInterface
public interface TriggeredController
{
    Long evaluate(Long inputValue, Supplier<Long> targetValue);
}
