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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is purposefully not TimeZone sensitive. It makes this easier to deal with in clusters with servers in different
 * timezones, but it means that we can't rely on table maintenance in the middle of the night. That's ok, we're going to
 * do this like the rest of the maintenance on the hour. It does mean that even with 12 hour tables we may have data in
 * one from 10 am to 10 pm local instead of 12 to 12.
 *
 * <p/>Due to the leap seconds its possible some tables will technically span more than MILLISECONDS_PER_TABLE of real
 * time. This is an irrelevant quirk.
 *
 * @author Greg Hinkle
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
    public static final long MILLESECONDS_PER_TABLE = MILLISECONDS_PER_DAY / TABLES_PER_DAY;

    public static final long RAW_PURGE = STORED_DAYS * MILLISECONDS_PER_DAY;

    private Connection connection;

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

    public static MeasurementDataManagerUtility getInstance(Connection connection) {
        MeasurementDataManagerUtility util = new MeasurementDataManagerUtility();
        util.connection = connection;
        return util;
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

    private static int getTableIndex(long time) {
        long day = time / MILLISECONDS_PER_DAY;
        long timeOfDay = time - (day * MILLISECONDS_PER_DAY);

        long table = ((day * TABLES_PER_DAY) + (timeOfDay / MILLESECONDS_PER_TABLE));
        long tableIndex = (table % TABLE_COUNT);

        return (int) tableIndex;
    }

    public static int getTableNameIndex(String tableName) {
        String indexString = tableName.substring(TABLE_PREFIX.length());
        int result;

        try {
            result = Integer.valueOf(indexString).intValue();
        } catch (NumberFormatException e) {
            LOG.error("Invalid raw table name: " + tableName + ", returning table index 0.");
            result = 0;
        }

        return result;
    }

    /**
     * Get the server purge configuration, loaded on startup.
     */
    private static void loadPurgeDefaults() {
        LOG.debug("Loading default purge intervals");

        try {
            Properties conf = LookupUtil.getSystemManager().getSystemConfiguration();
            purge1h = Long.parseLong(conf.getProperty(HQConstants.DataPurge1Hour));
            purge6h = Long.parseLong(conf.getProperty(HQConstants.DataPurge6Hour));
            purge1d = Long.parseLong(conf.getProperty(HQConstants.DataPurge1Day));
            purgeRt = Long.parseLong(conf.getProperty(HQConstants.RtDataPurge));
            purgeAlert = Long.parseLong(conf.getProperty(HQConstants.AlertPurge));
        } catch (Exception e) {
            // Shouldn't happen unless manual edit of config table
            //throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForResource(long beginTime,
        long endTime, int resourceId, int[] measurementDefinitionIds) throws MeasurementNotFoundException {
        String otherTable = ", RHQ_MEASUREMENT_SCHED s ";

        String conditions = "  AND d.schedule_id = s.id" + "  AND s.resource_id = ? \n" + "  AND s.definition = ? \n";

        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection myConnection = null;
        List<List<MeasurementDataNumericHighLowComposite>> data = new ArrayList<List<MeasurementDataNumericHighLowComposite>>();

        try {
            myConnection = getConnection();
            for (int measurementDefinitionId : measurementDefinitionIds) {
                ps = null;
                rs = null;
                try {
                    ps = getFullQuery(myConnection, beginTime, endTime, 60, otherTable, conditions, resourceId,
                        measurementDefinitionId);
                    rs = ps.executeQuery();

                    List<MeasurementDataNumericHighLowComposite> compositeList = new ArrayList<MeasurementDataNumericHighLowComposite>();
                    while (rs.next()) {
                        long timestamp = rs.getLong(1);
                        double value = rs.getDouble(2);
                        if (rs.wasNull()) {
                            value = Double.NaN;
                        }

                        double peak = rs.getDouble(3);
                        if (rs.wasNull()) {
                            peak = Double.NaN;
                        }

                        double low = rs.getDouble(4);
                        if (rs.wasNull()) {
                            low = Double.NaN;
                        }

                        MeasurementDataNumericHighLowComposite next = new MeasurementDataNumericHighLowComposite(
                            timestamp, value, peak, low);
                        compositeList.add(next);
                    }

                    data.add(compositeList);
                } finally {
                    JDBCUtil.safeClose(ps, rs);
                }
            }
        } catch (SQLException e) {
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(myConnection);
            connection = null; // the close above invalidates the member
        }
        return data;
    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForSiblingResources(long beginTime,
        long endTime, int[] resourceIds, int measurementDefinitionId) throws MeasurementNotFoundException {
        String otherTable = ", RHQ_MEASUREMENT_SCHED s ";

        String conditions = "  AND d.schedule_id = s.id" + "  AND s.resource_id = ? \n" + "  AND s.definition = ? \n";

        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection myConnection = null;
        List<List<MeasurementDataNumericHighLowComposite>> data = new ArrayList<List<MeasurementDataNumericHighLowComposite>>();
        for (int resourceId : resourceIds) {
            try {
                myConnection = getConnection();
                ps = getFullQuery(myConnection, beginTime, endTime, 60, otherTable, conditions, resourceId,
                    measurementDefinitionId);
                rs = ps.executeQuery();

                List<MeasurementDataNumericHighLowComposite> compositeList = new ArrayList<MeasurementDataNumericHighLowComposite>();
                while (rs.next()) {
                    long timestamp = rs.getLong(1);
                    double value = rs.getDouble(2);
                    if (rs.wasNull()) {
                        value = Double.NaN;
                    }

                    double peak = rs.getDouble(3);
                    if (rs.wasNull()) {
                        peak = Double.NaN;
                    }

                    double low = rs.getDouble(4);
                    if (rs.wasNull()) {
                        low = Double.NaN;
                    }

                    MeasurementDataNumericHighLowComposite next = new MeasurementDataNumericHighLowComposite(timestamp,
                        value, peak, low);
                    compositeList.add(next);
                }

                data.add(compositeList);
            } catch (SQLException e) {
                throw new MeasurementNotFoundException(e);
            } finally {
                JDBCUtil.safeClose(myConnection, ps, rs);
                connection = null; // the close above invalidates the member
            }
        }

        return data;
    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataAggregatesForSiblingResources(
        long beginTime, long endTime, int[] resourceIds, int measurementDefinitionId)
        throws MeasurementNotFoundException {
        String otherTable = ", RHQ_MEASUREMENT_SCHED s ";

        String conditions = "  AND d.schedule_id = s.id" + "  AND s.resource_id IN ( "
            + JDBCUtil.generateInBinds(resourceIds.length) + ") \n" + "  AND s.definition = ? \n";

        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection myConnection = null;
        List<List<MeasurementDataNumericHighLowComposite>> data = new ArrayList<List<MeasurementDataNumericHighLowComposite>>();
        try {
            myConnection = getConnection();
            ps = getFullQuery(myConnection, beginTime, endTime, 60, otherTable, conditions, resourceIds,
                measurementDefinitionId);
            rs = ps.executeQuery();

            List<MeasurementDataNumericHighLowComposite> compositeList = new ArrayList<MeasurementDataNumericHighLowComposite>();
            while (rs.next()) {
                long timestamp = rs.getLong(1);
                double value = rs.getDouble(2);
                if (rs.wasNull()) {
                    value = Double.NaN;
                }

                double peak = rs.getDouble(3);
                if (rs.wasNull()) {
                    peak = Double.NaN;
                }

                double low = rs.getDouble(4);
                if (rs.wasNull()) {
                    low = Double.NaN;
                }

                MeasurementDataNumericHighLowComposite next = new MeasurementDataNumericHighLowComposite(timestamp,
                    value, peak, low);
                compositeList.add(next);
            }

            data.add(compositeList);
            return Collections.singletonList(compositeList);
        } catch (SQLException e) {
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(myConnection, ps, rs);
            connection = null; // the close above invalidates the member
        }
    }

    public MeasurementAggregate getAggregateByScheduleId(long beginTime, long endTime, long scheduleId)
        throws MeasurementNotFoundException {
        Connection myConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            myConnection = getConnection();
            String condition = "         AND d.schedule_id = ?\n";

            ps = getFullQuery(myConnection, beginTime, endTime, 1, "", condition, scheduleId);

            rs = ps.executeQuery();
            if (rs.next()) {
                MeasurementAggregate measurementAggregate = fillAggregateFromResultSet(rs);
                return measurementAggregate;
            }

            throw new MeasurementNotFoundException("Data not found");
        } catch (SQLException e) {
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(myConnection, ps, rs);
            connection = null; // the close above invalidates the member
        }
    }

    /**
     * @param  rs
     *
     * @return
     *
     * @throws SQLException
     */
    private MeasurementAggregate fillAggregateFromResultSet(ResultSet rs) throws SQLException {
        /*
         * ResultSet.getDouble() will return 0.0 for a SQL null so we need to explicitly check for null and set the
         * value to NaN accordingly
         */
        Double min = rs.getDouble(4);
        if (rs.wasNull()) {
            min = Double.NaN;
        }

        Double avg = rs.getDouble(2);
        if (rs.wasNull()) {
            avg = Double.NaN;
        }

        Double max = rs.getDouble(3);
        if (rs.wasNull()) {
            max = Double.NaN;
        }

        MeasurementAggregate measurementAggregate = new MeasurementAggregate(min, avg, max);
        return measurementAggregate;
    }

    public MeasurementAggregate getAggregateByScheduleIds(long beginTime, long endTime, int[] scheduleIds)
        throws MeasurementNotFoundException {

        Connection myConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String condition = null;

        try {
            condition = "         AND d.schedule_id IN ( " + JDBCUtil.generateInBinds(scheduleIds.length) + ")\n";

            myConnection = getConnection();
            ps = getFullQuery(myConnection, beginTime, endTime, 1, "", condition, scheduleIds);

            rs = ps.executeQuery();
            if (rs.next()) {
                MeasurementAggregate measurementAggregate = fillAggregateFromResultSet(rs);
                return measurementAggregate;
            }

            throw new MeasurementNotFoundException("Data not found");
        } catch (SQLException e) {
            LOG.warn("Error condition :" + condition);
            throw new MeasurementNotFoundException(e);
        } finally {
            JDBCUtil.safeClose(myConnection, ps, rs);
            connection = null; // the close above invalidates the member
        }
    }

    public MeasurementDataNumeric getLatestValueForSchedule(int scheduleId) {
        long now = System.currentTimeMillis();
        int index = getTableIndex(now);

        do {
            Connection myConnection = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                myConnection = getConnection();
                String table = TABLE_PREFIX + nf.format(index);
                String query = "SELECT d.time_stamp, d.value \n" + "FROM " + table + " d \n"
                    + "WHERE d.schedule_id = ? \n" + "AND d.time_stamp = ( SELECT MAX(dd.time_stamp) \n" + "FROM "
                    + table + " dd \n" + "WHERE dd.schedule_id = ? )";
                ps = myConnection.prepareStatement(query);
                ps.setInt(1, scheduleId);
                ps.setInt(2, scheduleId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return new MeasurementDataNumeric(new MeasurementDataPK(rs.getLong(1), scheduleId), rs.getDouble(2));
                }
            } catch (SQLException e) {
                throw new MeasurementNotFoundException(e);
            } finally {
                JDBCUtil.safeClose(myConnection, ps, rs);
                connection = null; // the close above invalidates the member
            }

            index = index - 1;
            if (index < 0) {
                index = LIVE_TABLES;
            }
        } while (!(TABLE_PREFIX + nf.format(index)).equals(getDeadTable(now)));

        return null;
    }

    private PreparedStatement getFullQuery(Connection connection, long beginTime, long endTime, int numberOfDataPoints,
        String otherTables, String conditions, Object... bindParameters) throws SQLException {
        long interval = (endTime - beginTime) / numberOfDataPoints;

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

            unions.append(getTableString(table, valuesClause, otherTables, conditions));
        }

        String sql = "SELECT timestamp, max(av), max(peak), max(low) FROM ( \n"
            + "   (SELECT timestamp, avg(value) as av, max(value) as peak, min(value) as low FROM (\n"
            + unions.toString()
            + "   ) data GROUP BY timestamp) \n"
            + "   UNION (select ? + (? * i) as timestamp, null as av, null as peak, null as low from RHQ_numbers where i < ?) ) alldata \n"
            + "GROUP BY timestamp ORDER BY timestamp";

        PreparedStatement ps = connection.prepareStatement(sql);

        StringBuilder fullSql = null;
        if (LOG.isDebugEnabled()) {
            fullSql = new StringBuilder(sql);
        }

        int i = 1;
        for (String table : tables) {
            ps.setLong(i++, beginTime);
            ps.setLong(i++, interval); //  2) interval
            ps.setInt(i++, numberOfDataPoints); //  3) points
            ps.setLong(i++, interval); //  4) interval

            if (LOG.isDebugEnabled()) {
                fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(beginTime));
                fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(interval));
                fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(numberOfDataPoints));
                fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(interval));
            }

            for (Object param : bindParameters) {
                if (param.getClass().isArray()) {
                    int length = Array.getLength(param);
                    if (length > MAX_IN_CLAUSE_PARAMS) {
                        throw new IllegalArgumentException("Number of resource id's must be less than or equal to "
                            + MAX_IN_CLAUSE_PARAMS + ".");
                    }

                    for (int x = 0; x < length; x++) {
                        ps.setObject(i++, Array.get(param, x));
                        if (LOG.isDebugEnabled()) {
                            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(param));
                        }
                    }
                } else {
                    ps.setObject(i++, param);
                    if (LOG.isDebugEnabled()) {
                        fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(param));
                    }
                }
            }
        }

        ps.setLong(i++, beginTime); //  1) begin
        ps.setLong(i++, interval); //  2) interval
        ps.setInt(i++, numberOfDataPoints); //  3) points

        if (LOG.isDebugEnabled()) {
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(beginTime));
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(interval));
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(numberOfDataPoints));
            LOG.debug(fullSql);
        }

        return ps;
    }

    public static String getTableString(String table, String valuesClause, String otherTables, String conditions) {
        return "      (SELECT begin as timestamp, value \n"
            + "      FROM (select ? + (? * i) as begin, i from RHQ_numbers where i < ?) n,\n" + "         " + table
            + " d " + otherTables + " \n" + "      WHERE time_stamp BETWEEN begin AND (begin + ?)\n" + "      "
            + conditions + "      ) \n";
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

    public static void main(String[] args) throws Exception {
        //      Class.forName("org.postgresql.Driver");
        //      Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432","jon","jon");

        Class.forName("oracle.jdbc.driver.OracleDriver");
        String db = "jdbc:oracle:thin:@192.168.1.5:1521:xe";

        System.out.println("=============getAggregateByScheduleId=================================================");
        Connection c = DriverManager.getConnection(db, "jon", "jon");
        long now = System.currentTimeMillis();
        long start = now - (1000L * 60 * 60 * 22);
        MeasurementAggregate ag = getInstance(c).getAggregateByScheduleId(start, now, 1);
        System.out.println(ag);

        System.out.println("=============getAggregateByScheduleIds=================================================");
        c = DriverManager.getConnection(db, "jon", "jon");
        ag = getInstance(c).getAggregateByScheduleIds(start, now, new int[] { 1, 2, 3 });
        System.out.println(ag);

        System.out
            .println("=============getMeasurementDataForResource=================================================");
        c = DriverManager.getConnection(db, "jon", "jon");
        List<List<MeasurementDataNumericHighLowComposite>> data = getInstance(c).getMeasurementDataForResource(start,
            now, 50, new int[] { 166 });
        System.out.println(data);

        System.out
            .println("===========getMeasurementDataForSiblingResources===================================================");
        c = DriverManager.getConnection(db, "jon", "jon");
        data = getInstance(c).getMeasurementDataForSiblingResources(start, now, new int[] { 50 }, 166);
        System.out.println(data);

        System.out
            .println("=============getMeasurementDataAggregatesForSiblingResources=================================================");
        c = DriverManager.getConnection(db, "jon", "jon");
        data = getInstance(c).getMeasurementDataAggregatesForSiblingResources(start, now, new int[] { 50 }, 166);
        System.out.println(data);

        //      Date d = new Date(0);
        //      System.out.println("Table for time " + d + " (" + d.getTime() + "): " + getTable(d.getTime()) + " Dead: " + getDeadTable(d.getTime()));
        //
        //      d = new Date();
        //      System.out.println("Table for time " + d + " (" + d.getTime() + "): " + getTable(d.getTime()) + " Dead: " + getDeadTable(d.getTime()));
        //
        //      for (int i = 0; i < (24 * 7); i++)
        //      {
        //         d = new Date(d.getTime() + (1000L * 60 * 60));
        //         System.out.println("Table for time " + d + " (" + d.getTime() + "): " + getTable(d.getTime()) + " Dead: " + getDeadTable(d.getTime()));
        //      }

        /* System.out.println("-----------");
         * for (int i = 0; i < 100; i++) {
         *
         * long time = TimingVoodoo.roundDownTime(now - (1000L * 60 *60 * i), 1000L * 60 * 60); System.out.println("Table
         * for " + new Date(time) + " is " + getTable(time)); System.out.println("Table for " + new Date(time-1) + " is
         * " + getTable(time-1));
         *
         * }*/

        String[] ts = getTables(now - (1000L * 60 * 60 * 8), now);
        System.out.println(Arrays.toString(ts));
        System.out.println("NOW: " + getTable(now));
    }

    public static long getRawTimePeriodStart(long end) {
        long now = System.currentTimeMillis();
        return (end - RAW_PURGE);
    }

    public static boolean isRawTimePeriod(long beginTime) {
        long now = System.currentTimeMillis();
        return ((now - RAW_PURGE) < beginTime);
    }

    public static boolean isRawTable(String tableName) {
        return ((null != tableName) && (tableName.startsWith(TABLE_PREFIX)));
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null) {
            return connection;
        } else {
            return datasource.getConnection();
        }
    }
}