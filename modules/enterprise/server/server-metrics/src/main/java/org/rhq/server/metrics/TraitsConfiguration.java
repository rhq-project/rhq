package org.rhq.server.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Configuration, mainly for trait retention period.
 */
public class TraitsConfiguration {

    private int ttl;

    /**
     * Construct a new instance.
     * @param ttl TTL in days
     */
    public TraitsConfiguration() {
        setTTLDays(365);
    }

    /**
     * Returns TTL in days.
     */
    public int getTTLDays() {
        return ttl;
    }

    /**
     * Returns TTL in seconds.
     */
    public int getTTLSeconds() {
        return (int) TimeUnit.SECONDS.convert(ttl, TimeUnit.DAYS);
    }

    /**
     * Setter for TTL in days.
     */
    public void setTTLDays(int ttl) {
        if (ttl <= 0)
            throw new IllegalArgumentException();
        this.ttl = ttl;
    }

}
