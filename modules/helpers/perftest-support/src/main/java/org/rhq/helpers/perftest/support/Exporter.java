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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.set.ListOrderedSet;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.ForwardOnlyResultSetTableFactory;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.PrimaryKeyFilter.PkTableMap;
import org.dbunit.database.search.ImportedKeysSearchCallbackFilteredByPKs;
import org.dbunit.database.search.TablesDependencyHelper;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.stream.DataSetProducerAdapter;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.dbunit.util.CollectionsHelper;
import org.dbunit.util.search.DepthFirstSearch;
import org.dbunit.util.search.SearchException;
import org.rhq.helpers.perftest.support.util.DbUnitUtil;
import org.rhq.helpers.perftest.support.util.ExportedKeysSearchCallbackFilteredByPKs;

/**
 *
 * @author Lukas Krejci
 */
public class Exporter {

    private Exporter() {

    }

    public static void run(Properties settings, IDataSetConsumer consumer) throws Exception {
        IDatabaseConnection connection = DbUnitUtil.getConnection(settings);
        try {
            //get the list of the tables to load from the settings.
            //empty = all tables
            Map<String, String> tableQueries = getTableQueries(settings);

            PkTableMap pksToLoad = new PkTableMap();
            for (Map.Entry<String, String> entry : tableQueries.entrySet()) {
                String table = entry.getKey();
                String query = entry.getValue();

                SortedSet<Object> pks = getPksFromQuery(connection, table, query);
                pksToLoad.addAll(table, pks);
            }

            IDataSet data = null;

            if (pksToLoad.isEmpty()) {
                data = connection.createDataSet();
            } else {
                IDataSet dependingData = getDependingData(connection, pksToLoad);
                IDataSet dependentData = TablesDependencyHelper.getDataset(connection, pksToLoad);
                data = new CompositeDataSet(new IDataSet[] {dependingData, dependentData});
                System.err.println("rhq_resource_type depends on: " + Arrays.asList(TablesDependencyHelper.getDependsOnTables(connection, "rhq_resource_type")));
                System.err.println("Depending on rhq_resource_type: " + Arrays.asList(TablesDependencyHelper.getDependentTables(connection, "rhq_resource_type")));
            }

            ReplacementDataSet nullReplacingData = new ReplacementDataSet(data);
            nullReplacingData.addReplacementObject(null, Settings.NULL_REPLACEMENT);

            DataSetProducerAdapter producer = new DataSetProducerAdapter(nullReplacingData);
            producer.setConsumer(consumer);
            producer.produce();
        } finally {
            connection.close();
        }
    }

    private static SortedSet<Object> getPksFromQuery(IDatabaseConnection connection, String table, String query)
        throws DataSetException, SQLException {
        
        SortedSet<Object> ret = new TreeSet<Object>();

        if (query == null) {
            return ret;
        }

        IDataSet data = connection.createDataSet(new String[] { table });

        Column[] tablePks = data.getTableMetaData(table).getPrimaryKeys();

        if (tablePks.length > 1) {
            throw new UnsupportedOperationException(
                "Filtering on tables with multi-column primary key is not supported. Table '" + table
                    + "' has the following primary keys: " + Arrays.asList(tablePks));
        }

        String pkName = tablePks[0].getColumnName();

        //the connection shouldn't be closed here, because we're just reusing an already existing one.
        Connection jdbcConnection = connection.getConnection();

        Statement statement = null;
        try {
            statement = jdbcConnection.createStatement();
            ResultSet results = statement.executeQuery(query);
            
            while (results.next()) {
                Object pk = results.getObject(pkName);
    
                ret.add(pk);
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

        return ret;
    }

    private static Map<String, String> getTableQueries(Properties settings) {
        Map<String, String> ret = new HashMap<String, String>();

        for (Entry<Object, Object> entry : settings.entrySet()) {
            if (entry.getKey() instanceof String && (entry.getValue() == null || entry.getValue() instanceof String)) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                if (key.startsWith("table.")) {
                    String tableName = key.substring("table.".length());
                    String filterSql = value;
                    if (value != null && value.trim().isEmpty()) {
                        filterSql = null;
                    }

                    ret.put(tableName, filterSql);
                }
            }
        }

        return ret;
    }
    
    private static IDataSet getDependingData(IDatabaseConnection connection, PkTableMap rootTables) throws SearchException, DataSetException, SQLException {
        ExportedKeysSearchCallbackFilteredByPKs callback = new ExportedKeysSearchCallbackFilteredByPKs(connection, rootTables);
        ITableFilter filter = callback.getFilter();
        DepthFirstSearch search = new DepthFirstSearch();
        String[] tableNames = rootTables.getTableNames(); 
        ListOrderedSet tmpTables = search.search( tableNames, callback );
        String[] dependentTables  = CollectionsHelper.setToStrings( tmpTables );
        IDataSet tmpDataset = connection.createDataSet( dependentTables );
        FilteredDataSet dataset = new FilteredDataSet(filter, tmpDataset);
        return dataset;
    }
}
