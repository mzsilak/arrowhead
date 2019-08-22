package eu.arrowhead.gams.model;

import java.util.function.Supplier;

@FunctionalInterface
public interface TargetValue extends Supplier<Long>
{
    Long get();
}
