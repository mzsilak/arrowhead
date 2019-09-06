package eu.arrowhead.gams.web;

import eu.arrowhead.gams.utils.GamsUtils;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping(value = "/")
public class SwaggerResource {

    @GetMapping(path = {"/", "/api", "/swagger"})
    public ResponseEntity api() throws URISyntaxException {
        return ResponseEntity.status(HttpStatus.FOUND).location(GamsUtils.uriFromPath("/gams/swagger-ui.html")).build();
    }
}
