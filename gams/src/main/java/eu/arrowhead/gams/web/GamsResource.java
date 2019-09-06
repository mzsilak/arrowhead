package eu.arrowhead.gams.web;

import eu.arrowhead.gams.GamsService;
import eu.arrowhead.gams.api.model.ErrorResponse;
import eu.arrowhead.gams.api.model.GamsInstance;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceStateRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.errors.InvalidModificationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/instance", consumes = MediaType.APPLICATION_JSON_VALUE, produces =
    MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Gams"})
public class GamsResource {

    private final Logger logger = LogManager.getLogger();
    private final GamsService gamsService;

    @Autowired
    public GamsResource(final GamsService gamsService) {
        this.gamsService = gamsService;
    }

    @GetMapping(value = "", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Return a all GAMS instances. Optionally filtered by name")
    @ApiResponses({@ApiResponse(code = 200, response = GamsInstance.class, responseContainer = "Set", message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity list(@RequestParam(value = "name", required = false) final String name) {
        ResponseEntity responseEntity;
        try {
            final Set<GamsInstance> set;
            if (StringUtils.hasText(name)) {
                logger.info("Searching for  GAMS instances with name like '{}'", name);
                set = gamsService.searchGamsInstanceByName(name);
            } else {
                logger.info("Listing all GAMS instances");
                set = gamsService.getGamsInstances();
            }

            responseEntity = new ResponseEntity<>(set, HttpStatus.OK);
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;
    }

    @GetMapping(value = "/{uuid}", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Return a specific GAMS instance")
    @ApiResponses({@ApiResponse(code = 200, response = GamsInstance.class, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity readGamsInstance(@PathVariable final UUID uuid) {
        ResponseEntity responseEntity;
        try {
            logger.info("Reading GAMS instances with UUID '{}'", uuid);
            final GamsInstance instance = gamsService.getGamsInstance(uuid);
            responseEntity = new ResponseEntity<>(instance, HttpStatus.OK);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
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
    public ResponseEntity createGamsInstance(@RequestBody final ModifyGamsInstanceRequest request) {
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

    @PutMapping("/{uuid}")
    @ApiOperation(value = "Update an existing GAMS instance")
    @ApiResponses({@ApiResponse(code = 201, message = "UPDATED"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity updateGamsInstance(@PathVariable final UUID uuid,
                                             @RequestBody final ModifyGamsInstanceRequest request) {
        ResponseEntity responseEntity;
        try {
            logger.info("Updating GAMS instance '{}' with {}", uuid, request);
            gamsService.updateGamsInstance(uuid, request);
            final URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/" + uuid.toString())
                                                       .queryParam("timeZone", request.getCreationDate().getZone())
                                                       .build().toUri();
            responseEntity = ResponseEntity.created(uri).build();
        } catch (InstanceNotFoundException | InvalidModificationException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;

    }

    @PutMapping("/{uuid}/state")
    @ApiOperation(value = "Update the state of an existing GAMS instance")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity updateState(@PathVariable final UUID uuid,
                                      @RequestBody final ModifyGamsInstanceStateRequest request) {
        ResponseEntity responseEntity;
        try {
            logger.info("Updating GAMS instance '{}' - new state: {}", uuid, request);
            gamsService.setGamsInstanceState(uuid, request.getNewState());
            responseEntity = ResponseEntity.ok().build();
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = ResponseEntity.badRequest().body(ErrorResponse.from(e));
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(e));
        }
        return responseEntity;

    }

    @DeleteMapping(value = "/{uuid}", consumes = MediaType.ALL_VALUE)
    @ApiOperation(value = "Delete an existing GAMS instance")
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, response = ErrorResponse.class, message = "The specified instance does not exist"),
        @ApiResponse(code = 403, response = ErrorResponse.class, message = "The given GAMS instance is currently "
            + "running"), @ApiResponse(code = 500, response = ErrorResponse.class, message = "Internal Error")})
    public ResponseEntity deleteGamsInstance(@PathVariable final UUID uuid) {
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
