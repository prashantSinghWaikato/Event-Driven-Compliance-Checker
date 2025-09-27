package nz.compliscan.api.refdata;

import nz.compliscan.api.refdata.model.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ScreeningService {
    private final OfacClient ofacClient;
    private final PepClient pepClient;
    private final RefdataProperties props;
    private final RefdataCache cache = new RefdataCache();

    // thresholds (tune as needed)
    private static final double OFAC_NAME_THRESHOLD = 0.92;
    private static final double PEP_NAME_THRESHOLD = 0.90;
    private static final double TOKEN_WEIGHT = 0.35; // blended with JW

    public ScreeningService(OfacClient ofacClient, PepClient pepClient, RefdataProperties props) {
        this.ofacClient = ofacClient;
        this.pepClient = pepClient;
        this.props = props;
        reload(); // initial load at startup
    }

    // ✅ Use property placeholder, not SpEL:
    // env var: REFDATA_REFRESH_CRON
    @Scheduled(cron = "${refdata.refresh-cron:0 30 3 * * *}")
    public void scheduledReload() {
        reload();
    }

    public synchronized void reload() {
        var ofac = ofacClient.fetchAll();
        var peps = pepClient.fetchAll();
        cache.replace(ofac, peps);
    }

    public record Match(String source, String uid, String display, double score, String extra) {
    }

    public record ScreenResult(
            String inputName,
            List<Match> ofacMatches,
            List<Match> pepMatches,
            String risk // HIGH (any OFAC), MEDIUM (PEP only), LOW (none)
    ) {
    }

    public ScreenResult screenByName(String name) {
        String norm = NameTools.normalize(name);
        var tA = NameTools.tokens(norm);

        // OFAC
        var ofac = cache.getOfac().stream()
                .map(e -> {
                    String n = NameTools.normalize(e.name());
                    double jw = NameTools.jw(norm, n);
                    double tok = NameTools.tokenOverlapScore(tA, NameTools.tokens(n));
                    double score = blend(jw, tok);
                    return new Match(e.source(), e.uid(), e.name(), score, e.program());
                })
                .filter(m -> m.score >= OFAC_NAME_THRESHOLD)
                .sorted(Comparator.comparingDouble((Match m) -> m.score).reversed())
                .limit(10)
                .toList();

        // PEP
        var pep = cache.getPeps().stream()
                .map(e -> {
                    String n = NameTools.normalize(e.name());
                    double jw = NameTools.jw(norm, n);
                    double tok = NameTools.tokenOverlapScore(tA, NameTools.tokens(n));
                    double score = blend(jw, tok);
                    String extra = (e.country().isBlank() ? "" : e.country()) +
                            (e.role().isBlank() ? "" : (extraSep(e.country()) + e.role()));
                    return new Match("PEP", e.uid(), e.name(), score, extra);
                })
                .filter(m -> m.score >= PEP_NAME_THRESHOLD)
                .sorted(Comparator.comparingDouble((Match m) -> m.score).reversed())
                .limit(10)
                .toList();

        String risk = !ofac.isEmpty() ? "HIGH" : (!pep.isEmpty() ? "MEDIUM" : "LOW");

        return new ScreenResult(name, ofac, pep, risk);
    }

    public record Stats(int ofacCount, int pepCount, long lastLoadedMs) {
    }

    public Stats stats() {
        return new Stats(cache.getOfac().size(), cache.getPeps().size(), cache.getLastLoadedEpochMs());
    }

    // helper blend
    private static double blend(double jw, double token) {
        return (jw * (1.0 - TOKEN_WEIGHT)) + (token * TOKEN_WEIGHT);
    }

    private static String extraSep(String s) {
        return (s == null || s.isBlank()) ? "" : " • ";
    }
}
