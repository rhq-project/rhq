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
package org.rhq.core.db.ant.dbsetup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;
import org.rhq.core.db.setup.DBSetup;

/**
 * Ant task wrapper around {@link DBSetup}.
 */
public class DBSetupTask extends Task {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private File xmlFile;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private boolean dataOnly = false;
    private boolean uninstall = false;
    private boolean exportXml = false;
    private ArrayList<Environment.Variable> sysProps = new ArrayList<Environment.Variable>();

    // These are used when "dataOnly" is true, to only setup a single table
    private String table = null;
    private boolean doDelete = false;

    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    public void setJdbcUrl(String url) {
        this.jdbcUrl = url;
    }

    public void setJdbcUser(String user) {
        this.jdbcUser = user;
    }

    public void setJdbcPassword(String pass) {
        this.jdbcPassword = pass;
    }

    public void setDataOnly(boolean data_only) {
        this.dataOnly = data_only;
    }

    public void setUninstall(boolean uninstall) {
        this.uninstall = uninstall;
    }

    public void setExportXml(boolean export) {
        this.exportXml = export;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setDelete(boolean del) {
        this.doDelete = del;
    }

    /**
     * Support subelements to set System properties e.g &lt;sysproperty key="foo" value="bar" /&gt; After the task has
     * completed, the system properties will be reverted to their old values (of if the system property didn't exist
     * before, it will be removed).
     *
     * @param sysprop
     */
    public void addSysproperty(Environment.Variable sysprop) {
        sysProps.add(sysprop);
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        validateAttributes();

        // being able to set system properties can be useful to set JDBC LoggerDriver system properties
        // but remember the old values so we can revert back to them after the task finishes
        Properties old_sysprops = new Properties(); // old values for keys that existed
        List<String> nonexistent_sysprops = new ArrayList<String>(); // keys that didn't exist in system properties
        for (Environment.Variable env_var : sysProps) {
            String old_value = System.setProperty(env_var.getKey(), env_var.getValue());
            if (old_value == null) {
                nonexistent_sysprops.add(env_var.getKey());
            } else {
                old_sysprops.put(env_var.getKey(), old_value);
            }
        }

        try {
            DBSetup dbs = new DBSetup(jdbcUrl, jdbcUser, jdbcPassword);

            if (uninstall) {
                dbs.uninstall(xmlFile.getAbsolutePath());
            } else if (exportXml) {
                dbs.export(xmlFile.getAbsolutePath());
            } else if (table == null) {
                dbs.setup(xmlFile.getAbsolutePath());
            } else {
                dbs.setup(xmlFile.getAbsolutePath(), table, dataOnly, doDelete);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            // revert back to the old system properties
            for (String name : nonexistent_sysprops) {
                System.clearProperty(name);
            }

            for (Map.Entry old_entry : old_sysprops.entrySet()) {
                System.setProperty((String) old_entry.getKey(), (String) old_entry.getValue());
            }
        }
    }

    private void validateAttributes() throws BuildException {
        if (xmlFile == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.TASK_MISSING_ATTRIB, getTaskName(), "xmlFile"));
        }

        if (jdbcUrl == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.TASK_MISSING_ATTRIB, getTaskName(), "jdbcUrl"));
        }
    }
}