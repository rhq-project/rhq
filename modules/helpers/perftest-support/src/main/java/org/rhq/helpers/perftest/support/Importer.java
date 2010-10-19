/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support;

import java.sql.Connection;
import java.util.Properties;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.rhq.helpers.perftest.support.dbunit.DbUnitUtil;

/**
 * Utility method to import the data previously produced by the {@link Exporter}.
 *
 * @author Lukas Krejci
 */
public class Importer {

    private Importer() {

    }

    /**
     * Imports the data from the input into a database using the provided JDBC connection.
     *
     * @param jdbcConnection
     * @param input
     * @throws Exception
     */
    public static void run(Connection jdbcConnection, Input input) throws Exception {
        run(new DatabaseConnection(jdbcConnection), input);
    }

    public static void run(Properties settings, Input input) throws Exception {
        run(DbUnitUtil.getConnection(settings), input);
    }

    public static void run(IDatabaseConnection connection, Input input) throws Exception {
        ReplacementDataSet dataSet = new ReplacementDataSet(new StreamingDataSet(input.getProducer()));
        dataSet.addReplacementObject(Settings.NULL_REPLACEMENT, null);

        DatabaseOperation.DELETE_ALL.execute(connection, dataSet);

        input.close();

        dataSet = new ReplacementDataSet(new StreamingDataSet(input.getProducer()));
        dataSet.addReplacementObject(Settings.NULL_REPLACEMENT, null);

        DatabaseOperation.INSERT.execute(connection, dataSet);
    }
}
