package eu.arrowhead.gams;

import eu.arrowhead.gams.model.SenMLEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

@Service
@RequestMapping("/")
public class GamsResource
{

    public GamsResource()
    {
        super();
    }

    @GetMapping
    public void list() {}

    @PostMapping("event")
    public void postEvents(final Set<SenMLEvent> eventContainer) {}

    @DeleteMapping("/gams/{id}")
    public void remove(@PathVariable final String id) {}


}
