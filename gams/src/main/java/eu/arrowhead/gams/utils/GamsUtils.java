package eu.arrowhead.gams.utils;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public class GamsUtils {

    public static void verify(final UUID uuid) {
        Objects.requireNonNull(uuid, "UUID must not be null");
    }

    public static void verify(final SensorData sensorData) {
        Objects.requireNonNull(sensorData, "SensorData must not be null");
    }

    public static void verify(final SensorDataModel sensorDataModel) {
        Objects.requireNonNull(sensorDataModel, "SensorDataModel must not be null");
    }

    public static void verifyQuerySize(Integer size) {
        Objects.requireNonNull(size, "Query size must not be null");
    }

    public static void verify(ZoneId targetZone) {
        Objects.requireNonNull(targetZone, "TargetZone must not be null");
    }

    public static void verify(ModifyGamsInstanceRequest instance) {
        Objects.requireNonNull(instance, "Request must not be null");
    }

    public static void verify(GamsInstanceModel model) {
        Objects.requireNonNull(model, "GamsInstanceModel must not be null");
    }
}
