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

    private ResultSetMapper<AggregateNumericMetric> resultSetMapper;

    public AggregateNumericMetricMapper() {
        this(false);
    }

    public AggregateNumericMetricMapper(boolean includeMetadata) {
        if (includeMetadata) {
            resultSetMapper = new ResultSetMapper<AggregateNumericMetric>() {
                @Override
                public List<AggregateNumericMetric> mapAll(ResultSet resultSet) {
                    List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();
                    while (!resultSet.isExhausted()) {
                        metrics.add(mapOne(resultSet));
                    }

                    return metrics;
                }

                @Override
                public AggregateNumericMetric mapOne(ResultSet resultSet) {
                    return map(resultSet.one(), resultSet.one(), resultSet.one()).get(0);
                }

                @Override
                public List<AggregateNumericMetric> map(Row... row) {
                    List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();

                    for (int i = 0; i < row.length; i += 3) {
                        AggregateNumericMetric metric = new AggregateNumericMetric();
                        metric.setScheduleId(row[i].getInt(0));
                        metric.setBucket(Bucket.fromString(row[i].getString(1)));
                        ColumnMetadata maxMetadata = new ColumnMetadata(row[i].getInt(4), row[i].getLong(5));
                        metric.setMaxColumnMetadata(maxMetadata);
                        metric.setTimestamp(row[i].getDate(1).getTime());
                        metric.setMax(row[i].getDouble(3));
                        if (row[i + 1] != null) {
                            metric.setMin(row[i + 1].getDouble(3));
                            ColumnMetadata minMetadata = new ColumnMetadata(row[i + 1].getInt(4), row[i + 1].getLong(5));
                            metric.setMinColumnMetadata(minMetadata);

                            if (row[i + 2] != null) {
                                metric.setAvg(row[i + 2].getDouble(3));
                                ColumnMetadata avgMetadata = new ColumnMetadata(row[i + 2].getInt(4), row[i + 2].getLong(5));
                                metric.setAvgColumnMetadata(avgMetadata);
                            }
                        }

                        metrics.add(metric);
                    }

                    return metrics;
                }

                @Override
                public AggregateNumericMetric map(Row row) {
                    throw new UnsupportedOperationException(
                        "Method is not supported. Only triples are accepted for mapping.");
                }
            };
        } else {
            resultSetMapper = new ResultSetMapper<AggregateNumericMetric>() {
                @Override
                public List<AggregateNumericMetric> mapAll(ResultSet resultSet) {
                    List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();
                    while (!resultSet.isExhausted()) {
                        metrics.add(mapOne(resultSet));
                    }

                    return metrics;
                }

                @Override
                public AggregateNumericMetric mapOne(ResultSet resultSet) {
                    return map(resultSet.one(), resultSet.one(), resultSet.one()).get(0);
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
                    throw new UnsupportedOperationException(
                        "Method is not supported. Only triples are accepted for mapping.");
                }
            };
        }
    }

    @Override
    public List<AggregateNumericMetric> mapAll(ResultSet resultSet) {
        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();
        for (Row row : resultSet) {
            metrics.add(new AggregateNumericMetric(row.getInt(0), Bucket.fromString(row.getString(1)),
                row.getDouble(3), row.getDouble(5), row.getDouble(4), row.getDate(2).getTime()));
        }
        return metrics;
    }

    @Override
    public AggregateNumericMetric mapOne(ResultSet resultSet) {
        return resultSetMapper.mapOne(resultSet);
    }

    @Override
    public List<AggregateNumericMetric> map(Row... rows) {
        return resultSetMapper.map(rows);
    }

    @Override
    public AggregateNumericMetric map(Row row) {
        return resultSetMapper.map(row);
    }
}