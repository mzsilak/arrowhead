package eu.arrowhead.gams;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/")
public class SwaggerResource {

    private <T> ResponseEntity<T> createResponseWithLocation(final URI location) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping(path = {"/", "/api", "/swagger"})
    public ResponseEntity<Void> api() throws URISyntaxException {
        return createResponseWithLocation(new URI("/gams/swagger-ui.html"));
    }
}
