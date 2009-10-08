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
package org.rhq.core.db.setup;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.SQLServerDatabaseType;

class Table {
    private DBSetup m_parent;
    private String m_strName;
    private boolean m_indexOrganized = false; // Index Organized Table
    private boolean m_parallel = false; // Parallel processing
    private boolean m_logging = false;
    private boolean m_cache = false;
    private String m_tableSpace;
    private String m_storage;
    private String m_engine;
    private List<Column> m_listColumns;
    private Collection<Index> m_collIndexes;
    private Collection<Constraint> m_collConstraints;
    private DataSet m_dataset;
    private boolean m_obsolete = false; // will be true if the table should no longer exist in the schema

    protected Table(Node node, DatabaseType dbtype, DBSetup dbsetup) throws SAXException {
        m_parent = dbsetup;

        if (Table.isTable(node)) {
            NamedNodeMap map = node.getAttributes();

            for (int iTab = 0; iTab < map.getLength(); iTab++) {
                Node nodeMap = map.item(iTab);

                if (nodeMap.getNodeName().equalsIgnoreCase("name")) {
                    this.m_strName = nodeMap.getNodeValue();
                } else if (nodeMap.getNodeName().equalsIgnoreCase("index-organized")) {
                    this.m_indexOrganized = nodeMap.getNodeValue().equalsIgnoreCase("true");
                } else if (nodeMap.getNodeName().equalsIgnoreCase("parallel")) {
                    this.m_parallel = nodeMap.getNodeValue().equalsIgnoreCase("true");
                } else if (nodeMap.getNodeName().equalsIgnoreCase("logging")) {
                    this.m_logging = nodeMap.getNodeValue().equalsIgnoreCase("true");
                } else if (nodeMap.getNodeName().equalsIgnoreCase("cache")) {
                    this.m_cache = nodeMap.getNodeValue().equalsIgnoreCase("true");
                } else if (nodeMap.getNodeName().equalsIgnoreCase("tablespace")) {
                    this.m_tableSpace = nodeMap.getNodeValue();
                } else if (nodeMap.getNodeName().equalsIgnoreCase("storage-options")) {
                    this.m_storage = nodeMap.getNodeValue();
                } else if (nodeMap.getNodeName().equalsIgnoreCase("engine")) {
                    this.m_engine = nodeMap.getNodeValue();
                } else if (nodeMap.getNodeName().equalsIgnoreCase("obsolete")) {
                    this.m_obsolete = (nodeMap.getNodeValue() != null)
                        && nodeMap.getNodeValue().equalsIgnoreCase("true");
                } else {
                    System.out.println("Unknown attribute \'" + nodeMap.getNodeName() + "\' in tag \'table\'");
                }
            }

            this.m_listColumns = Column.getColumns(node, this, dbtype);
            this.m_collIndexes = Index.getIndexes(this, node, dbtype);
            this.m_collConstraints = Constraint.getConstraints(this, node, dbtype);

            if (dbsetup.getDatabaseType() instanceof SQLServerDatabaseType) {
                // needs special pre and post handling for data set insertions
                this.m_dataset = new SQLServerXmlDataSet(this, node);
            } else {
                this.m_dataset = new XmlDataSet(this, node);
            }
        } else {
            throw new SAXException("node is not a table.");
        }
    }

    protected Table(ResultSet set, DatabaseMetaData meta, DBSetup dbsetup) throws SQLException {
        this.m_parent = dbsetup;
        this.m_strName = set.getString(3);
        this.m_listColumns = Column.getColumns(meta, this, dbsetup.getJdbcUser());
        if (dbsetup.getDatabaseType() instanceof SQLServerDatabaseType) {
            // needs special pre and post handling for data set insertions
            this.m_dataset = new SQLServerSqlDataSet(this);
        } else {
            this.m_dataset = new SqlDataSet(this);
        }
    }

    protected void create(Collection typemaps) throws SQLException {
        List collCmds = new java.util.Vector();
        this.getCreateCommands(collCmds, typemaps, m_parent.getDatabaseType());

        Iterator iter = collCmds.iterator();
        while (iter.hasNext()) {
            String strCmd = (String) iter.next();
            m_parent.doSQL(strCmd);
        }
    }

