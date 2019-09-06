package eu.arrowhead.gams.web;

import eu.arrowhead.gams.GamsEventsService;
import eu.arrowhead.gams.api.model.ErrorResponse;
import eu.arrowhead.gams.api.model.SenMLEvent;
import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.api.model.request.SensorDataEventRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE, produces =
    MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Sensors"})
public class GamsEventsResource {

    private final Logger logger = LogManager.getLogger();
    private final GamsEventsService gamsEventsService;

    @Autowired
    public GamsEventsResource(final GamsEventsService gamsEventsService) {
        this.gamsEventsService = gamsEventsService;
    }

    @GetMapping(value = "/{uuid}", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Returns the latest SensorEvents for a specific gams instance")
    @ApiResponses({@ApiResponse(code = 200, response = SensorData.class, responseContainer = "Set", message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity list(@PathVariable final UUID uuid,
                               @RequestParam(defaultValue = "1", required = false) final Integer size,
                               @RequestParam(defaultValue = "UTC", required = false) final ZoneId timeZone) {
        ResponseEntity responseEntity;
        try {
            logger.info("Searching for last {} event(s) for instance '{}'", size, uuid);
            final Set<SensorData> dataSet = gamsEventsService.query(uuid, size, timeZone);
            responseEntity = new ResponseEntity<>(dataSet, HttpStatus.OK);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }

    @PostMapping("/{uuid}")
    @ApiOperation(value = "Posts a new SensorEvent to the server")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    @Deprecated // Register sensors during the setup
    public ResponseEntity postEvents(@PathVariable final UUID uuid, @RequestBody final SensorDataEventRequest request) {
        ResponseEntity responseEntity;
        try {
            logger.info("Dispatching event for instance '{}': {}", uuid, request);
            gamsEventsService.dispatch(uuid, request.toModel());
            responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }

        return responseEntity;

    }
}
