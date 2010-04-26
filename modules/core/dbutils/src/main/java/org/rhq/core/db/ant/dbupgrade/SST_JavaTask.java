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
import org.rhq.core.db.upgrade.DatabaseUpgradeTask;

/**
 * Task that allows more complicated upgrade logic to be externalized into a Java class.  The JavaTask must implement
 * the {@link DatabaseUpgradeTask} interface, which will pass the active {@link Connection} and {@link DatabaseType} 
 * objects used by the main upgrader to it.  Authors of JavaTasks are encouraged to use the helper methods provided by 
 * the {@link DatabaseType} object, which will help keep the implementation vendor agnostic with respect to the set of
 * databases supported by {@link DBUpgrader} at the time it was written.
 *
 * Ant task to invoke an external JavaTask, which can perform arbitrarily DDL and DML against the database. This task 
 * accepts one required attribute:
 *
 * <ul>
 *   <li>className</li>
 * </ul>
 * 
 * @author Joseph Marques
 */
public class SST_JavaTask extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String className;

    /**
     * Sets the name of the class that implements the {@link DatabaseUpgradeTask} interface.  If the name is not 
     * package-prefixed, the default "org.rhq.core.db.upgrade" will be assumed.
     *
     * @param className 
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Launches the {@link DatabaseUpgradeTask} defined by the className attribute, passing the {@link Connection} and
     * {@link DatabaseType} from the main {@link DBUpgrader}.
     *
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        validateAttributes();

        try {
            if (className.indexOf(".") == -1) {
                className = "org.rhq.core.db.upgrade." + className;
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.JAVA_TASK_EXECUTING, className));

            Class<?> javaTaskClass = Class.forName(className);
            DatabaseUpgradeTask javaTask = (DatabaseUpgradeTask) javaTaskClass.newInstance();

            DatabaseType db_type = getDatabaseType();
            Connection conn = getConnection();

            javaTask.execute(db_type, conn);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.JAVA_TASK_ERROR, e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (className == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_CHILD_ELEMENT,
                "JavaTask", "className"));
        }
    }

}