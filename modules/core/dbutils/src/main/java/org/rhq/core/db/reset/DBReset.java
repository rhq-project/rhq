/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.core.db.reset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DbUtil;

//import groovy.sql.Sql;

/**
 * @author Stefan Negrea
 *
 */
public class DBReset {
    private static Log log = LogFactory.getLog(DBReset.class);

    private static final String DB_NAME = System.getProperty("rhq.ds.db-name", "rhq_installer_test_db");
    private static final String SERVER = System.getProperty("rhq.ds.server-name", "127.0.0.1");
    private static final String DB_URL = System.getProperty("rhq.ds.connection-url", "jdbc:postgresql://" + SERVER
        + ":5432/" + DB_NAME);
    private static final String USER = System.getProperty("rhq.ds.user-name", "rhqadmin");
    private static final String ADMIN_USER = System.getProperty("rhq.db.admin.username", "postgres");
    private static final String ADMIN_PASSWORD = System.getProperty("rhq.db.admin.password", "postgres");
    private static final String DB_TYPE_MAPPING = System.getProperty("rhq.ds.type-mapping", "PostgreSQL");
    private static final String DB_RESET = System.getProperty("dbreset", "false");

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (DB_RESET.equals("false")) {
            return;
        }

        DBReset dbreset = new DBReset();

        try {
            dbreset.performDBReset(DB_TYPE_MAPPING, DB_URL, DB_NAME, USER, ADMIN_USER, ADMIN_PASSWORD);
            System.setProperty("dbsetup", "true");
        } catch (Exception e) {
            log.info(e);
            System.exit(1);
        }
    }

    public void performDBReset(String dbTypeMapping, String dbUrl, String dbName, String user, String adminUser,
        String adminPassword) throws Exception {
        if (dbTypeMapping.equals("PostgreSQL")) {
            System.out.println("PostgreSQL started!");
            Connection connection = null;
            Statement dropDB = null;
            Statement createDB = null;

            try {
                connection = DbUtil.getConnection(dbUrl.replace(dbName, "postgres"), adminUser, adminPassword);

                dropDB = connection.createStatement();
                dropDB.execute("drop database if exists " + dbName);

                createDB = connection.createStatement();
                createDB.execute("create database " + dbName + " with owner " + user);

                log.info("Dropped and created postgres database " + dbName + ".");
            } finally {
                if (dropDB != null) {
                    dropDB.close();
                }
                if (createDB != null) {
                    createDB.close();
                }
                if (connection != null) {
                    connection.close();
                }

            }
        } else if (dbTypeMapping.equals("Oracle10g")) {
            Connection connection = null;
            PreparedStatement cleanUserStatement = null;

            try {
                connection = DbUtil.getConnection(dbUrl, adminUser, adminPassword);
                connection.setAutoCommit(false);

                String plsql = "declare cursor all_objects_to_drop is\n"
                    + "select *  from user_objects where object_type in ('TABLE', 'VIEW', 'FUNCTION', 'SEQUENCE');\n"
                    + "begin\n"
                    + "  for obj in all_objects_to_drop loop\n"
                    + "    begin\n"
                    + "      if obj.object_type = 'TABLE' then\n"
                    + "        execute immediate('DROP '||obj.object_type||' '||obj.object_name||' CASCADE CONSTRAINTS PURGE');\n"
                    + "      else\n" 
                    + "        execute immediate('DROP '||obj.object_type||' '||obj.object_name);\n"
                    + "      end if;\n" 
                    + "      exception when others then null;\n" 
                    + "    end;\n" 
                    + "  end loop;\n"
                    + " end;\n";
                cleanUserStatement = connection.prepareStatement(plsql);
                cleanUserStatement.execute();
                connection.commit();

                log.info("Cleaned Oracle database " + dbName + ".");
            } finally {
                if (cleanUserStatement != null) {
                    cleanUserStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        }
        else {
            throw new Exception("dbreset not supported for "+ dbTypeMapping +"!");
        }
    }
}
