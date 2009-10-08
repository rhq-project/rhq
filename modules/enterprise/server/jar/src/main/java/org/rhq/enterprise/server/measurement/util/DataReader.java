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
import java.util.Arrays;
import java.util.Date;

public class DataReader {
    private static Connection c;

    public static void read(long beginTime) throws SQLException {
        long numberOfDataPoints = 60;
        long endTime = beginTime + (1000L * 60 * 60 * 150);

        long interval = (endTime - beginTime) / numberOfDataPoints;

        System.out.println("Starting table: " + MeasurementDataManagerUtility.getTable(beginTime));
        System.out.println("Ending table: " + MeasurementDataManagerUtility.getTable(endTime));
        System.out.println("Dead table: " + MeasurementDataManagerUtility.getDeadTable(beginTime));

        StringBuilder unions = new StringBuilder();
        String[] tables = MeasurementDataManagerUtility.getTables(beginTime, endTime);
        for (String table : tables) {
            if (unions.length() != 0) {
                unions.append("   UNION \n ");
            }

            unions.append(getTableString(table));
        }

        String sql = "SELECT timestamp, max(av), max(peak), max(low) FROM ( \n"
            + "   (SELECT timestamp, avg(value) as av, max(value) as peak, min(value) as low FROM (\n"
            + unions.toString()
            + ") data GROUP BY timestamp) \n"
            + "   UNION (select ? + (? * i) as timestamp, 0 as av, 0 as peak, 0 as low from RHQ_numbers where i < ?) ) alldata \n"
            + "GROUP BY timestamp";

        //System.out.println(sql);

        StringBuilder fullSql = new StringBuilder(sql);

        PreparedStatement ps = c.prepareStatement(sql);

        int i = 1;
        for (String table : tables) {
            ps.setLong(i++, beginTime); //  1) begin
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(beginTime));

            ps.setLong(i++, interval); //  2) interval
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(interval));

            ps.setLong(i++, numberOfDataPoints); //  3) points
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(numberOfDataPoints));

            ps.setLong(i++, interval); //  4) interval
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(interval));

            ps.setInt(i++, 0); // schedule_id
            fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(0));
        }

        ps.setLong(i++, beginTime); //  1) begin
        fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(beginTime));

        ps.setLong(i++, interval); //  2) interval
        fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(interval));

        ps.setLong(i++, numberOfDataPoints); //  3) points
        fullSql.replace(fullSql.indexOf("?"), fullSql.indexOf("?") + 1, String.valueOf(numberOfDataPoints));

        System.out.println("-------------------------------------");
        System.out.println("\n\n\nFinal sql was:\n" + fullSql.toString());
        System.out.println("-------------------------------------");

        long timingStart = System.currentTimeMillis();
        ResultSet rs = ps.executeQuery();
        System.out.println("Executed query in: " + (System.currentTimeMillis() - timingStart) + "ms");

        int count = 0;
        long lastStart = 0;
        while (rs.next()) {
            count++;
            if (lastStart != 0) {
                System.out.println(Arrays.deepToString(MeasurementDataManagerUtility
                    .getTables(lastStart, rs.getLong(1))));
            }

            lastStart = rs.getLong(1);
            System.out.println(new Date(rs.getLong(1)) + " - avg: " + rs.getDouble(2) + " - max: " + rs.getDouble(3)
                + " - min: " + rs.getDouble(4));
        }

        System.out.println("Count: " + count);
    }

    public static String getTableString(String table) {
        return "      (SELECT begin as timestamp, value \n"
            + "      FROM (select ? + (? * i) as begin, i from RHQ_numbers where i < ?) n,\n" + "         " + table
            + " d \n" + "      WHERE time_stamp BETWEEN begin AND (begin + ?)\n" + "         AND d.schedule_id = ?\n"
            + "      ORDER BY begin) \n";
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432", "jon", "jon");

        long time = System.currentTimeMillis();
        read(time - (1000L * 60 * 60 * 70));
    }
}