    private void doCmd(List collCmds) throws SQLException {
        Iterator iter = collCmds.iterator();
        while (iter.hasNext()) {
            String strCmd = (String) iter.next();
            m_parent.doSQL(strCmd);
        }
    }

    private void doCmdsWithoutAbortingOnErrors(List collCmds) throws SQLException {
        SQLException sqlException = null;
        Iterator cmdsIter = collCmds.iterator();
        while (cmdsIter.hasNext()) {
            String strCmd = (String) cmdsIter.next();
            try {
                m_parent.doSQL(strCmd);
            } catch (SQLException e) {
                if (sqlException == null) {
                    sqlException = e;
                } else {
                    sqlException.setNextException(e);
                    sqlException = e;
                }
            }
        }

        if (sqlException != null) {
            throw sqlException;
        }
    }

    protected void clear() throws SQLException {
        List collCmds = new ArrayList<String>();
        this.getClearCommands(collCmds);
        doCmd(collCmds);
    }

    protected void drop() throws SQLException {
        List collCmds = new ArrayList<String>();
        this.getDropCommands(collCmds);

        // *** NOTE *** we must execute all the DROPs no matter what, because although a
        //              table may not exist, sequences associated with it could still exist!
        doCmdsWithoutAbortingOnErrors(collCmds);
    }

    protected List getColumns() {
        return this.m_listColumns;
    }

    protected String getTableSpace() {
        return this.m_tableSpace;
    }

    protected String getStorage() {
        return this.m_storage;
    }

    protected String getEngine() {
        return this.m_engine;
    }

    protected boolean isObsolete() {
        return this.m_obsolete;
    }

    protected void getCreateCommands(List cmds, Collection typemaps, DatabaseType dbtype) {
        String strCmd = "CREATE TABLE " + this.getName() + " (";

        if (this.getName().length() > 30) {
            throw new RuntimeException("Column names must be at most 30 characters for oracle compatibility "
                + this.getName());
        }

        Iterator iter = this.getColumns().iterator();
        boolean bFirst = true;

        List<String> preCreateCommands = new ArrayList<String>();
        List<String> postCreateCommands = new ArrayList<String>();
        Column col;

        while (iter.hasNext()) {
            if (bFirst) {
                bFirst = false;
            } else {
                strCmd += ", ";
            }

            col = (Column) iter.next();
            col.getPreCreateCommands(preCreateCommands);
            strCmd += col.getCreateCommand(cmds, typemaps, dbtype);
            col.getPostCreateCommands(postCreateCommands);
        }

        // Add constraint decls
        iter = this.getConstraints().iterator();
        Constraint constraint;
        while (iter.hasNext()) {
            constraint = (Constraint) iter.next();
            strCmd += constraint.getCreateString();
            constraint.getPostCreateCommands(postCreateCommands);
        }

        strCmd += ')';

        if (this.m_indexOrganized) {
            strCmd += this.getIndexOrganizedSyntax(dbtype);
        }

        // deal with the special tablespace attr
        if ((this.m_tableSpace != null) && !this.m_tableSpace.equals("DEFAULT")
            && !this.getTableSpaceSyntax().equals("")) {
            strCmd += this.getTableSpaceSyntax() + this.getTableSpace();
        }

        // add a storage clause if any apply
        strCmd += this.getStorageClause();

        if (this.m_parallel) {
            strCmd += this.getParallelSyntax();
        }

        if (!this.m_logging) // Oracle does NOLOGGING
        {
            strCmd += this.getLoggingSyntax();
        }

        if (this.m_cache) {
            strCmd += this.getCacheSyntax();
        }

        cmds.addAll(preCreateCommands);
        cmds.add(strCmd);
        cmds.addAll(postCreateCommands);
    }

    /**
     * Get the storage clause for this table. Returns an empty string if it is not applicable
     *
     * @return the storage clause for this table
     */
    public String getStorageClause() {
        if ((this.m_storage != null) && !this.getStorageSyntax().equals("")) {
            return this.getStorageSyntax() + "(" + m_storage + ")";
        } else {
            return "";
        }
    }

