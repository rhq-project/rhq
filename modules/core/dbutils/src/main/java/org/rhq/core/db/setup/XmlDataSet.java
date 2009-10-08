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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class XmlDataSet extends DataSet {
    private Iterator m_iterator;
    private List m_listCurRow;

    protected XmlDataSet(Table table, Node nodeTable) {
        super(table.getName(), table.getDBSetup());

        Collection<List<Data>> collRows = new ArrayList<List<Data>>();
        NodeList listData = nodeTable.getChildNodes();

        for (int i = 0; i < listData.getLength(); i++) {
            Node node = listData.item(i);

            if (XmlDataSet.isDataSet(node)) {
                List<Data> listRow = new ArrayList<Data>();

                NamedNodeMap map = node.getAttributes();

                for (int iAttr = 0; iAttr < map.getLength(); iAttr++) {
                    listRow.add(new Data(map.item(iAttr)));
                }

                collRows.add(listRow);
            }
        }

        this.m_iterator = collRows.iterator();
    }

    protected int getNumberColumns() {
        return this.m_listCurRow == null ? 0 : this.m_listCurRow.size();
    }

    protected Data getData(int columnIndex) {
        return (Data) this.m_listCurRow.get(columnIndex);
    }

    protected boolean next() {
        boolean bResult;

        bResult = this.m_iterator.hasNext();

        if (bResult) {
            this.m_listCurRow = (List) this.m_iterator.next();
        } else {
            this.m_listCurRow = null;
        }

        return bResult;
    }

    protected static boolean isDataSet(Node node) {
        String strTmp = node.getNodeName();
        return strTmp.equalsIgnoreCase("data");
    }
}