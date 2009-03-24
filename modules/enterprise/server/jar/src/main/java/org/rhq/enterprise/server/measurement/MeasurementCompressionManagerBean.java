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
package org.rhq.enterprise.server.measurement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.clientapi.util.TimeUtil;
import org.rhq.core.util.StopWatch;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.TimingVoodoo;

/**
 * Compresses data that increases in size over time to maintain the system for long durations. Any table that
 * continuously grows in a non-negligible manner should be compressed and/or purged in this job.
 *
 * @author Greg Hinkle
 * @author jay shaughnessy
 */
@Stateless
public class MeasurementCompressionManagerBean implements MeasurementCompressionManagerLocal {
    private final Log log = LogFactory.getLog(MeasurementCompressionManagerBean.class);

    private static final String DATASOURCE_NAME = RHQConstants.DATASOURCE_JNDI_NAME;

    private static final long SECOND = 1000L;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;
    private static final long SIX_HOUR = HOUR * 6;

    @javax.annotation.Resource
    private SessionContext ctx;

    @EJB
    private SystemManagerLocal systemManager;
    @EJB
    private MeasurementCompressionManagerLocal compressionManager;

    private long purge1h;
    private long purge6h;
    private long purge1d;

    /**
     * Get the server purge configuration, loaded on startup.
     */
    private void loadPurgeDefaults() {
        this.log.debug("Loading default purge intervals");

        Properties conf = systemManager.getSystemConfiguration();

        try {
            this.purge1h = Long.parseLong(conf.getProperty(RHQConstants.DataPurge1Hour));
            this.purge6h = Long.parseLong(conf.getProperty(RHQConstants.DataPurge6Hour));
            this.purge1d = Long.parseLong(conf.getProperty(RHQConstants.DataPurge1Day));
        } catch (NumberFormatException e) {
            // Shouldn't happen unless the config table was manually edited.
            throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public void compressData() throws SQLException {
        loadPurgeDefaults();

        // current time rounded down to the start of this hour.
        long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), HOUR);

        // Compress hourly data
        long hourAgo = TimingVoodoo.roundDownTime(now - HOUR, HOUR);
        String deadTable = MeasurementDataManagerUtility.getDeadTable(hourAgo);
        int deadTableIndex = MeasurementDataManagerUtility.getTableNameIndex(deadTable);
        String[] rawTables = MeasurementDataManagerUtility.getAllRawTables(deadTableIndex + 1);

        // compress uncompressed raw data in the non-dead tables. Go through all of the tables
        // to handle missed compressions due to a downed server. Start with the oldest data first so that
        // we build up the history correctly (because compression rolls forward in time.)
        // NOTE : This is somewhat inefficient for a running server actively collecting raw data as
        // NOTE : in general we'll only really need to process the "now" table. Although, 
        // NOTE : compressionManager.compressData has an optimization to return quickly if there is no work.
        for (String rawTable : rawTables) {
            if (!rawTable.equals(deadTable)) {
                compressionManager.compressData(rawTable, TAB_DATA_1H, HOUR, now);
            }
        }

        // truncate the dead table. Note that we may truncate the same table repeatedly during a 12 hour cycle, this is OK, it's fast
        compressionManager.truncateMeasurements(deadTable);

        // begin time of last compression period
        long last;

        // Compress 6 hour data
        last = compressionManager.compressData(TAB_DATA_1H, TAB_DATA_6H, SIX_HOUR, now);

        // Purge, ensuring we don't purge data not yet compressed.
        compressionManager.purgeMeasurements(TAB_DATA_1H, Math.min(now - this.purge1h, last));

        // Compress daily data
        last = compressionManager.compressData(TAB_DATA_6H, TAB_DATA_1D, DAY, now);

        // Purge, ensuring we don't purge data not yet compressed.
        compressionManager.purgeMeasurements(TAB_DATA_6H, Math.min(now - this.purge6h, last));

        // Purge, we never store more than 1 year of data.
        compressionManager.purgeMeasurements(TAB_DATA_1D, now - this.purge1d);
    }

