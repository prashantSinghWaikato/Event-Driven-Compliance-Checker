package nz.compliscan.api.refdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "refdata")
public class RefdataProperties {

    /**
     * Direct CSV endpoints (defaults provided).
     * You can override via env:
     * REFDATA_OFAC_SDN_URL
     * REFDATA_OFAC_CONSOLIDATED_URL
     * REFDATA_PEP_CSV_URL
     * REFDATA_REFRESH_CRON
     */
    private String ofacSdnUrl = "https://www.treasury.gov/ofac/downloads/sdn.csv";

    private String ofacConsolidatedUrl = "https://www.treasury.gov/ofac/downloads/consolidated/consolidated.csv";

    private String pepCsvUrl = "https://data.opensanctions.org/datasets/peps/latest/peps.csv";

    // Default: daily at 03:30
    private String refreshCron = "0 30 3 * * *";

    public String getOfacSdnUrl() {
        return ofacSdnUrl;
    }

    public void setOfacSdnUrl(String ofacSdnUrl) {
        this.ofacSdnUrl = ofacSdnUrl;
    }

    public String getOfacConsolidatedUrl() {
        return ofacConsolidatedUrl;
    }

    public void setOfacConsolidatedUrl(String ofacConsolidatedUrl) {
        this.ofacConsolidatedUrl = ofacConsolidatedUrl;
    }

    public String getPepCsvUrl() {
        return pepCsvUrl;
    }

    public void setPepCsvUrl(String pepCsvUrl) {
        this.pepCsvUrl = pepCsvUrl;
    }

    public String getRefreshCron() {
        return refreshCron;
    }

    public void setRefreshCron(String refreshCron) {
        this.refreshCron = refreshCron;
    }
}
