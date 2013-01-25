/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public class AggregateMetricMapper implements ResultSetMapper<AggregatedNumericMetric> {

    private ResultSetMapper<AggregatedNumericMetric> resultSetMapper;

    public AggregateMetricMapper() {
        this(false);
    }

    public AggregateMetricMapper(boolean includeMetadata) {
        if (includeMetadata) {
            resultSetMapper = new ResultSetMapper<AggregatedNumericMetric>() {
                @Override
                public List<AggregatedNumericMetric> mapAll(ResultSet resultSet) {
                    List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();
                    while (!resultSet.isExhausted()) {
                        metrics.add(mapOne(resultSet));
                    }

                    return metrics;
                }

                @Override
                public AggregatedNumericMetric mapOne(ResultSet resultSet) {
                    return map(resultSet.fetchOne(), resultSet.fetchOne(), resultSet.fetchOne()).get(0);
                }

                @Override
                public List<AggregatedNumericMetric> map(Row... row) {
                    List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();

                    for (int i = 0; i < row.length; i += 3) {
                        AggregatedNumericMetric metric = new AggregatedNumericMetric();
                        metric.setScheduleId(row[i].getInt(0));
                        metric.setTimestamp(row[i].getDate(1).getTime());
                        metric.setMax(row[i].getDouble(3));
                        metric.setMin(row[i + 1].getDouble(3));
                        metric.setAvg(row[i + 2].getDouble(3));

                        ColumnMetadata maxMetadata = new ColumnMetadata(row[i].getInt(4), row[i].getLong(5));
                        ColumnMetadata minMetadata = new ColumnMetadata(row[i + 1].getInt(4), row[i + 1].getLong(5));
                        ColumnMetadata avgMetadata = new ColumnMetadata(row[i + 2].getInt(4), row[i + 2].getLong(5));

                        metric.setAvgColumnMetadata(avgMetadata);
                        metric.setMaxColumnMetadata(maxMetadata);
                        metric.setMinColumnMetadata(minMetadata);

                        metrics.add(metric);
                    }

                    return metrics;
                }

                @Override
                public AggregatedNumericMetric map(Row row) {
                    throw new UnsupportedOperationException(
                        "Method is not supported. Only triples are accepted for mapping.");
                }
            };
        } else {
            resultSetMapper = new ResultSetMapper<AggregatedNumericMetric>() {
                @Override
                public List<AggregatedNumericMetric> mapAll(ResultSet resultSet) {
                    List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();
                    while (!resultSet.isExhausted()) {
                        metrics.add(mapOne(resultSet));
                    }

                    return metrics;
                }

                @Override
                public AggregatedNumericMetric mapOne(ResultSet resultSet) {
                    return map(resultSet.fetchOne(), resultSet.fetchOne(), resultSet.fetchOne()).get(0);
                }

                @Override
                public List<AggregatedNumericMetric> map(Row... row) {
                    List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();

                    for (int i = 0; i < row.length; i += 3) {
                        AggregatedNumericMetric metric = new AggregatedNumericMetric();
                        metric.setScheduleId(row[i].getInt(0));
                        metric.setTimestamp(row[i].getDate(1).getTime());
                        metric.setMax(row[i].getDouble(3));
                        metric.setMin(row[i + 1].getDouble(3));
                        metric.setAvg(row[i + 2].getDouble(3));

                        metrics.add(metric);
                    }

                    return metrics;
                }

                @Override
                public AggregatedNumericMetric map(Row row) {
                    throw new UnsupportedOperationException(
                        "Method is not supported. Only triples are accepted for mapping.");
                }
            };
        }
    }

    @Override
    public List<AggregatedNumericMetric> mapAll(ResultSet resultSet) {
        return resultSetMapper.mapAll(resultSet);
    }

    @Override
    public AggregatedNumericMetric mapOne(ResultSet resultSet) {
        return resultSetMapper.mapOne(resultSet);
    }

    @Override
    public List<AggregatedNumericMetric> map(Row... rows) {
        return resultSetMapper.map(rows);
    }

    @Override
    public AggregatedNumericMetric map(Row row) {
        return resultSetMapper.map(row);
    }
}