    /**
     * Compress data.
     *
     * @return The last timestamp that was compressed
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public long compressData(String fromTable, String toTable, long interval, long now) throws SQLException {

        // First determine the window to operate on.  If no previous compression
        // information is found, the last value from the table to compress from
        // is used.  (This will only occur on the first compression run).
        long start = getMaxTimestamp(toTable);

        if (start == 0L) {
            // No compressed data found, start from scratch.
            start = getMinTimestamp(fromTable);

            // No measurement data found. (Probably a new installation)
            if (start == 0L) {
                return 0L;
            }
        } else {
            // Start at next interval
            start = start + interval;
        }

        // If a server has been down for a long time (greater than our raw data store time) we 
        // may find data older than our raw collection period. It is considered stale and ignored.
        // Ensure a valid begin time by setting it no older than the valid collection period.
        long rawTimeStart = TimingVoodoo.roundDownTime(MeasurementDataManagerUtility.getRawTimePeriodStart(now), HOUR);
        // Rounding only necessary if we are starting from scratch.
        long begin = TimingVoodoo.roundDownTime(start, interval);

        if (begin < rawTimeStart) {
            begin = rawTimeStart;
        }

        // If the from table does not contain data later than the begin time then just return
        long fromTableMax = getMaxTimestamp(fromTable);
        if (fromTableMax < begin) {
            return begin;
        }

        if (log.isDebugEnabled()) {
            log.debug("Compressing from: " + fromTable + " to " + toTable);
        }

        // Compress all the way up to now.
        return compactData(fromTable, toTable, begin, now, interval);
    }

    private long compactData(String fromTable, String toTable, long begin, long now, long interval) throws SQLException {
        Connection conn = null;
        PreparedStatement selStmt = null;
        PreparedStatement insStmt = null;
        ResultSet rs = null;

        try {
            conn = ((DataSource) ctx.lookup(DATASOURCE_NAME)).getConnection();

            selStmt = conn.prepareStatement("SELECT count(DISTINCT schedule_id) FROM " + fromTable
                + " WHERE time_stamp >= ? AND time_stamp < ?");

            // One special case. If we are compressing from an
            // already compressed table, we'll take the MIN and
            // MAX from the already calculated min and max columns.
            String minMax;
            if (MeasurementDataManagerUtility.isRawTable(fromTable)) {
                minMax = "AVG(value), MIN(value), MAX(value) ";
            } else {
                minMax = "AVG(value), MIN(minvalue), MAX(maxvalue) ";
            }

            // TODO GH: Why does this do each schedule separately?
            insStmt = conn.prepareStatement("INSERT INTO " + toTable + " (SELECT ?, ft.schedule_id, " + minMax
                + "  FROM " + fromTable + " ft " + "  WHERE ft.time_stamp >= ? AND ft.time_stamp < ? "
                + "  GROUP BY ft.schedule_id)");

            StopWatch watch = new StopWatch();
            while (begin < now) {
                watch.reset();
                long end = begin + interval;

                // Get the list of measurement id's for this interval.
                selStmt.setLong(1, begin);
                selStmt.setLong(2, end);

                log.debug("Compression interval: " + TimeUtil.toString(begin) + " to " + TimeUtil.toString(end));

                try {
                    rs = selStmt.executeQuery();

                    while (rs.next()) {
                        log.debug("Compressing [" + rs.getInt(1) + "] measurements");
                    }
                } finally {
                    close(rs);
                }

                // Compress.
                int rows = 0;
                try {
                    insStmt.setLong(1, begin);
                    insStmt.setLong(2, begin);
                    insStmt.setLong(3, end);
                    rows = insStmt.executeUpdate();
                    log.debug("Rows merged: " + rows);
                } catch (SQLException e) {
                    // Just log the error and continue
                    log.debug("SQL exception when inserting data at " + TimeUtil.toString(begin), e);
                }

                MeasurementMonitor.getMBean().incrementMeasurementCompressionTime(watch.getElapsed());
                log.info("Compressed from " + fromTable + " into " + rows + " rows in " + toTable + " in ("
                    + (watch.getElapsed() / SECOND) + " seconds)");

                // Increment for next iteration.
                begin = end;
            }
        } finally {
            close(selStmt);
            close(insStmt);
            close(conn);
        }

        // Return the last interval that was compressed.
        return begin;
    }

    /**
     * Get the oldest timestamp in the database. Getting the minimum time is expensive, so this is only called once when
     * the compression routine runs for the first time. After the first call, the range is cached.
     */
    private long getMinTimestamp(String dataTable) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = ((DataSource) ctx.lookup(DATASOURCE_NAME)).getConnection();
            String sql = "SELECT MIN(time_stamp) FROM " + dataTable; // returns null rows if nothing exists

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            // We'll have a single result
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new SQLException("Unable to determine oldest measurement");
            }
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Get the most recent measurement.
     */
    private long getMaxTimestamp(String dataTable) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = ((DataSource) ctx.lookup(DATASOURCE_NAME)).getConnection();
            String sql = "SELECT MAX(time_stamp) FROM " + dataTable; // returns null rows if nothing exists

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            // We'll have a single result
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                // New installation
                return 0L;
            }
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Purge data older than a given time.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public void purgeMeasurements(String tableName, long purgeAfter) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        log.debug("Purging data older than " + TimeUtil.toString(purgeAfter) + " in " + tableName);
        StopWatch watch = new StopWatch();
        int rows;
        try {
            conn = ((DataSource) ctx.lookup(DATASOURCE_NAME)).getConnection();

            String sql = "DELETE FROM " + tableName + " WHERE time_stamp < ?";

            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, purgeAfter);

            rows = stmt.executeUpdate();
        } finally {
            close(stmt);
            close(conn);
        }

        MeasurementMonitor.getMBean().incrementPurgeTime(watch.getElapsed());
        log.info("Done purging [" + rows + "] rows older than " + TimeUtil.toString(purgeAfter) + " from " + tableName
            + " in (" + ((watch.getElapsed()) / SECOND) + " seconds)");
    }

    // this is "NOT_SUPPORTED" because the database will auto-commit the DDL "truncate table"
    // and this causes havoc with the XA transaction when set to "REQUIRES_NEW"
    // for example, on oracle, this method would throw SQLException: ORA-02089: COMMIT is not allowed in a subordinate session
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @TransactionTimeout(6 * 60 * 60)
    public void truncateMeasurements(String tableName) throws SQLException {
        // Make sure we only truncate the dead table... other tables may have live data in them
        if (tableName.equals(MeasurementDataManagerUtility.getDeadTable(System.currentTimeMillis()))) {
            Connection conn = null;
            Statement stmt = null;
            StopWatch watch = new StopWatch();
            try {
                conn = ((DataSource) ctx.lookup(DATASOURCE_NAME)).getConnection();
                stmt = conn.createStatement();
                long startTime = System.currentTimeMillis();
                stmt.executeUpdate("TRUNCATE TABLE " + tableName);
                MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
            } finally {
                close(stmt);
                close(conn);
                log.info("Truncated table: " + tableName + " in (" + (watch.getElapsed() / SECOND) + " seconds)");
            }
        }
    }

    private void close(Statement c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (Exception e) {
            log.warn(this.ctx + ": Error closing statement.", e);
        }
    }

    private void close(ResultSet c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (Exception e) {
            log.warn(this.ctx + ": Error closing result set.", e);
        }
    }

    private void close(Connection c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (Exception e) {
            log.warn(this.ctx + ": Error closing connection.", e);
        }
    }
}