package eu.arrowhead.gams.controller;

import eu.arrowhead.gams.model.TargetValue;

import java.util.function.Supplier;

@FunctionalInterface
public interface TriggeredDoubleValueController
{
    Integer evaluate(Long inputValue, Supplier<Long> upper, Supplier<Long> lower);
}
