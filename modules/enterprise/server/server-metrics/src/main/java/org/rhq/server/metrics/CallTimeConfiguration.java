package org.rhq.server.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Configuration, mainly for trait retention period.
 */
public class CallTimeConfiguration {

    private int ttl;

    /**
     * Mainly for data migration use, ensure the same UUID is
     * produced each time for a call time and destination.
     */
    private boolean idempotent;

    /**
     * Construct a new instance.
     * @param ttl TTL in days
     */
    public CallTimeConfiguration() {
        setTTLDays(30);
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

    /**
     * Returns true if multiple inserts of a call time with same schedule ID,
     * begin time, and destination should overwrite themselves. Note there is a
     * small chance two destination strings will hash to the same value, which
     * is why this defaults to false. This is mainly useful for data migration,
     * which may need to be run multiple times.
     */
    public boolean isIdempotentInsert() {
        return idempotent;
    }

    /**
     * See {@link #isIdempotentInsert()}.
     */
    public void setIdempotentInsert(boolean indepotentInsert) {
        this.idempotent = indepotentInsert;
    }

}
