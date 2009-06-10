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

/**
 * @author Joseph Marques
 */
class SQLServerColumn extends Column {
    protected SQLServerColumn(Node node, Table table) throws SAXException {
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

        return super.getCreateCommand(cmds, typemaps, dbtype);
    }

    /* 
     * Dual cascade path to RHQ_CONFIG_PROPERTY causes constraint creation errors on SQL Server, see
     *    http://support.microsoft.com/kb/321843
     *
     * So, for now, just prevent creation of all on delete cascade rules 
     */
    protected String getOnDelete() {
        return null;
    }

    protected String getSizeCommand(List cmds) {
        /*
         * to create a VARCHAR data type with a maximum of more than 8K character, they must be declared
         * as VARCHAR(MAX) data type with a check constraint to the column for the desired length
         */
        if (getType().equals("CLOB")) { // translates into TEXT datatype, which takes no size param
            return "";
        } else if (getType().equals("VARCHAR2") && getSize() > 8000) {
            return "(MAX)";
        } else {
            return super.getSizeCommand(cmds);
        }
    }

    protected void getPostCreateCommands(List cmds) {
        /*
         * to create a VARCHAR data type with a maximum of more than 8K character, they must be declared
         * as VARCHAR(MAX) data type with a check constraint to the column for the desired length
         */
        if (getType().equals("VARCHAR2") && getSize() > 8000) {
            String table = this.m_strTableName.toUpperCase();
            String column = this.m_strName.toUpperCase();
            String constraintName = column + String.valueOf(getSize());

            cmds.add("ALTER TABLE " + table //
                + " ADD CONSTRAINT " + constraintName //
                + " CHECK DATALENGTH(" + column + ") <= " + getSize());
        }
    }

    protected String getDefaultCommand(List cmds) {
        String strCmd = "DEFAULT ";

        switch (this.getDefault()) {
        case Column.DEFAULT_AUTO_INCREMENT:
        case Column.DEFAULT_SEQUENCE_ONLY: {
            int seed = this.getIncrementSequence() > 0 ? this.getIncrementSequence() : 1;
            int increment = this.getIncrementSequence() > 0 ? this.getIncrementSequence() : 1;
            // IDENTITY columns do not start as DEFAULT definitions, they are special 
            strCmd = "IDENTITY ( " + String.valueOf(seed) + ", " + String.valueOf(increment) + " )";
        }
        }

        return strCmd;
    }

    protected void getDropCommands(List cmds) {
        if (this.hasDefault()) {
            switch (this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY: {
                // there's apparently no way to remove the IDENTITY property via T-SQL
                // it needs to be done through system tables
                cmds.add("sp_configure 'allow update', 1");
                cmds.add("reconfigure with override");
                cmds.add("update syscolumns set colstat = colstat - 1 " //
                    + "where id = object_id('" + this.m_strTableName.toUpperCase() + "') " //
                    + "and name = '" + this.getName().toUpperCase() + "'");
                cmds.add("exec sp_configure 'allow update', 0");
                cmds.add("reconfigure with override");
                break;
            }
            }
        }
    }
}