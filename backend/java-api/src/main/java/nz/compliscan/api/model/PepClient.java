package nz.compliscan.api.refdata;

import nz.compliscan.api.refdata.model.PepEntry;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class PepClient {
    private final WebClient http;
    private final RefdataProperties props;

    public PepClient(WebClient http, RefdataProperties props) {
        this.http = http;
        this.props = props;
    }

    public List<PepEntry> fetchAll() {
        try {
            byte[] bytes = http.get().uri(props.getPepCsvUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .onErrorResume(e -> Mono.empty())
                    .block();
            if (bytes == null)
                return List.of();

            try (var reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                Iterable<CSVRecord> recs = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                var out = new ArrayList<PepEntry>();
                for (var r : recs) {
                    String name = pick(r, "name", "full_name", "caption", "person.name");
                    if (name == null || name.isBlank())
                        continue;
                    String country = pick(r, "country", "country_name", "countries");
                    String role = pick(r, "position", "role", "function");
                    String source = pick(r, "dataset", "source", "publisher");
                    String uid = pick(r, "id", "entity_id", "os_id");
                    out.add(new PepEntry(name.trim(), orEmpty(country), orEmpty(role), orEmpty(source), orEmpty(uid)));
                }
                return out;
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String pick(CSVRecord r, String... keys) {
        for (var k : keys) {
            if (r.isMapped(k)) {
                var v = r.get(k);
                if (v != null && !v.isBlank())
                    return v;
            }
        }
        return null;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
