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

package org.rhq.helpers.perftest.support.dbunit;

import java.io.StringReader;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.PrimaryKeyFilteredTableWrapper;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.filter.AbstractTableFilter;
import org.rhq.helpers.perftest.support.config.Entity;
import org.rhq.helpers.perftest.support.config.Relationship;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;
import org.rhq.helpers.perftest.support.jpa.ColumnValues;
import org.rhq.helpers.perftest.support.jpa.ConfigurableDependencyInclusionResolver;
import org.rhq.helpers.perftest.support.jpa.DependencyInclusionResolver;
import org.rhq.helpers.perftest.support.jpa.DependencyType;
import org.rhq.helpers.perftest.support.jpa.EagerMappingInclusionResolver;
import org.rhq.helpers.perftest.support.jpa.Edge;
import org.rhq.helpers.perftest.support.jpa.EntityDependencyGraph;
import org.rhq.helpers.perftest.support.jpa.Node;
import org.rhq.helpers.perftest.support.jpa.mapping.ColumnValuesTableMap;
import org.rhq.helpers.perftest.support.jpa.mapping.EntityTranslation;
import org.rhq.helpers.perftest.support.jpa.mapping.RelationshipTranslation;

/**
 *
 * @author Lukas Krejci
 */
public class EntityRelationshipFilter extends AbstractTableFilter {

    private IDatabaseConnection connection;

    private EntityDependencyGraph edg;
    private Map<String, Node> tableToNode = new HashMap<String, Node>();
    private Map<String, Edge> relationTableToEdge = new HashMap<String, Edge>();
    private Map<Class<?>, Set<ColumnValues>> primaryPks = new HashMap<Class<?>, Set<ColumnValues>>();
    private DependencyInclusionResolver inclusionResolver;

    public EntityRelationshipFilter(IDatabaseConnection connection, Map<Class<?>, Set<ColumnValues>> allowedPks,
        DependencyInclusionResolver inclusionResolver) {
        this.connection = connection;
        this.inclusionResolver = inclusionResolver;

        edg = new EntityDependencyGraph();
        edg.addEntities(allowedPks.keySet());

        primaryPks.putAll(allowedPks);
    }

    public boolean isValidName(String tableName) throws DataSetException {
        return tableToNode.get(tableName) != null || relationTableToEdge.get(tableName) != null;
    }

    public ITableIterator iterator(IDataSet dataSet, boolean reversed) throws DataSetException {
        //TODO iterate over the tables in the correct order from the roots of the dependency graph down.
        //TODO actually do the PK filtering.

        try {
            ColumnValuesTableMap resolvedPks = new ColumnValuesTableMap();
            resolvePks(primaryPks, resolvedPks, true);

            return new EntityRelationshipTableIterator(reversed ? dataSet.reverseIterator() : dataSet.iterator(),
                resolvedPks);
        } catch (SQLException e) {
            throw new DataSetException("Primary keys resolution failed during dataset inspection.", e);
        }
    }

    private void resolvePks(Map<Class<?>, Set<ColumnValues>> primaryPks, ColumnValuesTableMap resolvedPks,
        boolean resolveDependents) throws SQLException {
        for (Map.Entry<Class<?>, Set<ColumnValues>> entry : primaryPks.entrySet()) {
            Node node = edg.getNode(entry.getKey());
            Set<ColumnValues> pks = entry.getValue();

            //check that the pk columns have defined names from the user
            if (pks != null) {
                for (ColumnValues pk : pks) {
                    int idx = 0;
                    for (ColumnValues.Column col : pk) {
                        if (col.getName() == null) {
                            col.setName(node.getTranslation().getPkColumns()[idx]);
                        }

                        idx++;
                    }
                }
            }
            
            resolvePks(node, pks, resolvedPks);
        }
    }

