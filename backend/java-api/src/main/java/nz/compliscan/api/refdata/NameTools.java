package nz.compliscan.api.refdata;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import java.text.Normalizer;
import java.util.*;

public class NameTools {
    private static final JaroWinklerDistance JW = new JaroWinklerDistance();

    public static String normalize(String s) {
        if (s == null)
            return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        x = x.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return x;
    }

    public static double jw(String a, String b) {
        try {
            Double d = JW.apply(a, b);
            return d == null ? 0 : d;
        } catch (Exception e) {
            return 0;
        }
    }

    public static Set<String> tokens(String normalized) {
        if (normalized.isBlank())
            return Set.of();
        return new LinkedHashSet<>(Arrays.asList(normalized.split("\\s+")));
    }

    public static double tokenOverlapScore(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty())
            return 0;
        int inter = 0;
        for (String t : a)
            if (b.contains(t))
                inter++;
        double jacc = inter / (double) (a.size() + b.size() - inter);
        return jacc;
    }
}
