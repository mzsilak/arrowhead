package eu.arrowhead.gams.controller;

import eu.arrowhead.gams.model.TargetValue;

public interface TriggeredController<INPUT extends Number, OUTPUT>
{
    OUTPUT evaluate(INPUT inputValue, TargetValue targetValue);
}
