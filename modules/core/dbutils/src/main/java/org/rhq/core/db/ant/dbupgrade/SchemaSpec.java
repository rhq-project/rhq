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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mazz.i18n.Msg;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * An ANT task that defines a single, specific upgrade step that upgrades the schema to a particular
 * {@link SchemaVersion version}. Each {@link SchemaSpec} contains child {@link SchemaSpecTask tasks} that define the
 * actual work that needs to get done to perform this upgrade step.
 */
public class SchemaSpec extends Task implements TaskContainer, Comparable {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String versionString = null;
    private SchemaVersion version = null;
    private List<SchemaSpecTask> schemaSpecTasks = new ArrayList<SchemaSpecTask>();
    private Connection conn = null;
    private DBUpgrader upgrader = null;

    /**
     * Creates a new {@link SchemaSpec} object.
     *
     * @param parent
     */
    public SchemaSpec(DBUpgrader parent) {
    }

    /**
     * This is the version to which this upgrade step takes the schema to. After this object's child tasks are run, the
     * schema is considered to be at this version.
     *
     * @param version
     */
    public void setVersion(String version) {
        versionString = version;
    }

    /**
     * Returns the schema version of this object. This is the version to which this upgrade step takes the schema to.
     * After this object's child tasks are run, the schema is considered to be at this version.
     *
     * @return the version
     *
     * @throws BuildException if the version string was invalid
     */
    public SchemaVersion getVersion() throws BuildException {
        if (version == null) {
            if (versionString == null) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.MISSING_SCHEMA_SPEC_VERSION));
            }

            try {
                version = new SchemaVersion(versionString);
            } catch (IllegalArgumentException e) {
                throw new BuildException(e.getMessage(), e);
            }
        }

        return version;
    }

    /**
     * Initializes this object with a database connection and a DBUpgrade task object.
     *
     * @param db_conn
     * @param db_upgrader
     */
    public void initialize(Connection db_conn, DBUpgrader db_upgrader) {
        this.conn = db_conn;
        this.upgrader = db_upgrader;
    }

    /**
     * Adds the given child task to the list of tasks. The task must be of type {@link SchemaSpecTask}.
     *
     * @param  schema_spec_task
     *
     * @throws BuildException if the task is not of the expected type
     */
    public void addTask(Task schema_spec_task) {
        if (schema_spec_task instanceof SchemaSpecTask) {
            schemaSpecTasks.add((SchemaSpecTask) schema_spec_task);
        } else if (schema_spec_task instanceof UnknownElement) {
            ((UnknownElement) schema_spec_task).maybeConfigure();
            schema_spec_task = ((UnknownElement) schema_spec_task).getTask();

            if ((schema_spec_task != null) && (schema_spec_task instanceof SchemaSpecTask)) {
                schemaSpecTasks.add((SchemaSpecTask) schema_spec_task);
            } else {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.CANNOT_ADD_SCHEMA_SPEC_TASK, schema_spec_task
                    .getTaskName(), SchemaSpecTask.class));
            }
        }
    }

    /**
     * Executes each task defined in this schema spec.
     *
     * @throws BuildException
     */
    public void execute() throws BuildException {
        validateAttributes();

        for (SchemaSpecTask sst : schemaSpecTasks) {
            try {
                log(MSG.getMsg(DbAntI18NResourceKeys.EXECUTING_SCHEMA_SPEC_TASK, sst.getClass(), getVersion()));

                sst.initialize(conn, upgrader);
                // to be able to ignore a failed ddl update we need to execute that
                // update in its own trans, because the failure may mark the trans
                // for rollback. So, commit ant transaction in progress.
                if (sst.isIgnoreError()) {
                    try {
                        conn.commit();
                    } catch (SQLException e) {
                        log("commit() exception: " + e.toString());
                    }
                }
                sst.execute();
            } catch (Exception e) {
                String msg = MSG.getMsg(DbAntI18NResourceKeys.ERROR_EXECUTING_SCHEMA_SPEC_TASK, sst.getClass()
                    .getName(), getVersion(), e);
                if (!sst.isIgnoreError()) {
                    throw new BuildException(msg, e);
                } else {
                    // rollback the trans so the next statement starts a new trans
                    try {
                        conn.rollback();
                    } catch (SQLException e2) {
                        log("rollback() exception: " + e2.toString());
                    }
                    log(msg);
                }
            }
        }
    }

    /**
     * Makes sure the schema spec version string is valid.
     *
     * @throws BuildException
     */
    private void validateAttributes() throws BuildException {
        getVersion();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "SchemaSpec[" + getVersion() + "]";
    }

    /**
     * Compares the versions.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        if (o instanceof SchemaSpec) {
            SchemaSpec ss = (SchemaSpec) o;
            return getVersion().compareTo(ss.getVersion());
        }

        throw new IllegalArgumentException(MSG.getMsg(DbAntI18NResourceKeys.CANNOT_COMPARE_NON_SCHEMA_SPEC, o));
    }
}