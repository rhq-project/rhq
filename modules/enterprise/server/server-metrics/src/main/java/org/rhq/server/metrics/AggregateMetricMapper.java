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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public class AggregateMetricMapper implements ResultSetMapper<AggregatedNumericMetric> {

    @Override
    public AggregatedNumericMetric map(ResultSet resultSet) throws SQLException {
        AggregatedNumericMetric metric = new AggregatedNumericMetric();
        metric.setScheduleId(resultSet.getInt(1));
        metric.setTimestamp(resultSet.getDate(2).getTime());
        metric.setMax(resultSet.getDouble(4));

        resultSet.next();
        metric.setMin(resultSet.getDouble(4));

        resultSet.next();
        metric.setAvg(resultSet.getDouble(4));

        return metric;
    }

    @Override
    public AggregatedNumericMetric map(Row... row) {
        AggregatedNumericMetric metric = new AggregatedNumericMetric();
        metric.setScheduleId(row[0].getInt(0));
        metric.setTimestamp(row[0].getDate(1).getTime());
        metric.setMax(row[0].getDouble(3));
        metric.setMin(row[1].getDouble(3));
        metric.setAvg(row[2].getDouble(3));

        return metric;
    }
}