    private void resolvePks(Node node, Set<ColumnValues> nodePks, ColumnValuesTableMap resolvedPks) throws SQLException {
        Set<ColumnValues> unresolvedPks;

        Set<ColumnValues> resolvedTablePks = resolvedPks.get(node.getTranslation().getTableName());

        //determine whether to bale out...
        if (resolvedPks.containsKey(node.getTranslation().getTableName())) {
            if (resolvedTablePks == null) {
                //yes, this table has been identified as "include all"
                return;
            }
            
            if (nodePks == null) {
                //there is an entry for this table in the resolved pks already and we're
                //telling it to include everything... let's leave what's in the resolution
                //already and quit.
                return;
            }
        }

        if (resolvedTablePks == null || resolvedTablePks.isEmpty()) {
            unresolvedPks = nodePks;
        } else {
            unresolvedPks = new HashSet<ColumnValues>();
            for (ColumnValues pk : nodePks) {
                if (!resolvedTablePks.contains(pk)) {
                    unresolvedPks.add(pk);
                }
            }
        }

        if (unresolvedPks != null) {
            if (!unresolvedPks.isEmpty()) {
                resolvedPks.getOrCreate(node.getTranslation().getTableName()).addAll(unresolvedPks);
            } else {
                //there are no data to include for this table. bale out.
                return;
            }
        } else {
            resolvedPks.put(node.getTranslation().getTableName(), null);
        }

        for (Edge e : node.getEdges()) {
            if (e.getFrom() == node) {
                //only include the dependents if the relationship
                //is actually defined on the entity (i.e. don't include
                //"back-references", like combined @JoinColumn @ManyToOne defined only on the target
                //entity
                if (e.getFromField() != null && inclusionResolver.isValid(e)) {
                    Set<ColumnValues> dependentPks = resolveDependentPks(e, unresolvedPks, resolvedPks);
                    resolvePks(e.getTo(), dependentPks, resolvedPks);
                } else {
                    //add nothing or create a new record for this table
                    //this will mark it as "done"
                    resolvedPks.getOrCreate(e.getTo().getTranslation().getTableName());
                }
            } else {
                if (e.getToField() != null) {
                    Set<ColumnValues> dependingPks = resolveDependingPks(e, unresolvedPks, resolvedPks);
                    resolvePks(e.getFrom(), dependingPks, resolvedPks);
                } else {
                    resolvedPks.getOrCreate(e.getFrom().getTranslation().getTableName());
                }
            }
        }
    }

    private Set<ColumnValues>
        resolveDependentPks(Edge edge, Set<ColumnValues> fromPks, ColumnValuesTableMap resolvedPks) throws SQLException {

        RelationshipTranslation translation = edge.getTranslation();

        if (translation.getRelationTable() != null) {
            //copy the fromPks to columnValues. We'll use the pks from the from table
            //to find the corresponding entries in the relation table
            Set<ColumnValues> columnValues = null;

            if (fromPks != null) {
                columnValues = new HashSet<ColumnValues>();
                for (ColumnValues pk : fromPks) {
                    columnValues.add(pk.clone());
                }

                //now change the names of the columns in columnValues to the corresponding
                //relationTableFromColumns (this assumes the same order of the columns
                //in the case of composite pk)
                for (int i = 0; i < translation.getRelationTableFromColumns().length; ++i) {
                    for (ColumnValues cols : columnValues) {
                        cols.getColumns().get(i).setName(translation.getRelationTableFromColumns()[i]);
                    }
                }
            }

            String[] fromAndToCols = new String[translation.getRelationTableFromColumns().length
                + translation.getRelationTableToColumns().length];
            System.arraycopy(translation.getRelationTableFromColumns(), 0, fromAndToCols, 0,
                translation.getRelationTableFromColumns().length);
            System.arraycopy(translation.getRelationTableToColumns(), 0, fromAndToCols,
                translation.getRelationTableFromColumns().length, translation.getRelationTableToColumns().length);

            if (fromPks != null) {
                Set<ColumnValues> fromAndToValues = getValuesFromTable(translation.getRelationTable(), fromAndToCols,
                    columnValues);

                //add the relation table to the resolvedPks using fromAndToValues as its primary keys
                resolvedPks.getOrCreate(translation.getRelationTable()).addAll(fromAndToValues);

                //now read out the to pks from fromAndToCols are return them as the "to" table primary keys
                Set<ColumnValues> toPks = new HashSet<ColumnValues>();
                for (ColumnValues cols : fromAndToValues) {
                    ColumnValues toPk = new ColumnValues();
                    for (int i = 0; i < translation.getRelationTableToColumns().length; ++i) {
                        String colName = translation.getRelationTableToColumns()[i];
                        String pkName = edge.getTo().getTranslation().getPkColumns()[i];

                        toPk.add(pkName, cols.getColumnByName(colName).getValue());
                    }
                    toPks.add(toPk);
                }

                return removeValuesWithNullColumn(toPks);
            } else {
                resolvedPks.put(translation.getRelationTable(), null);
                return null;
            }
        } else {
            if (fromPks == null) {
                return null;
            }

            //get the values of the "fromColumns" of the relation from the "from" table
            Set<ColumnValues> columnValues = getValuesFromTable(edge.getFrom().getTranslation().getTableName(),
                translation.getFromColumns(), fromPks);

            //now change the names of the columns in columnValues to correspond to the ones
            //in the "to" table (this assumes that the columns in fromColumns and toColumns
            //correspond to each other by position)
            for (int i = 0; i < translation.getToColumns().length; ++i) {
                for (ColumnValues cols : columnValues) {
                    cols.getColumns().get(i).setName(translation.getToColumns()[i]);
                }
            }

            Set<ColumnValues> ret = getValuesFromTable(edge.getTo().getTranslation().getTableName(), edge.getTo()
                .getTranslation().getPkColumns(), columnValues);

            return removeValuesWithNullColumn(ret);
        }
    }

