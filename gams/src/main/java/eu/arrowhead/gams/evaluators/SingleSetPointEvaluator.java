package eu.arrowhead.gams.evaluators;

import java.util.function.Supplier;

@FunctionalInterface
public interface SingleSetPointEvaluator {

    Long evaluate(Long inputValue, Supplier<Long> targetValue);
}