    /**
     * Get the engine clause for this table. Returns an empty string if not applicable
     */
    public String getEngineClause() {
        //        if(this.m_engine != null && !this.getEngineSyntax().equals("")) {
        //            return this.getEngineSyntax() + "(" + m_engine + ")";
        //        }
        // FIXME hardcoded to all tables w/ same engine clause if engine is supported
        if (!this.getEngineSyntax().equals("")) {
            return this.getEngineSyntax();
        } else {
            return "";
        }
    }

    protected String getIndexOrganizedSyntax(DatabaseType dbtype) {
        return (DatabaseTypeFactory.isOracle(dbtype)) ? " ORGANIZATION INDEX" : "";
    }

    protected String getParallelSyntax() {
        return "";
    }

    protected String getLoggingSyntax() {
        return "";
    }

    protected String getCacheSyntax() {
        return "";
    }

    protected String getTableSpaceSyntax() {
        return "";
    }

    protected String getStorageSyntax() {
        return "";
    }

    protected String getEngineSyntax() {
        return "";
    }

    protected DataSet getDataSet() {
        return this.m_dataset;
    }

    protected void getClearCommands(List cmds) {
        String strCmd = "DELETE FROM " + this.getName();
        cmds.add(strCmd);
    }

    protected void getDropCommands(List cmds) {
        String strCmd = "DROP TABLE " + this.getName();
        cmds.add(strCmd);

        Iterator iter = this.getColumns().iterator();

        while (iter.hasNext()) {
            ((Column) iter.next()).getDropCommands(cmds);
        }
    }

    protected Collection<Constraint> getConstraints() {
        return this.m_collConstraints;
    }

    protected Collection<Index> getIndexes() {
        return this.m_collIndexes;
    }

    protected String getQueryCommand() {
        String strCmd = "SELECT ";

        Iterator iter = this.getColumns().iterator();

        while (iter.hasNext()) {
            strCmd += ((Column) iter.next()).getName();

            if (iter.hasNext()) {
                strCmd += ',';
            }

            strCmd += ' ';
        }

        strCmd = strCmd + "FROM " + this.getName();

        return strCmd;
    }

    protected String getName() {
        return this.m_strName;
    }

    protected static List<Table> getTables(Node node, DatabaseType dbtype, DBSetup parent) {
        List<Table> colResult = new ArrayList<Table>();
        NodeList listTabs = node.getChildNodes();

        for (int iTab = 0; iTab < listTabs.getLength(); iTab++) {
            Node nodeTab = listTabs.item(iTab);

            if (Table.isTable(nodeTab)) {
                //noinspection EmptyCatchBlock
                try {
                    if (DatabaseTypeFactory.isOracle(dbtype)) {
                        colResult.add(new OracleTable(nodeTab, dbtype, parent));
                    } else {
                        colResult.add(new Table(nodeTab, dbtype, parent));
                    }
                } catch (SAXException e) {
                }
            }
        }

        return colResult;
    }

    protected static Collection<Table> getTables(DatabaseType dbtype, DBSetup parent, String username)
        throws SQLException {
        if (DatabaseTypeFactory.isOracle(dbtype)) {
            return OracleTable.getTables(parent, username);
        }

        Collection<Table> coll = new ArrayList<Table>();
        DatabaseMetaData meta = parent.getConnection().getMetaData();
        String[] types = { "TABLE" };

        // there doesn't seem to be a general case for this but we
        // know this works for Oracle, so we'll start there
        ResultSet setTabs = meta.getTables(null, null, "%", types);

        // Find Tables
        while (setTabs.next()) {
            coll.add(new Table(setTabs, meta, parent));
        }

        return coll;
    }

    protected static boolean isTable(Node nodeTable) {
        return nodeTable.getNodeName().equalsIgnoreCase("table");
    }

    //  Can be overridden to do cleanup on an uninstall
    protected static void uninstallCleanup(DBSetup parent) throws SQLException {
    }

    protected DBSetup getDBSetup() {
        return m_parent;
    }
}