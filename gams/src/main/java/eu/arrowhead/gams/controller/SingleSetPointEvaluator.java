package eu.arrowhead.gams.controller;

import java.util.function.Supplier;

@FunctionalInterface
public interface SingleSetPointEvaluator {

    Long evaluate(Long inputValue, Supplier<Long> targetValue);
}
