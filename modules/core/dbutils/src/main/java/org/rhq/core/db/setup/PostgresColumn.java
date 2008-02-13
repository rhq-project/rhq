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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class PostgresColumn extends Column {
    protected PostgresColumn(Node node, Table table) throws SAXException {
        super(node, table);
    }

    protected String getDefaultCommand(List cmds) {
        if (m_strType.equalsIgnoreCase("auto") || (m_iDefault == Column.DEFAULT_SEQUENCE_ONLY)) {
            return "";
        }

        String strSeqName = this.m_strTableName.toUpperCase() + '_' + this.getName().toUpperCase() + "_SEQ";

        return "DEFAULT nextval('" + strSeqName + "')";
    }

    protected void getPreCreateCommands(List cmds) {
        if (m_strType.equalsIgnoreCase("auto")) {
            return;
        }

        if (hasDefault()) {
            switch (getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY: {
                String strSeqName = this.m_strTableName.toUpperCase() + '_' + this.getName().toUpperCase() + "_SEQ";
                cmds.add(0, "CREATE SEQUENCE " + strSeqName + " START " + this.getInitialSequence() + " INCREMENT "
                    + this.getIncrementSequence() + " CACHE 10");
                break;
            }
            }
        }
    }

    protected void getDropCommands(List cmds) {
        if (this.hasDefault()) {
            switch (this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY: {
                String strSeqName = this.m_strTableName.toUpperCase() + '_' + this.getName().toUpperCase() + "_SEQ";
                cmds.add("DROP SEQUENCE " + strSeqName);
                break;
            }
            }
        }
    }
}