package nz.compliscan.api.refdata.model;

public record SanctionEntry(
        String source, // "OFAC:SDN" or "OFAC:Consolidated"
        String name,
        String program, // optional
        String type, // e.g. "individual", "entity" (if available)
        String uid // unique id if present
) {
}