    private Set<ColumnValues> resolveDependingPks(Edge edge, Set<ColumnValues> toPks, ColumnValuesTableMap resolvedPks)
        throws SQLException {

        RelationshipTranslation translation = edge.getTranslation();

        if (translation.getRelationTable() == null) {
            if (toPks == null) {
                return null;
            }

            //get the foreign keys in the "to" table
            Set<ColumnValues> columnValues = getValuesFromTable(edge.getTo().getTranslation().getTableName(),
                translation.getToColumns(), toPks);

            //now rename the foreign keys to their primary key counterparts in the "from" table
            for (int i = 0; i < translation.getFromColumns().length; ++i) {
                for (ColumnValues cols : columnValues) {
                    cols.getColumns().get(i).setName(translation.getFromColumns()[i]);
                }
            }

            return removeValuesWithNullColumn(columnValues);
        } else {
            //only bother with one-to-many relationships. A many-to-many
            //relationship implicitly means that the two entities are not tightly
            //connected (with a many-to-many relationship, either of the entities
            //can always "live without" the entities from the other side of the relationship).
            if (edge.getDependencyType() != DependencyType.MANY_TO_MANY) {
                //copy the toPks to columnValues. We'll use the pks from the to table
                //to find the corresponding entries in the relation table
                Set<ColumnValues> columnValues = null;

                if (toPks != null) {
                    columnValues = new HashSet<ColumnValues>();
                    for (ColumnValues pk : toPks) {
                        columnValues.add(pk.clone());
                    }

                    //now change the names of the columns in columnValues to the corresponding
                    //relationTableToColumns (this assumes the same order of the columns
                    //in the case of composite pk)
                    for (int i = 0; i < translation.getRelationTableToColumns().length; ++i) {
                        for (ColumnValues cols : columnValues) {
                            cols.getColumns().get(i).setName(translation.getRelationTableToColumns()[i]);
                        }
                    }
                }

                String[] fromAndToCols = new String[translation.getRelationTableFromColumns().length
                    + translation.getRelationTableToColumns().length];
                System.arraycopy(translation.getRelationTableFromColumns(), 0, fromAndToCols, 0,
                    translation.getRelationTableFromColumns().length);
                System.arraycopy(translation.getRelationTableToColumns(), 0, fromAndToCols,
                    translation.getRelationTableFromColumns().length, translation.getRelationTableToColumns().length);

                if (toPks != null) {
                    Set<ColumnValues> fromAndToValues = getValuesFromTable(translation.getRelationTable(),
                        fromAndToCols, columnValues);

                    //add the relation table to the resolvedPks using fromAndToValues as its primary keys
                    resolvedPks.getOrCreate(translation.getRelationTable()).addAll(fromAndToValues);

                    //now read out the to pks from fromAndToCols are return them as the "from" table primary keys
                    Set<ColumnValues> fromPks = new HashSet<ColumnValues>();
                    for (ColumnValues cols : fromAndToValues) {
                        ColumnValues fromPk = new ColumnValues();
                        for (int i = 0; i < translation.getRelationTableFromColumns().length; ++i) {
                            String colName = translation.getRelationTableFromColumns()[i];
                            String pkName = edge.getFrom().getTranslation().getPkColumns()[i];
                            fromPk.add(pkName, cols.getColumnByName(colName).getValue());
                        }
                        fromPks.add(fromPk);
                    }

                    return removeValuesWithNullColumn(fromPks);
                } else {
                    resolvedPks.put(translation.getRelationTable(), null);
                    return null;
                }
            } else {
                //put no restrictions on the search if the toPks are null (unrestricted)
                //otherwise pretend there's nothing depending.
                return toPks == null ? null : new HashSet<ColumnValues>();
            }
        }
    }

