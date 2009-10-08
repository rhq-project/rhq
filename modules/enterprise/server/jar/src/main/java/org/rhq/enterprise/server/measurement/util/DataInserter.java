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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.measurement.MeasurementStorageException;

public class DataInserter {
    private static final long INTERVAL = 5 * 60 * 1000L; // five minute average
    private static final long HOUR = 1000L * 60 * 60;

    private static Connection c;

    // Inserts one hour of time
    public static void insert(long time, int numSchedules) throws SQLException {
        c.setAutoCommit(false);
        int count = 0;

        long start = System.currentTimeMillis();

        try {
            List<Integer> scheduleIds = new ArrayList<Integer>();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT id FROM RHQ_MEASUREMENT_SCHED");

            while (rs.next() && (numSchedules > 0)) {
                scheduleIds.add(rs.getInt(1));
                numSchedules--;
            }

            JDBCUtil.safeClose(s, rs);

            System.out.println("INSERTING for " + scheduleIds.size() + " schedules starting at " + new Date(time));

            for (long i = 0; i < HOUR; i += INTERVAL) {
                long ts = (long) (time + i + (Math.random() * 1000D));

                PreparedStatement ps = null;

                try {
                    String table = MeasurementDataManagerUtility.getTable(ts);
                    String insertSql = "INSERT INTO " + table + "(schedule_id,time_stamp,value) VALUES(?,?,?)";
                    ps = c.prepareStatement(insertSql);

                    for (int scheduleId : scheduleIds) {
                        ps.setInt(1, scheduleId);
                        ps.setLong(2, ts);
                        ps.setInt(3, (int) (Math.random() * 100));

                        ps.addBatch();
                    }

                    int[] res = ps.executeBatch();
                    for (int updates : res) {
                        if (updates != 1) {
                            throw new MeasurementStorageException("Unexpected batch update size [" + updates + "]");
                        }

                        count++;
                    }

                    c.commit();
                    System.out.print(".");
                    System.out.flush();
                    JDBCUtil.safeClose(ps);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    JDBCUtil.safeClose(ps);
                }
            }

            System.out.println(" " + count + " rows added in " + (System.currentTimeMillis() - start)
                + "ms starting in " + MeasurementDataManagerUtility.getTable(time));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public static int sum(int[] arr) {
        int count = 0;
        for (int ar : arr) {
            count += ar;
        }

        return count;
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        String driver = System.getProperty("driver", "org.postgresql.Driver");
        String url = System.getProperty("url", "jdbc:postgresql://127.0.0.1:5432");
        String username = System.getProperty("username", "jon");
        String password = System.getProperty("password", "jon");
        int numSchedules = Integer.parseInt(System.getProperty("numSchedules", "5"));

        System.out.println("driver=" + driver);
        System.out.println("url=" + url);
        System.out.println("username=" + username);
        System.out.println("password=" + password);
        System.out.println("numSchedules=" + numSchedules);
        System.out.println("(to override these, set system properties of the same names as above)");

        Class.forName(driver);
        c = DriverManager.getConnection(url, username, password);

        try {
            long time = System.currentTimeMillis() - (7L * 24 * HOUR);
            time = time - (time % HOUR);
            for (int i = 0; i < (7 * 24); i++) {
                long bucket = time + (i * HOUR);
                insert(bucket, numSchedules);
            }
        } finally {
            JDBCUtil.safeClose(c, null, null);
        }
    }
}