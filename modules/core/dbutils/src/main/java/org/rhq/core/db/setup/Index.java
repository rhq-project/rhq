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
import java.util.Vector;
import mazz.i18n.Msg;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

class Index {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String m_strName;
    private Collection<String> m_collFields;
    private Table m_table;
    private boolean m_isUnique;
    private DBSetup m_parent;
    private String m_tableSpace;
    private String m_condition;

    protected Index(Node node, Table table, DatabaseType dbtype) throws SAXException {
        m_parent = table.getDBSetup();

        // Bail out early if this is the wrong node (should never happen).
        if (!Index.isIndex(node)) {
            throw new SAXException("node is not an INDEX.");
        }

        NamedNodeMap map = node.getAttributes();

        for (int iAttr = 0; iAttr < map.getLength(); iAttr++) {
            Node nodeMap = map.item(iAttr);
            String strName = nodeMap.getNodeName();
            String strValue = nodeMap.getNodeValue();

            if (strName.equalsIgnoreCase("name")) {
                // Get the index Name
                this.m_strName = strValue;
            }

            if (strName.equalsIgnoreCase("unique")) {
                if (strValue.equalsIgnoreCase("true")) {
                    this.m_isUnique = true;
                } else if (strValue.equalsIgnoreCase("false")) {
                    this.m_isUnique = false;
                } else {
                    throw new SAXException("value of unique attribute on " + "INDEX element must be 'true' "
                        + "or 'false' (was '" + strValue + "')");
                }
            }

            if (strName.equalsIgnoreCase("tablespace")) {
                this.m_tableSpace = strValue;
            }

            if (strName.equalsIgnoreCase("condition")) {
                if (DatabaseTypeFactory.isPostgres(dbtype)) {
                    this.m_condition = strValue;
                } else {
                    System.out.println(MSG
                        .getMsg(DbAntI18NResourceKeys.INDEX_CONDITION_NOT_SUPPORTED, dbtype.getName()));
                }
            }
        }

        ///////////////////////////////////////////////////////////////////
        // Get the columns references in the index. This is not currently
        // verified to ensure they are valid.

        this.m_collFields = new ArrayList<String>();
        NodeList listFields = node.getChildNodes();

        for (int iField = 0; iField < listFields.getLength(); iField++) {
            node = listFields.item(iField);

            if (Index.isField(node)) {
                map = node.getAttributes();

                for (int iAttr = 0; iAttr < map.getLength(); iAttr++) {
                    Node nodeMap = map.item(iAttr);
                    String strName = nodeMap.getNodeName();
                    String strValue = nodeMap.getNodeValue();

                    if (strName.equalsIgnoreCase("name") || strName.equalsIgnoreCase("ref")) {
                        this.m_collFields.add(strValue);
                    }
                }
            }
        }

        this.m_table = table;
    }

    protected void create() throws SQLException {
        String strCmd = this.getCreateString();

        m_parent.doSQL(strCmd);
    }

    protected String getCreateString() {
        String strCmd;

        if (this.m_isUnique) {
            strCmd = "CREATE UNIQUE INDEX " + this.getName() + " ON " + this.getTable().getName() + " (";
        } else {
            strCmd = "CREATE INDEX " + this.getName() + " ON " + this.getTable().getName() + " (";
        }

        Iterator iter = this.getFields().iterator();
        boolean bFirst = true;

        while (iter.hasNext()) {
            if (bFirst) {
                bFirst = false;
            } else {
                strCmd += ", ";
            }

            String strField = (String) iter.next();

            strCmd += strField;
        }

        strCmd += ')';

        // deal with the tablespace
        strCmd += getTableSpaceClause();

        // now deal with storage attribute which is always inherited
        // from the table to ensure consistency
        // this may need to be changed later
        strCmd += m_table.getStorageClause();

        /* If we have a condition, add it  */
        if (m_condition != null) {
            strCmd += " WHERE " + m_condition;
        }

        return strCmd;
    }

    protected Collection getFields() {
        return this.m_collFields;
    }

    protected String getName() {
        return this.m_strName.toUpperCase();
    }

    protected Table getTable() {
        return this.m_table;
    }

    protected static Collection<Index> getIndexes(Table table, Node nodeTable, DatabaseType dbtype) {
        ///////////////////////////////////////////////////////////////
        // Get the Columns Names and Related Info

        String strTableName = nodeTable.getNodeName();
        NodeList listIdx = nodeTable.getChildNodes();
        Collection<Index> colResult = new Vector<Index>();

        for (int i = 0; i < listIdx.getLength(); i++) {
            Node node = listIdx.item(i);

            if (Index.isIndex(node)) {
                try {
                    if (DatabaseTypeFactory.isOracle(dbtype)) {
                        colResult.add(new OracleIndex(node, table, dbtype));
                    } else {
                        colResult.add(new Index(node, table, dbtype));
                    }
                } catch (SAXException e) {
                }
            }
        }

        return colResult;
    }

    protected String getTableSpaceClause() {
        if ((this.m_tableSpace != null) && !this.m_tableSpace.equals("DEFAULT") && !getTableSpaceSyntax().equals("")) {
            return getTableSpaceSyntax() + m_tableSpace;
        } else {
            return "";
        }
    }

    protected String getTableSpaceSyntax() {
        return "";
    }

    protected static boolean isField(Node node) {
        return node.getNodeName().equalsIgnoreCase("field");
    }

    protected static boolean isIndex(Node node) {
        return node.getNodeName().equalsIgnoreCase("index");
    }
}