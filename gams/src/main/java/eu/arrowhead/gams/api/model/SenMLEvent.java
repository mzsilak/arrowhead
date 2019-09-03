package eu.arrowhead.gams.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class SenMLEvent implements Comparable<SenMLEvent>
{
    @SerializedName("bn")
    private String baseName;
    @SerializedName("bt")
    private Number baseTime;
    @SerializedName("bu")
    private String baseUnit;
    @SerializedName("bv")
    private Number baseValue;
    @SerializedName("bver")
    private Number baseVersion;
    @SerializedName("n")
    private String name;
    @SerializedName("u")
    private String unit;
    @SerializedName("v")
    private Number value;
    @SerializedName("vs")
    private String valueString;
    @SerializedName("vb")
    private Boolean valueBoolean;
    @SerializedName("vd")
    private String valueData;
    @SerializedName("s")
    private Number sum;
    @SerializedName("t")
    private Number time;
    @SerializedName("ut")
    private Number updateTime;

    public SenMLEvent()
    {
        super();
    }

    public String getBaseName()
    {
        return baseName;
    }

    public void setBaseName(final String baseName)
    {
        this.baseName = baseName;
    }

    public Number getBaseTime()
    {
        return baseTime;
    }

    public void setBaseTime(final Number baseTime)
    {
        this.baseTime = baseTime;
    }

    public String getBaseUnit()
    {
        return baseUnit;
    }

    public void setBaseUnit(final String baseUnit)
    {
        this.baseUnit = baseUnit;
    }

    public Number getBaseValue()
    {
        return baseValue;
    }

    public void setBaseValue(final Number baseValue)
    {
        this.baseValue = baseValue;
    }

    public Number getBaseVersion()
    {
        return baseVersion;
    }

    public void setBaseVersion(final Number baseVersion)
    {
        this.baseVersion = baseVersion;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(final String unit)
    {
        this.unit = unit;
    }

    public Number getValue()
    {
        return value;
    }

    public void setValue(final Number value)
    {
        this.value = value;
    }

    public String getValueString()
    {
        return valueString;
    }

    public void setValueString(final String valueString)
    {
        this.valueString = valueString;
    }

    public Boolean getValueBoolean()
    {
        return valueBoolean;
    }

    public void setValueBoolean(final Boolean valueBoolean)
    {
        this.valueBoolean = valueBoolean;
    }

    public String getValueData()
    {
        return valueData;
    }

    public void setValueData(final String valueData)
    {
        this.valueData = valueData;
    }

    public Number getSum()
    {
        return sum;
    }

    public void setSum(final Number sum)
    {
        this.sum = sum;
    }

    public Number getTime()
    {
        return time;
    }

    public void setTime(final Number time)
    {
        this.time = time;
    }

    public Number getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(final Number updateTime)
    {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final SenMLEvent that = (SenMLEvent) o;
        return Objects.equals(baseName, that.baseName) &&
                Objects.equals(baseTime, that.baseTime) &&
                Objects.equals(baseUnit, that.baseUnit) &&
                Objects.equals(baseValue, that.baseValue) &&
                Objects.equals(baseVersion, that.baseVersion) &&
                Objects.equals(name, that.name) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(value, that.value) &&
                Objects.equals(valueString, that.valueString) &&
                Objects.equals(valueBoolean, that.valueBoolean) &&
                Objects.equals(valueData, that.valueData) &&
                Objects.equals(sum, that.sum) &&
                Objects.equals(time, that.time) &&
                Objects.equals(updateTime, that.updateTime);
    }

    @Override
    public int hashCode()
    {
        return Objects
                .hash(baseName, baseTime, baseUnit, baseValue, baseVersion, name, unit, value, valueString,
                      valueBoolean,
                      valueData, sum, time, updateTime);
    }

    @Override
    public int compareTo(final SenMLEvent other)
    {
        if (this.baseTime != null && other.baseTime != null)
        {
            return Long.compare(this.baseTime.longValue(), other.baseTime.longValue());
        }
        return 0;
    }
}
