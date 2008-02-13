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
package org.rhq.core.db.ant.dbupgrade;

import java.sql.Connection;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * This is a DB-Upgrade ANT task that is used to create sequences in the database. The name of the sequence table must
 * be specified (see {@link #setName(String)}). The initial value and the increment can optionally be specified; they
 * will default to 1 if not specified.
 *
 * @author John Mazzitelli
 *
 */
public class SST_CreateSequence extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    /**
     * The name of the sequence table to be created.
     */
    private String m_name = null;
    private String m_initial = "1";
    private String m_increment = "1";

    /**
     * The name attribute of the ANT task element which defines the sequence table name.
     *
     * @param name sequence table name
     */
    public void setName(String name) {
        m_name = name;
    }

    /**
     * Sets the sequence increment.
     *
     * @param increment the increment
     */
    public void setIncrement(String increment) {
        m_increment = increment;
    }

    /**
     * Sets the initial value of the first sequence number.
     *
     * @param initial the initial sequence value
     */
    public void setInitial(String initial) {
        m_initial = initial;
    }

    /**
     * Creates the sequence table.
     *
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        // make sure the task has been defined properly
        validateAttributes();

        try {
            DatabaseType db_type = getDatabaseType();
            Connection conn = getConnection();

            log(MSG.getMsg(DbAntI18NResourceKeys.CREATE_SEQUENCE_EXECUTING, m_name, m_initial, m_increment));
            db_type.createSequence(conn, m_name, m_initial, m_increment);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_FAILURE, "CreateSequence", e), e);
        }

        return;
    }

    /**
     * This will confirm that the ANT task has all required attributes defined and are valid.
     *
     * @throws BuildException if the validation checks failed
     */
    private void validateAttributes() throws BuildException {
        if (m_name == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB,
                "CreateSequence", "name"));
        }

        if (m_increment == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB,
                "CreateSequence", "increment"));
        }

        if (m_initial == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB,
                "CreateSequence", "initial"));
        }

        try {
            Integer.parseInt(m_increment);
        } catch (NumberFormatException e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_INVALID_ATTRIB,
                "CreateSequence", "increment", m_increment));
        }

        try {
            Integer.parseInt(m_initial);
        } catch (NumberFormatException e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_INVALID_ATTRIB,
                "CreateSequence", "initial", m_initial));
        }

        // all checks out OK
        return;
    }
}