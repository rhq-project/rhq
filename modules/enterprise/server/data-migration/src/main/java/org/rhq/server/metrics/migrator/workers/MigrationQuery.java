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

/**
 * @author Stefan Negrea
 *
 */
public enum MigrationQuery {
    SELECT_1H_DATA(
        "SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_1H ORDER BY schedule_id, time_stamp"), SELECT_6H_DATA(
        "SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_6H ORDER BY schedule_id, time_stamp"), SELECT_1D_DATA(
        "SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_1D ORDER BY schedule_id, time_stamp"),

    DELETE_1H_DATA("DELETE FROM RHQ_MEASUREMENT_DATA_NUM_1H"), DELETE_6H_DATA("DELETE FROM RHQ_MEASUREMENT_DATA_NUM_6H"), DELETE_1D_DATA(
        "DELETE FROM RHQ_MEASUREMENT_DATA_NUM_1D"),

    COUNT_1H_DATA("SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_1H"), COUNT_6H_DATA(
        "SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_6H"), COUNT_1D_DATA(
        "SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_1D"),

    MAX_TIMESTAMP_1H_DATA("SELECT MAX(time_stamp) FROM RHQ_MEASUREMENT_DATA_NUM_1H"), MAX_TIMESTAMP_6H_DATA(
        "SELECT MAX(time_stamp) FROM RHQ_MEASUREMENT_DATA_NUM_6H"), MAX_TIMESTAMP_1D_DATA(
        "SELECT MAX(time_stamp) FROM RHQ_MEASUREMENT_DATA_NUM_1D"),

    COUNT_RAW("SELECT COUNT(*) FROM %s"), SELECT_RAW_DATA(
        "SELECT schedule_id, time_stamp, value FROM %s ORDER BY schedule_id, time_stamp"), DELETE_RAW_ALL_DATA(
        "DELETE FROM %s"), DELETE_RAW_ENTRY("DELETE FROM %s WHERE schedule_id = ?");

    public static final int SCHEDULE_INDEX = 0;
    public static final int TIMESTAMP_INDEX = 1;
    public static final int VALUE_INDEX = 2;
    public static final int MIN_VALUE_INDEX = 3;
    public static final int MAX_VALUE_INDEX = 4;

    private String query;

    private MigrationQuery(String query) {
        this.query = query;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return query;
    }
}