    /**
     * @param columnValues
     * @return
     */
    private static Set<ColumnValues> removeValuesWithNullColumn(Set<ColumnValues> columnValues) {
        Set<ColumnValues> ret = new HashSet<ColumnValues>();

        for (ColumnValues cols : columnValues) {
            boolean add = true;
            for (ColumnValues.Column c : cols) {
                if (c.getValue() == null) {
                    add = false;
                    break;
                }
            }
            if (add) {
                ret.add(cols);
            }
        }

        return ret;
    }

    private static String colNamesToSql(String[] colNames) {
        StringBuilder bld = new StringBuilder();

        if (colNames.length == 0)
            return "";

        for (String col : colNames) {
            bld.append(", ").append(col);
        }

        return bld.substring(1);
    }

    private Set<ColumnValues> getValuesFromTable(String tableName, String[] valueColumns,
        Set<ColumnValues> knownlColumns) throws SQLException {
        //I know, doing this one by one is super lame, but prevents the 1000 IN clause members limit of Oracle
        StringBuilder sqlCommon = new StringBuilder("SELECT ").append(colNamesToSql(valueColumns)).append(" FROM ")
            .append(tableName).append(" WHERE ");

        Set<ColumnValues> ret = new HashSet<ColumnValues>();

        for (ColumnValues cols : knownlColumns) {
            StringBuilder sql = new StringBuilder(sqlCommon);
            for (ColumnValues.Column c : cols) {
                sql.append(c.getName()).append(" = ? AND ");
            }

            sql.replace(sql.length() - 4, sql.length(), "");

            PreparedStatement st = null;
            try {
                st = connection.getConnection().prepareStatement(sql.toString());
                int idx = 1;
                for (ColumnValues.Column c : cols) {
                    st.setObject(idx++, c.getValue());
                }

                ResultSet rs = st.executeQuery();

                ResultSetMetaData rsmd = rs.getMetaData();

                while (rs.next()) {
                    ColumnValues vals = new ColumnValues();

                    for (int i = 1; i <= rsmd.getColumnCount(); ++i) {
                        String columnName = rsmd.getColumnName(i);
                        Object value = rs.getObject(i);
                        vals.add(columnName, value);
                    }

                    ret.add(vals);
                }
            } finally {
                if (st != null) {
                    st.close();
                }
            }
        }

        return ret;
    }

    public static void main(String[] args) throws Exception {
        HashMap<Class<?>, Set<ColumnValues>> allowed = new HashMap<Class<?>, Set<ColumnValues>>();
        allowed.put(Class.forName("org.rhq.core.domain.resource.Resource"), ColumnValues.setOf(10001));

        Class.forName("org.postgresql.Driver");
        DatabaseConnection con = new DatabaseConnection(DriverManager.getConnection(
            "jdbc:postgresql://127.0.0.1:5432/rhqdev", "rhqadmin", "rhqadmin"));

//        EntityRelationshipFilter f = new EntityRelationshipFilter(con, allowed, new EagerMappingInclusionResolver());
//        System.out.println(f.edg);
//
//        f.iterator(con.createDataSet(), false);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<graph packagePrefix=\"org.rhq.core.domain\">\n"
            + "    <entity name=\"resource.Resource\" includeAllDependents=\"true\" />\n" + "</graph>";

        JAXBContext c = ExportConfiguration.getJAXBContext();
        Unmarshaller um = c.createUnmarshaller();
        ExportConfiguration edg = (ExportConfiguration) um.unmarshal(new StringReader(xml));

        EntityRelationshipFilter f2 = new EntityRelationshipFilter(con, allowed,
            new ConfigurableDependencyInclusionResolver(edg));
        f2.iterator(con.createDataSet(), false);
    }
}
