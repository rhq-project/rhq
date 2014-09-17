/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.server.metrics.migrator.workers;

import org.joda.time.Duration;

import org.rhq.server.metrics.domain.Bucket;

/**
 * @author Stefan Negrea
 *
 */
public enum MigrationTable {
    RAW("raw_metrics", Duration.standardDays(7).toStandardSeconds().getSeconds(), null, Bucket.ONE_HOUR),
    ONE_HOUR("one_hour_metrics", Duration.standardDays(14).toStandardSeconds().getSeconds(), Bucket.ONE_HOUR, Bucket.SIX_HOUR),
    SIX_HOUR("six_hour_metrics", Duration.standardDays(31).toStandardSeconds().getSeconds(), Bucket.SIX_HOUR, Bucket.TWENTY_FOUR_HOUR),
    TWENTY_FOUR_HOUR("twenty_four_hour_metrics", Duration.standardDays(365).toStandardSeconds().getSeconds(), Bucket.TWENTY_FOUR_HOUR,null);

    private final String tableName;
    private final int ttl;
    private final Bucket aggregationBucket;
    private final Bucket migrationBucket;

    private MigrationTable(String tableName, int ttl, Bucket migrationBucket, Bucket aggregationBucket) {
        this.tableName = tableName;
        this.ttl = ttl;
        this.aggregationBucket = aggregationBucket;
        this.migrationBucket = migrationBucket;
    }

    public String getTableName() {
        return this.tableName;
    }

    public int getTTL() {
        return this.ttl;
    }

    public long getTTLinMilliseconds() {
        return this.ttl * 1000l;
    }

    public Bucket getAggregationBucket() {
        return this.aggregationBucket;
    }

    public Bucket getMigrationBucket() {
        return this.migrationBucket;
    }

    @Override
    public String toString() {
        return this.tableName;
    }
}