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

class View {
    private DBSetup m_parent;
    private String m_strName;
    private String m_strQuery;
    private DataSet m_dataset;

    protected View(Node node, DatabaseType dbtype, DBSetup dbsetup) throws SAXException {
        m_parent = dbsetup;
        boolean queryIsSet = false;

        if (View.isView(node)) {
            NamedNodeMap map = node.getAttributes();

            for (int iTab = 0; iTab < map.getLength(); iTab++) {
                Node nodeMap = map.item(iTab);

                if (nodeMap.getNodeName().equalsIgnoreCase("name")) {
                    this.m_strName = nodeMap.getNodeValue();
                } else {
                    System.out.println("Unknown attribute '" + nodeMap.getNodeName() + "' in tag 'table'");
                }
            }

            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeName().equalsIgnoreCase("query") && child.hasChildNodes()) {
                    this.m_strQuery = child.getFirstChild().getNodeValue();
                    queryIsSet = true;
                }
            }

            if (!queryIsSet) {
                throw new SAXException("no query specified");
            }
        } else {
            throw new SAXException("node is not a table.");
        }
    }

    /**
     * protected View(ResultSet set, DatabaseMetaData meta, DBSetup dbsetup) throws SQLException { this.m_parent =
     * dbsetup; this.m_strName = set.getString(3); this.m_listColumns = Column.getColumns(meta, this); this.m_dataset =
     * new SqlDataSet(this); }
     */

    protected void create(Collection typemaps) throws SQLException {
        List commands = new java.util.Vector();
        this.getCreateCommands(commands, typemaps, m_parent.getDatabaseType());
        Iterator iter = commands.iterator();
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

    protected void drop() throws SQLException {
        List<String> collCmds = new ArrayList<String>();
        this.getDropCommands(collCmds);

        doCmd(collCmds);
    }

    protected void getCreateCommands(List cmds, Collection typemaps, DatabaseType dbtype) {
        String strCmd = "CREATE VIEW " + this.getName() + " AS " + this.getQuery();

        cmds.add(0, strCmd);
    }

    protected DataSet getDataSet() {
        return this.m_dataset;
    }

    protected void getDropCommands(List cmds) {
        String strCmd = "DROP VIEW " + this.getName();
        cmds.add(strCmd);
    }

    protected String getQueryCommand() {
        String strCmd = "SELECT * ";

        strCmd = strCmd + "FROM " + this.getName();

        return strCmd;
    }

    protected String getName() {
        return this.m_strName.toUpperCase();
    }

    protected String getQuery() {
        return this.m_strQuery;
    }

    protected static List<View> getViews(Node node, DatabaseType dbtype, DBSetup parent) {
        List<View> colResult = new ArrayList<View>();
        NodeList listViews = node.getChildNodes();

        for (int i = 0; i < listViews.getLength(); i++) {
            Node nodeView = listViews.item(i);

            if (View.isView(nodeView)) {
                try {
                    colResult.add(new View(nodeView, dbtype, parent));
                } catch (SAXException e) {
                }
            }
        }

        return colResult;
    }

    protected static boolean isView(Node nodeTable) {
        return nodeTable.getNodeName().equalsIgnoreCase("view");
    }

    protected DBSetup getDBSetup() {
        return m_parent;
    }

    protected static void uninstallCleanup(DBSetup parent) throws SQLException {
    }
}