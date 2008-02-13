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

import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.rhq.core.db.DatabaseType;

class ConstraintImpl_FK implements ConstraintImpl {
    private String _name = null;
    private Constraint _constraint = null;
    private DatabaseType _dbtype = null;
    private String _local = null;
    private String _references = null;
    private String _onDelete = null;

    public ConstraintImpl_FK(String name, Constraint constraint, DatabaseType dbtype, Node node) throws SAXException {
        _name = name;
        _constraint = constraint;
        _dbtype = dbtype;

        NamedNodeMap attrs = node.getAttributes();
        int numAttrs = attrs.getLength();
        for (int iAttr = 0; iAttr < numAttrs; iAttr++) {
            node = attrs.item(iAttr);
            if (node.getNodeName().equalsIgnoreCase("local")) {
                _local = node.getNodeValue();
            } else if (node.getNodeName().equalsIgnoreCase("references")) {
                _references = node.getNodeValue();
            } else if (node.getNodeName().equalsIgnoreCase("ondelete")) {
                _onDelete = node.getNodeValue();
            } else {
                throw new SAXException("Unrecognized attribute in ForeignKey element: " + node.getNodeName());
            }
        }
    }

    public void getPostCreateCommands(List postCreateCommands) {
    }

    public String getCreateString() {
        if (_name == null) {
            _name = "";
        }

        if (_onDelete == null) {
            _onDelete = "";
        } else {
            _onDelete = "ON DELETE " + _onDelete.toUpperCase();
        }

        return ", CONSTRAINT " + _name + " FOREIGN KEY (" + _local + ") REFERENCES " + _references + " " + _onDelete;
    }
}