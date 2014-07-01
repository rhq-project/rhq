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
    DELETE_TRAIT_DATA("TRUNCATE TABLE RHQ_MEASUREMENT_DATA_TRAIT"),
    DELETE_CALL_TIME_DATA_KEY("TRUNCATE TABLE RHQ_CALLTIME_DATA_KEY"),
    DELETE_CALL_TIME_DATA_VALUE("TRUNCATE TABLE RHQ_CALLTIME_DATA_VALUE"),

    COUNT_1H_DATA("SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_1H"), COUNT_6H_DATA(
        "SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_6H"), COUNT_1D_DATA(
        "SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_1D"),

    COUNT_TRAIT_DATA("select count(*) from RHQ_MEASUREMENT_DATA_TRAIT"),
    COUNT_CALL_TIME_DATA("select count(*) from RHQ_CALLTIME_DATA_KEY k, RHQ_CALLTIME_DATA_VALUE v where k.id = v.key_id"),
    SELECT_CALL_TIME_DATA(
            "select schedule_id, call_destination, begin_time, end_time, minimum, maximum, total, \"count\"" +
            "from rhq_calltime_data_key k, rhq_calltime_data_value v where k.id = v.key_id"),
    SELECT_TRAIT_DATA(
            "select time_stamp, schedule_id, value from RHQ_MEASUREMENT_DATA_TRAIT"),

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
