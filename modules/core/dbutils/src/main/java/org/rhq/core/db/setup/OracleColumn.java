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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.rhq.core.db.DatabaseType;

class OracleColumn extends Column {
    protected OracleColumn(Node node, Table table) throws SAXException {
        super(node, table);
    }

    protected String getCreateCommand(List cmds, Collection typemaps, DatabaseType dbtype) {
        String defaultValue = this.getsDefault();
        if (defaultValue != null) {
            if (defaultValue.equalsIgnoreCase("TRUE")) {
                this.m_sDefault = "1";
            } else if (defaultValue.equalsIgnoreCase("FALSE")) {
                this.m_sDefault = "0";
            }
        }

        if ("CLOB".equals(this.getMappedType(typemaps, dbtype))) {
            // Oracle doesn't care about clob sizes... clear it out (it actually blows up)
            this.m_iSize = 0;
        }

        return super.getCreateCommand(cmds, typemaps, dbtype);
    }

    protected String getDefaultCommand(List cmds) {
        if (this.hasDefault()) {
            switch (this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY: {
                String strSeqName = this.m_strTableName.toUpperCase() + '_' + this.getName().toUpperCase() + "_SEQ";
                cmds.add(0, "CREATE SEQUENCE " + strSeqName + " START WITH " + this.getInitialSequence()
                    + " INCREMENT BY " + this.getIncrementSequence() + " NOMAXVALUE NOCYCLE CACHE 10");
                break;
            }
            }
        }

        return "";
    }

    protected void getPostCreateCommands(List cmds) {
        if (this.hasDefault()) {
            switch (this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT: {
                String strSeqName = this.m_strTableName.toUpperCase() + '_' + this.getName().toUpperCase() + "_SEQ";
                cmds.add("CREATE OR REPLACE TRIGGER " + strSeqName + "_T " + "BEFORE INSERT ON " + this.m_strTableName
                    + " " + "FOR EACH ROW " + "BEGIN " + "SELECT " + strSeqName + ".NEXTVAL INTO :NEW."
                    + this.getName().toUpperCase() + " FROM DUAL; " + "END;");
                break;
            }
            }
        }
    }

    protected void getDropCommands(List cmds) {
        if (this.hasDefault()) {
            switch (this.getDefault()) {
            case Column.DEFAULT_SEQUENCE_ONLY:
            case Column.DEFAULT_AUTO_INCREMENT: {
                String strSeqName = this.m_strTableName.toUpperCase() + '_' + this.getName().toUpperCase() + "_SEQ";
                cmds.add("DROP SEQUENCE " + strSeqName);

                // Dropping the table automatically drops the sequence
                // before this command gets executed.
                //-- you must mean it drops the trigger automatically, yea?
                //cmds.add("DROP TRIGGER " + strSeqName + "_t");

                break;
            }
            }
        }
    }
}