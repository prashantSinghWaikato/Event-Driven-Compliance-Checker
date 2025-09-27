package nz.compliscan.api.refdata.model;

public record PepEntry(
        String name,
        String country, // if present
        String role, // position/office if present
        String source, // dataset source id
        String uid) {
}
