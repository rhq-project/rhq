/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
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
package org.rhq.enterprise.server.measurement.util;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is purposefully not TimeZone sensitive. It makes it easier for it to deal with clusters with servers in different
 * timezones, but it means that we can't rely on table maintenance in the middle of the night. That's ok, we're going to
 * do this like the rest of the maintenance - on the hour. It does mean that even with 12 hour tables we may have data
 * in one from 10 am to 10 pm local instead of 12 to 12.
 * <p/>
 * Due to the leap seconds, it's possible some tables will technically span more than MILLISECONDS_PER_TABLE of real
 * time. This is an irrelevant quirk.
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class MeasurementDataManagerUtility {
    private static final Log LOG = LogFactory.getLog(MeasurementDataManagerUtility.class);

    public static final int STORED_DAYS = 7;
    public static final int TABLES_PER_DAY = 2;
    public static final int BUFFER_TABLES = 1;
    public static final int LIVE_TABLES = STORED_DAYS * TABLES_PER_DAY;
    public static final int TABLE_COUNT = LIVE_TABLES + BUFFER_TABLES;

    private static final String TABLE_PREFIX = "RHQ_MEAS_DATA_NUM_R";

    static final String TABLE_DATA_1H = "RHQ_MEASUREMENT_DATA_NUM_1H";
    static final String TABLE_DATA_6H = "RHQ_MEASUREMENT_DATA_NUM_6H";
    static final String TABLE_DATA_1D = "RHQ_MEASUREMENT_DATA_NUM_1D";

    private static final int MAX_IN_CLAUSE_PARAMS = 1000;

    private static NumberFormat nf = new DecimalFormat("00");

    private static final long MILLISECONDS_PER_DAY = 1000L * 60 * 60 * 24;
    public static final long MILLISECONDS_PER_TABLE = MILLISECONDS_PER_DAY / TABLES_PER_DAY;

    public static final long RAW_PURGE = STORED_DAYS * MILLISECONDS_PER_DAY;

    /** For methods taking a number of data points, the default if the passed value is invalid (<=0). */
    public static final int DEFAULT_NUM_DATA_POINTS = 60;

    private DataSource datasource;

    private static long purge1h;
    private static long purge6h;
    private static long purge1d;
    private static long purgeRt;
    private static long purgeAlert;

    static {
        loadPurgeDefaults();
    }

    public static MeasurementDataManagerUtility getInstance(DataSource dataSource) {
        MeasurementDataManagerUtility util = new MeasurementDataManagerUtility();
        util.datasource = dataSource;
        return util;
    }

    /**
     * Get the server purge configuration, loaded on startup.
     */
    private static void loadPurgeDefaults() {
        LOG.debug("Loading default purge intervals");

        try {
            Properties conf = LookupUtil.getSystemManager().getSystemConfiguration(
                LookupUtil.getSubjectManager().getOverlord());
            purge1h = Long.parseLong(conf.getProperty(RHQConstants.DataPurge1Hour));
            purge6h = Long.parseLong(conf.getProperty(RHQConstants.DataPurge6Hour));
            purge1d = Long.parseLong(conf.getProperty(RHQConstants.DataPurge1Day));
            purgeRt = Long.parseLong(conf.getProperty(RHQConstants.RtDataPurge));
            purgeAlert = Long.parseLong(conf.getProperty(RHQConstants.AlertPurge));
        } catch (Exception e) {
            // Shouldn't happen unless manual edit of config table
            //throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataAggregatesForContext(long beginTime,
        long endTime, EntityContext context, int definitionId, int numDataPoints) throws MeasurementNotFoundException {

        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection connection = null;

        List<List<MeasurementDataNumericHighLowComposite>> data = new ArrayList<List<MeasurementDataNumericHighLowComposite>>();

        try {
            String conditions = getQueryConditionsByDefinitionAndContext(definitionId, context);
            Object[] bindParams = getBindParamsByDefinitionAndContext(definitionId, context);

            connection = datasource.getConnection();
            ps = getFullQuery("getMeasurementDataAggregatesForContext", connection, beginTime, endTime, numDataPoints,
                conditions, bindParams);
            rs = ps.executeQuery();

            List<MeasurementDataNumericHighLowComposite> compositeList = new ArrayList<MeasurementDataNumericHighLowComposite>();
            while (rs.next()) {
                MeasurementDataNumericHighLowComposite next = fillHighLowCompositeFromResultSet(rs);
                compositeList.add(next);
            }

            data.add(compositeList);
            return Collections.singletonList(compositeList);
        } catch (SQLException e) {
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(connection, ps, rs);
        }
    }

    public MeasurementAggregate getAggregateByScheduleId(long beginTime, long endTime, long scheduleId)
        throws MeasurementNotFoundException {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = datasource.getConnection();
            String condition = "         AND d.schedule_id = ? \n";

            ps = getFullQuery("getAggregateByScheduleId", connection, beginTime, endTime, 1, condition, scheduleId);
            rs = ps.executeQuery();
            if (rs.next()) {
                MeasurementAggregate measurementAggregate = fillAggregateFromResultSet(rs);
                return measurementAggregate;
            }

            throw new MeasurementNotFoundException("Data not found");
        } catch (SQLException e) {
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(connection, ps, rs);
        }
    }

    public MeasurementAggregate getAggregateByDefinitionAndContext(long beginTime, long endTime, int definitionId,
        EntityContext context) throws MeasurementNotFoundException {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String conditions = getQueryConditionsByDefinitionAndContext(definitionId, context);
            Object[] bindParams = getBindParamsByDefinitionAndContext(definitionId, context);

            connection = datasource.getConnection();
            ps = getFullQuery("getAggregateByContext", connection, beginTime, endTime, 1, conditions, bindParams);
            rs = ps.executeQuery();
            if (rs.next()) {
                MeasurementAggregate measurementAggregate = fillAggregateFromResultSet(rs);
                return measurementAggregate;
            }

            throw new MeasurementNotFoundException("Data not found");
        } catch (SQLException e) {
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(connection, ps, rs);
        }
    }

    private String getQueryConditionsByDefinitionAndContext(int definitionId, EntityContext context) {
        String scheduleSubQuery = null;

        if (context.type == EntityContext.Type.Resource) {
            scheduleSubQuery = "" //
                + "SELECT innerSchedule.id \n" //
                + "  FROM rhq_measurement_sched innerSchedule \n" //
                + " WHERE innerSchedule.definition = ? \n" //
                + "   AND innerSchedule.resource_id = ? \n";

        } else if (context.type == EntityContext.Type.ResourceGroup) {
            scheduleSubQuery = "" //
                + "SELECT innerSchedule.id \n" //
                + "  FROM rhq_measurement_sched innerSchedule \n" //
                + "  JOIN rhq_resource_group_res_exp_map groupMap \n" //
                + "       ON innerSchedule.resource_id = groupMap.resource_id \n" //
                + " WHERE innerSchedule.definition = ? \n" //
                + "   AND groupMap.resource_group_id = ? \n";

        } else if (context.type == EntityContext.Type.AutoGroup) {
            scheduleSubQuery = "" //
                + "SELECT innerSchedule.id \n" //
                + "  FROM rhq_measurement_sched innerSchedule \n" //
                + "  JOIN rhq_resource innerRes \n"//
                + "       ON innerSchedule.resource_id = innerRes.id \n"//
                + " WHERE innerSchedule.definition = ? \n" //
                + "   AND innerRes.parent_resource_id = ? \n"//
                + "   AND innerRes.resource_type_id = ? \n";

        }

        if (scheduleSubQuery != null) {
            return "         AND d.schedule_id IN ( " + scheduleSubQuery + " ) \n";
        } else {
            return ""; // no condition filter for unknown entity context
        }
    }

    private Object[] getBindParamsByDefinitionAndContext(int definitionId, EntityContext context) {
        Object[] bindParams = null;

        if (context.type == EntityContext.Type.Resource) {
            bindParams = new Object[] { definitionId, context.getResourceId() };
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            bindParams = new Object[] { definitionId, context.getGroupId() };
        } else if (context.type == EntityContext.Type.AutoGroup) {
            bindParams = new Object[] { definitionId, context.getParentResourceId(), context.getResourceTypeId() };
        }

        if (bindParams != null) {
            return bindParams;
        } else {
            return new Object[0];
        }
    }

    private MeasurementDataNumericHighLowComposite fillHighLowCompositeFromResultSet(ResultSet rs) throws SQLException {
        long timestamp = rs.getLong(1);
        double value = getDoubleOrNanFromResultSet(rs, 2);
        double peak = getDoubleOrNanFromResultSet(rs, 3);
        double low = getDoubleOrNanFromResultSet(rs, 4);

        MeasurementDataNumericHighLowComposite highLowComposite = new MeasurementDataNumericHighLowComposite(timestamp,
            value, peak, low);
        return highLowComposite;
    }

    private MeasurementAggregate fillAggregateFromResultSet(ResultSet rs) throws SQLException {
        Double avg = getDoubleOrNanFromResultSet(rs, 2);
        Double max = getDoubleOrNanFromResultSet(rs, 3);
        Double min = getDoubleOrNanFromResultSet(rs, 4);

        MeasurementAggregate measurementAggregate = new MeasurementAggregate(min, avg, max);
        return measurementAggregate;
    }

    private static double getDoubleOrNanFromResultSet(ResultSet rs, int index) throws SQLException {
        double value = rs.getDouble(index);
        if (rs.wasNull()) {
            value = Double.NaN;
        }
        return value;
    }

    public MeasurementDataNumeric getLatestValueForSchedule(int scheduleId) {
        long now = System.currentTimeMillis();
        int index = getTableIndex(now);

        do {
            Connection connection = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                connection = datasource.getConnection();
                String table = TABLE_PREFIX + nf.format(index);
                String query = "SELECT d.time_stamp, d.value \n" + "FROM " + table + " d \n"
                    + "WHERE d.schedule_id = ? \n" + "AND d.time_stamp = ( SELECT MAX(dd.time_stamp) \n" + "FROM "
                    + table + " dd \n" + "WHERE dd.schedule_id = ? )";
                ps = connection.prepareStatement(query);
                ps.setInt(1, scheduleId);
                ps.setInt(2, scheduleId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return new MeasurementDataNumeric(new MeasurementDataPK(rs.getLong(1), scheduleId), rs.getDouble(2));
                }
            } catch (SQLException e) {
                throw new MeasurementNotFoundException(e);
            } finally {
                JDBCUtil.safeClose(connection, ps, rs);
            }

            index = index - 1;
            if (index < 0) {
                index = LIVE_TABLES;
            }
        } while (!(TABLE_PREFIX + nf.format(index)).equals(getDeadTable(now)));

        return null;
    }

    private PreparedStatement getFullQuery(String methodName, Connection connection, long beginTime, long endTime,
        int numDataPoints, String conditions, Object... bindParameters) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getFullQuery for " + methodName);
        }

        // ensure valid number of data points
        numDataPoints = (numDataPoints <= 0) ? DEFAULT_NUM_DATA_POINTS : numDataPoints;

        long interval = (endTime - beginTime) / numDataPoints;

        String valuesClause;
        if (isRawTimePeriod(beginTime)) {
            valuesClause = "avg(value), max(value) as peak, min(value) as low";
        } else {
            valuesClause = "avg(value), max(maxvalue) as peak, min(minvalue) as low";
        }

        StringBuilder unions = new StringBuilder();
        String[] tables = MeasurementDataManagerUtility.getTables(beginTime, endTime);
        for (String table : tables) {
            if (unions.length() != 0) {
                unions.append("   UNION ALL \n ");
            }

            unions.append(getTableString(table, valuesClause, conditions));
        }

        String sql = "SELECT timestamp, max(av), max(peak), max(low) FROM ( \n"
            + "   (SELECT timestamp, avg(value) as av, max(value) as peak, min(value) as low FROM (\n"
            + unions.toString()
            + "   ) data GROUP BY timestamp) \n"
            + "   UNION ALL (select ? + (? * i) as timestamp, null as av, null as peak, null as low from RHQ_numbers where i < ?) ) alldata \n"
            + "GROUP BY timestamp ORDER BY timestamp";

        PreparedStatement ps = connection.prepareStatement(sql);

        StringBuilder fullSql = null;
        if (LOG.isDebugEnabled()) {
            fullSql = new StringBuilder(sql);
        }

        int i = 1;
        for (int tableIndex = 0; tableIndex < tables.length; tableIndex++) {
            ps.setLong(i++, beginTime);
            ps.setLong(i++, interval); //  2) interval
            ps.setInt(i++, numDataPoints); //  3) points
            ps.setLong(i++, interval); //  4) interval

            if (LOG.isDebugEnabled()) {
                replaceNextPlaceHolders(fullSql, beginTime, interval, numDataPoints, interval);
            }

            for (Object param : bindParameters) {
                if (param.getClass().isArray()) {
                    int length = Array.getLength(param);
                    if (length > MAX_IN_CLAUSE_PARAMS) {
                        throw new IllegalArgumentException("Number of resource id's must be less than or equal to "
                            + MAX_IN_CLAUSE_PARAMS + ".");
                    }

                    for (int x = 0; x < length; x++) {
                        Object bindValue = Array.get(param, x);
                        ps.setObject(i++, bindValue);
                        if (LOG.isDebugEnabled()) {
                            replaceNextPlaceHolders(fullSql, bindValue);
                        }
                    }
                } else {
                    ps.setObject(i++, param);
                    if (LOG.isDebugEnabled()) {
                        replaceNextPlaceHolders(fullSql, param);
                    }
                }
            }
        }

        ps.setLong(i++, beginTime); //  1) begin
        ps.setLong(i++, interval); //  2) interval
        ps.setInt(i++, numDataPoints); //  3) points

        if (LOG.isDebugEnabled()) {
            replaceNextPlaceHolders(fullSql, beginTime, interval, numDataPoints);
            LOG.debug(fullSql);
        }

        return ps;
    }

    public static String getTableString(String table, String valuesClause, String conditions) {
        return "      (SELECT beginTS as timestamp, value \n" //
            + "      FROM (select ? + (? * i) as beginTS, i from RHQ_numbers where i < ?) n, \n" //
            + "         " + table + " d " + " \n" //
            + "      WHERE time_stamp BETWEEN beginTS AND (beginTS + ?) \n" //
            + "      " + conditions + "      ) \n";
    }

    private void replaceNextPlaceHolders(StringBuilder sqlWithQuestionMarks, Object... valuesToReplace) {
        for (Object nextValue : valuesToReplace) {
            int index = sqlWithQuestionMarks.indexOf("?");
            sqlWithQuestionMarks.replace(index, index + 1, String.valueOf(nextValue));
        }
    }

    public static String getTable(long time) {
        return TABLE_PREFIX + nf.format(getTableIndex(time));
    }

    public static String[] getAllRawTables() {
        String[] tables = new String[TABLE_COUNT];
        for (int i = 0; i < TABLE_COUNT; i++) {
            tables[i] = TABLE_PREFIX + nf.format(i);
        }

        return tables;
    }

    /** 
     * The raw tables starting at the specified index. Can be useful for getting the tables in a necessary time order (like
     * oldest data first.
     * 
     * @param startIndex >= 0. If >= TABLE_COUNT normalized via modulo. 
     * @return Array of raw table names starting with the table with the specified index. 
     */
    public static String[] getAllRawTables(int startIndex) {
        String[] tables = new String[TABLE_COUNT];

        for (int i = 0; i < TABLE_COUNT; i++) {
            tables[i] = TABLE_PREFIX + nf.format((i + startIndex) % TABLE_COUNT);
        }

        return tables;
    }

    public static String[] getTables(long beginTime, long endTime) {
        List<String> tables = new ArrayList<String>();

        long now = System.currentTimeMillis();
        if ((now - RAW_PURGE) < beginTime) {
            int startIndex = getTableIndex(beginTime);
            int endIndex = getTableIndex(endTime);
            int tableCount = (((endIndex + TABLE_COUNT) - startIndex) % TABLE_COUNT) + 1;

            if ((endTime - beginTime) > (STORED_DAYS * MILLISECONDS_PER_DAY)) {
                throw new RuntimeException("Time span greater than raw data storage");
            }

            for (int i = 0; i < tableCount; i++) {
                tables.add(TABLE_PREFIX + nf.format(((startIndex + i) % TABLE_COUNT)));
            }

            return tables.toArray(new String[tables.size()]);
        } else if ((now - purge1h) < beginTime) {
            return new String[] { TABLE_DATA_1H };
        } else if ((now - purge6h) < beginTime) {
            return new String[] { TABLE_DATA_6H };
        } else {
            return new String[] { TABLE_DATA_1D };
        }
    }

    public static String getDeadTable(long time) {
        long tableIndex = getTableIndex(time);
        return TABLE_PREFIX + nf.format((tableIndex + 1) % (TABLE_COUNT));
    }

    public static String getCurrentRawTable() {
        return getTable(System.currentTimeMillis());
    }

    public static String getNextRotationTime() {
        long now = System.currentTimeMillis();
        long day = now / MILLISECONDS_PER_DAY;
        long timeOfDay = now - (day * MILLISECONDS_PER_DAY);

        long remaining = MILLISECONDS_PER_TABLE - timeOfDay;
        long nextRotation = now + remaining;
        if (nextRotation < now) {
            nextRotation += MILLISECONDS_PER_TABLE;
        }

        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL).format(new Date(nextRotation));
    }

    private static int getTableIndex(long time) {
        long day = time / MILLISECONDS_PER_DAY;
        long timeOfDay = time - (day * MILLISECONDS_PER_DAY);

        long table = ((day * TABLES_PER_DAY) + (timeOfDay / MILLISECONDS_PER_TABLE));
        long tableIndex = (table % TABLE_COUNT);

        return (int) tableIndex;
    }

    public static int getTableNameIndex(String tableName) {
        String indexString = tableName.substring(TABLE_PREFIX.length());
        int result;

        try {
            result = Integer.valueOf(indexString);
        } catch (NumberFormatException e) {
            LOG.error("Invalid raw table name: " + tableName + ", returning table index 0.");
            result = 0;
        }

        return result;
    }

    public long getPurge1h() {
        return purge1h;
    }

    public long getPurge6h() {
        return purge6h;
    }

    public long getPurge1d() {
        return purge1d;
    }

    public long getPurgeRt() {
        return purgeRt;
    }

    public long getPurgeAlert() {
        return purgeAlert;
    }

    public static long getRawTimePeriodStart(long end) {
        //long now = System.currentTimeMillis();
        return (end - RAW_PURGE);
    }

    public static boolean isRawTimePeriod(long beginTime) {
        long now = System.currentTimeMillis();
        return ((now - RAW_PURGE) < beginTime);
    }

    public static boolean isRawTable(String tableName) {
        return ((null != tableName) && (tableName.startsWith(TABLE_PREFIX)));
    }
}