package nz.compliscan.api.refdata;

import nz.compliscan.api.refdata.model.SanctionEntry;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class OfacClient {
    private final WebClient http;
    private final RefdataProperties props;

    public OfacClient(WebClient http, RefdataProperties props) {
        this.http = http;
        this.props = props;
    }

    public List<SanctionEntry> fetchAll() {
        var all = new ArrayList<SanctionEntry>();
        all.addAll(fetchCsv(props.getOfacSdnUrl(), "OFAC:SDN"));
        all.addAll(fetchCsv(props.getOfacConsolidatedUrl(), "OFAC:Consolidated"));
        return all;
    }

    private List<SanctionEntry> fetchCsv(String url, String source) {
        try {
            byte[] bytes = http.get().uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .onErrorResume(e -> Mono.empty())
                    .block();
            if (bytes == null)
                return List.of();

            try (var reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                Iterable<CSVRecord> recs = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .parse(reader);

                var out = new ArrayList<SanctionEntry>();
                for (var r : recs) {
                    // OFAC CSV headers vary. Try common ones:
                    String name = pick(r, "name", "SDN_Name", "Entity", "Individual", "Last Name");
                    if (name == null || name.isBlank())
                        continue;

                    String program = pick(r, "program", "Program", "Programs", "Remarks");
                    String type = pick(r, "sdnType", "Type", "SDN_Type", "sdn_type");
                    String uid = pick(r, "uid", "uid", "ID", "Unique ID", "sdn_uid", "entity_number");

                    out.add(new SanctionEntry(source, name.trim(), orEmpty(program), orEmpty(type), orEmpty(uid)));
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
