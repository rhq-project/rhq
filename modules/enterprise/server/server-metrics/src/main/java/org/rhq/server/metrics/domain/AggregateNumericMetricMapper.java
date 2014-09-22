/*
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

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public class AggregateNumericMetricMapper implements ResultSetMapper<AggregateNumericMetric> {

    @Override
    public List<AggregateNumericMetric> mapAll(ResultSet resultSet) {
        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();
        for (Row row : resultSet) {
            metrics.add(map(row));
        }
        return metrics;
    }

    @Override
    public AggregateNumericMetric mapOne(ResultSet resultSet) {
        return map(resultSet.one());
    }

    @Override
    public List<AggregateNumericMetric> map(Row... row) {
        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();


        for (int i = 0; i < row.length; i += 3) {
            AggregateNumericMetric metric = new AggregateNumericMetric();
            metric.setScheduleId(row[i].getInt(0));
            metric.setBucket(Bucket.fromString(row[i].getString(1)));
            metric.setTimestamp(row[i].getDate(2).getTime());
            metric.setAvg(row[i].getDouble(3));
            metric.setMax(row[i].getDouble(4));
            metric.setMin(row[i].getDouble(5));
            metrics.add(metric);
        }

        return metrics;
    }

    @Override
    public AggregateNumericMetric map(Row row) {
        return new AggregateNumericMetric(row.getInt(0), Bucket.fromString(row.getString(1)), row.getDouble(3),
            row.getDouble(5), row.getDouble(4), row.getDate(2).getTime());
    }
}