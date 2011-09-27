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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.stream.DataSetProducerAdapter;
import org.dbunit.dataset.stream.IDataSetConsumer;

import org.rhq.helpers.perftest.support.config.Entity;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;
import org.rhq.helpers.perftest.support.dbunit.DbUnitUtil;
import org.rhq.helpers.perftest.support.dbunit.EntityRelationshipFilter;
import org.rhq.helpers.perftest.support.jpa.ColumnValues;
import org.rhq.helpers.perftest.support.jpa.ConfigurableDependencyInclusionResolver;
import org.rhq.helpers.perftest.support.jpa.mapping.MappingTranslator;

/**
 * A utility class to run an export.
 *
 * @author Lukas Krejci
 */
public class Exporter {

    private Exporter() {

    }

    /**
     * Runs the export using given export configuration and supplies the data to the provided
     * consumer (which should convert it to some kind of output).
     *
     * @param config
     * @param consumer
     * @throws Exception
     */
    public static void run(ExportConfiguration config, IDataSetConsumer consumer) throws Exception {
        IDatabaseConnection connection = DbUnitUtil.getConnection(config.getSettings());
        try {
            Map<Entity, String> entityQueries = getEntityQueries(config);

            Map<Class<?>, Set<ColumnValues>> pksToLoad = new HashMap<Class<?>, Set<ColumnValues>>();
            for (Map.Entry<Entity, String> entry : entityQueries.entrySet()) {
                Entity entity = entry.getKey();
                String query = entry.getValue();

                String tableName = MappingTranslator.getTableName(config.getClassForEntity(entity));

                Set<ColumnValues> pks = getPksFromQuery(connection, tableName, query);
                pksToLoad.put(config.getClassForEntity(entity), pks);
            }

            IDataSet data = null;

            if (pksToLoad.isEmpty()) {
                data = connection.createDataSet();
            } else {
                EntityRelationshipFilter filter = new EntityRelationshipFilter(connection, pksToLoad, new ConfigurableDependencyInclusionResolver(config));
                data = new FilteredDataSet(filter, connection.createDataSet());
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

    private static Set<ColumnValues> getPksFromQuery(IDatabaseConnection connection, String table, String query)
        throws DataSetException, SQLException {

        Set<ColumnValues> ret = new HashSet<ColumnValues>();

        IDataSet data = connection.createDataSet(new String[] { table });
        Column[] tablePks = data.getTableMetaData(table).getPrimaryKeys();
        String pkName = tablePks[0].getColumnName();

        if (query == null) {
            query = "SELECT " + pkName + " FROM " + table;
        }

        //the connection shouldn't be closed here, because we're just reusing an already existing one.
        Connection jdbcConnection = connection.getConnection();

        Statement statement = null;
        ResultSet results = null;
        try {
            statement = jdbcConnection.createStatement();
            results = statement.executeQuery(query);

            while (results.next()) {
                ColumnValues pks = new ColumnValues();
                for(Column pk : tablePks) {
                    Object pkVal = results.getObject(pkName);
                    pks.add(pk.getColumnName(), pkVal);
                }

                ret.add(pks);
            }
        } finally {
            if (results!=null) {
                try {
                    results.close();
                } catch (SQLException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }
            }
        }

        return ret;
    }

    private static Map<Entity, String> getEntityQueries(ExportConfiguration config) {
        Map<Entity, String> ret = new HashMap<Entity, String>();

        for (Entity e : config.getEntities()) {
            if (e.isRoot()) {
                ret.put(e, e.getFilter());
            }
        }

        return ret;
    }
}
