package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.api.model.SenMLEvent;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Service
@RequestMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE, produces =
    MediaType.APPLICATION_JSON_VALUE)
public class GamsEventsResource {

    private final Logger logger = LogManager.getLogger();
    private final GamsService gamsService;

    @Autowired
    public GamsEventsResource(final GamsService gamsService) {
        this.gamsService = gamsService;
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Set<SensorData>> list(@PathVariable final UUID uuid,
                                                @RequestParam(defaultValue = "1") final Long size) {
        ResponseEntity<Set<SensorData>> responseEntity;
        try {
            logger.info("Searching for last {} event(s) for instance '{}'", size, uuid);
            final Set<SensorData> dataSet = gamsService.query(uuid, size);
            responseEntity = new ResponseEntity<>(dataSet, HttpStatus.OK);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return responseEntity;
    }

    @PostMapping("/senml/{uuid}")
    public void postEvents(@PathVariable final UUID uuid, final Set<SenMLEvent> eventContainer) {
    }

    @PostMapping("/{uuid}")
    public ResponseEntity<Void> postEvents(@PathVariable final UUID uuid, final SensorData data) {
        ResponseEntity<Void> responseEntity;
        try {
            logger.info("Dispatching event for instance '{}': {}", uuid, data);
            gamsService.dispatch(uuid, data);
            responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.getMessage());
            responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return responseEntity;

    }
}
