/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.enterprise.gui.common.scheduling.supporting;

public enum TimeUnits {
    Seconds(1000), Minutes(Seconds.getMillis() * 60), Hours(Minutes.getMillis() * 60), Days(Hours.getMillis() * 24);

    private final long millis;

    TimeUnits(long millis) {
        this.millis = millis;
    }

    public long getMillis() {
        return millis;
    }
}
