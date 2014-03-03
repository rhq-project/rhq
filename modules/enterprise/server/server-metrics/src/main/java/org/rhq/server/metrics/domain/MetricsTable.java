/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics.domain;

import org.joda.time.Duration;


public enum MetricsTable {

    METRICS_CACHE("metrics_cache", -1),
    RAW("raw_metrics", Duration.standardDays(7).toStandardSeconds().getSeconds()),
    ONE_HOUR("one_hour_metrics", Duration.standardDays(14).toStandardSeconds().getSeconds()),
    SIX_HOUR("six_hour_metrics", Duration.standardDays(31).toStandardSeconds().getSeconds()),
    TWENTY_FOUR_HOUR("twenty_four_hour_metrics", Duration.standardDays(365).toStandardSeconds().getSeconds());


    private final String tableName;
    private final int ttl;

    private MetricsTable(String tableName, int ttl) {
        this.tableName = tableName;
        this.ttl = ttl;
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

    @Override
    public String toString() {
        return this.tableName;
    }
}