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

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.rhq.core.db.DatabaseType;

class Constraint {
    private Table m_table;
    private ConstraintImpl m_constraint;
    private DBSetup m_parent;

    protected Constraint(Node node, Table table, DatabaseType dbtype) throws SAXException {
        m_parent = table.getDBSetup();
        if (!Constraint.isConstraint(node)) {
            throw new SAXException("node is not an CONSTRAINT.");
        }

        String cname = null;

        NamedNodeMap map = node.getAttributes();

        for (int iAttr = 0; iAttr < map.getLength(); iAttr++) {
            Node nodeMap = map.item(iAttr);
            String strName = nodeMap.getNodeName();
            String strValue = nodeMap.getNodeValue();

            if (strName.equalsIgnoreCase("name")) {
                cname = strValue;
            }
        }

        ///////////////////////////////////////////////////////////////////
        // Get the columns references in the constraint. This is not currently
        // verified to ensure they are valid.
        NodeList listFields = node.getChildNodes();
        int numFields = listFields.getLength();

        for (int iField = 0; iField < numFields; iField++) {
            node = listFields.item(iField);
            if (node.getNodeName().equalsIgnoreCase("primarykey")) {
                m_constraint = new ConstraintImpl_PK(cname, dbtype, node);
            } else if (node.getNodeName().equalsIgnoreCase("foreignkey")) {
                m_constraint = new ConstraintImpl_FK(cname, this, dbtype, node);
            }
        }

        this.m_table = table;
    }

    protected String getCreateString() {
        return m_constraint.getCreateString();
    }

    protected Table getTable() {
        return m_table;
    }

    protected void getPostCreateCommands(List postCreateCommands) {
        m_constraint.getPostCreateCommands(postCreateCommands);
    }

    protected static Collection<Constraint> getConstraints(Table table, Node nodeTable, DatabaseType dbtype) {
        NodeList listIdx = nodeTable.getChildNodes();
        Collection<Constraint> colResult = new Vector<Constraint>();

        for (int i = 0; i < listIdx.getLength(); i++) {
            Node node = listIdx.item(i);

            if (Constraint.isConstraint(node)) {
                try {
                    colResult.add(new Constraint(node, table, dbtype));
                } catch (SAXException e) {
                }
            }
        }

        return colResult;
    }

    protected static boolean isConstraint(Node node) {
        return node.getNodeName().equalsIgnoreCase("constraint");
    }
}