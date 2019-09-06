package eu.arrowhead.gams.utils;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

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

    public static URI uriFromPath(final String path) {
        return uriBuilderFromPath(path).build().toUri();
    }

    public static UriComponentsBuilder uriBuilderFromPath(final String path) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path(path);
    }

    public static LocalDateTime toLocalDate(ZonedDateTime creationDate) {
        return LocalDateTime.ofInstant(creationDate.toInstant(), ZoneOffset.UTC);
    }
}
