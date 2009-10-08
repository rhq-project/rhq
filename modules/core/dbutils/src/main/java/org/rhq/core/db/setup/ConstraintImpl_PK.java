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
import java.util.Vector;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.rhq.core.db.DatabaseType;

class ConstraintImpl_PK implements ConstraintImpl {
    private String _name = null;
    private DatabaseType _dbtype = null;
    private List<String> _fields = null;

    public ConstraintImpl_PK(String name, DatabaseType dbtype, Node node) throws SAXException {
        _name = name;
        _dbtype = dbtype;
        _fields = new Vector<String>();

        NamedNodeMap attrs;

        NodeList listFields = node.getChildNodes();
        int numFields = listFields.getLength();
        for (int iField = 0; iField < numFields; iField++) {
            node = listFields.item(iField);
            if (node.getNodeName().equalsIgnoreCase("field")) {
                attrs = node.getAttributes();
                if (attrs.getLength() != 1) {
                    throw new SAXException("Primary key field element must have only 1 attribute and it must be 'ref'");
                }

                Node refAttr = attrs.item(0);
                if (!refAttr.getNodeName().equalsIgnoreCase("ref")) {
                    throw new SAXException("Primary key field element must have only 1 attribute and it must be 'ref'");
                }

                _fields.add(refAttr.getNodeValue());
            }
        }
    }

    public void getPostCreateCommands(List postCreateCommands) {
    }

    public String getCreateString() {
        if (_name == null) {
            _name = "";
        }

        return ", CONSTRAINT " + _name + " PRIMARY KEY (" + listItems(_fields) + ")";
    }

    private String listItems(List<String> list) {
        StringBuffer ret_items = new StringBuffer();

        if (list != null) {
            for (String string : list) {
                if (ret_items.length() > 0) {
                    ret_items.append(", ");
                }

                ret_items.append(string);
            }
        }

        return ret_items.toString();
    }
}