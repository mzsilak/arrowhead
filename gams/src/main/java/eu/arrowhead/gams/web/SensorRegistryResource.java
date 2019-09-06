package eu.arrowhead.gams.web;

import eu.arrowhead.gams.GamsService;
import eu.arrowhead.gams.api.model.ErrorResponse;
import eu.arrowhead.gams.api.model.Sensor;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.errors.InvalidModificationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/instance/{uuid}/sensor")
@Api(tags = {"Registries", "Sensors"})
public class SensorRegistryResource {

    private final Logger logger = LogManager.getLogger();

    private final GamsService gamsService;

    @Autowired
    public SensorRegistryResource(final GamsService gamsService) {
        this.gamsService = gamsService;
    }

    @GetMapping(value = "", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Get all Sensors")
    @ApiResponses({@ApiResponse(code = 200, response = Sensor.class, responseContainer = "Set", message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity getSensors(@PathVariable final UUID uuid) {
        ResponseEntity responseEntity;
        try {
            logger.info("Reading all sensors of instance '{}'", uuid);
            final Set<Sensor> sensors = gamsService.getSensors(uuid);
            responseEntity = ResponseEntity.ok(sensors);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }

    @GetMapping(value = "/{sensorId}", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Delete an existing GAMS instance")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 403, response = ErrorResponse.class, message = "The given GAMS instance is currently "
            + "running"), @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity getSensor(@PathVariable final UUID uuid, @PathVariable final UUID sensorId) {
        ResponseEntity responseEntity;
        try {
            logger.info("Deleting GAMS instance with uuid '{}'", uuid);
            gamsService.deleteGamsInstance(uuid);
            responseEntity = new ResponseEntity<>(HttpStatus.OK);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (InvalidModificationException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a new GAMS instance")
    @ApiResponses({@ApiResponse(code = 201, message = "CREATED"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity registerSensor(@RequestBody final ModifyGamsInstanceRequest request) {
        ResponseEntity responseEntity;
        try {
            logger.info("Processing create GAMS instances request '{}'", request);
            final UUID uuid = gamsService.createGamsInstance(request);
            final URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/" + uuid.toString())
                                                       .queryParam("timeZone", request.getCreationDate().getZone())
                                                       .build().toUri();
            responseEntity = ResponseEntity.created(uri).build();
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }


    @PutMapping(value = "/{sensorId}", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Delete an existing GAMS instance")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 403, response = ErrorResponse.class, message = "The given GAMS instance is currently "
            + "running"), @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity replaceSensor(@PathVariable final UUID uuid, @PathVariable final UUID sensorId) {
        ResponseEntity responseEntity;
        try {
            logger.info("Deleting GAMS instance with uuid '{}'", uuid);
            gamsService.deleteGamsInstance(uuid);
            responseEntity = new ResponseEntity<>(HttpStatus.OK);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (InvalidModificationException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }

    @DeleteMapping(value = "/{sensorId}", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Delete an existing GAMS instance")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 403, response = ErrorResponse.class, message = "The given GAMS instance is currently "
            + "running"), @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity deleteSensor(@PathVariable final UUID uuid, @PathVariable final UUID sensorId) {
        ResponseEntity responseEntity;
        try {
            logger.info("Deleting GAMS instance with uuid '{}'", uuid);
            gamsService.deleteGamsInstance(uuid);
            responseEntity = new ResponseEntity<>(HttpStatus.OK);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (InvalidModificationException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }
}
