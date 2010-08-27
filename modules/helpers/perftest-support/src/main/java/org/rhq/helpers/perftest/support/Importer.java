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
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.rhq.helpers.perftest.support.util.DbUnitUtil;

/**
 *
 * @author Lukas Krejci
 */
public class Importer {
    
    private Importer() {
        
    }
    
    public static void run(Connection jdbcConnection, IDataSetProducer producer) throws Exception {
        run(new DatabaseConnection(jdbcConnection), producer);
    }
    
    public static void run(Properties settings, IDataSetProducer producer) throws Exception {
        run(DbUnitUtil.getConnection(settings), producer);
    }
    
    private static void run(IDatabaseConnection connection, IDataSetProducer producer) throws Exception {
        IDataSet data = new StreamingDataSet(producer);
        DatabaseOperation.CLEAN_INSERT.execute(connection, data);        
    }
}
