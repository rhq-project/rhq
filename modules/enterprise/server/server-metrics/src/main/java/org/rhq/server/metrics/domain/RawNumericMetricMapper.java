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

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public class RawNumericMetricMapper implements ResultSetMapper<RawNumericMetric> {

    @Override
    public List<RawNumericMetric> mapAll(ResultSet resultSet) {
        List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
        for (Row row : resultSet) {
            metrics.add(this.map(row));
        }

        return metrics;
    }

    @Override
    public RawNumericMetric mapOne(ResultSet resultSet) {
        if (resultSet.isExhausted()) {
            return null;
        }
        return this.map(resultSet.one());
    }

    @Override
    public List<RawNumericMetric> map(Row... row) {
        List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
        for (Row singleRow : row) {
            metrics.add(this.map(singleRow));
        }

        return metrics;
    }

    @Override
    public RawNumericMetric map(Row row) {
        return new RawNumericMetric(row.getInt(0), row.getDate(1).getTime(), row.getDouble(2));
    }
}
