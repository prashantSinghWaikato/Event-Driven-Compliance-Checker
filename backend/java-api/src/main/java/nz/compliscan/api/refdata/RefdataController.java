package nz.compliscan.api.refdata;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refdata")
public class RefdataController {
    private final ScreeningService service;

    public RefdataController(ScreeningService service) {
        this.service = service;
    }

    @PostMapping("/reload")
    public ResponseEntity<?> reload() {
        service.reload();
        return ResponseEntity.ok(service.stats());
    }

    @GetMapping("/stats")
    public ScreeningService.Stats stats() {
        return service.stats();
    }

    @GetMapping("/screen")
    public ScreeningService.ScreenResult screen(@RequestParam String name) {
        return service.screenByName(name);
    }
}
