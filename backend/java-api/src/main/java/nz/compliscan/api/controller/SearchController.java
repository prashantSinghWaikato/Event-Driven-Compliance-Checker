// src/main/java/nz/compliscan/api/controller/SearchController.java
package nz.compliscan.api.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class SearchController {

    public record SearchReq(@NotBlank String name, String country) {
    }

    public record Match(String id, String name, String list, int riskScore, String country, String lastUpdated) {
    }

    public record Resp(List<Match> matches) {
    }

    @PostMapping
    public Resp search(@RequestBody SearchReq req) {
        // TODO: replace with real search; this is a safe stub
        var now = java.time.Instant.now().toString();
        return new Resp(List.of(
                new Match("stub-1", req.name().toUpperCase() + " HOLDINGS", "OFAC", 81,
                        req.country() == null ? "" : req.country(), now),
                new Match("stub-2", req.name().toUpperCase() + " SERVICES", "PEP", 56, "AU", now)));
    }
}
