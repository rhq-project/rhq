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
package org.rhq.core.db;

import java.sql.Connection;
import java.sql.ResultSet;

import org.testng.annotations.Test;

import org.rhq.core.db.setup.DBSetup;

/**
 * Tests database utilities like types and the factory. If you do not want the tests to fail if a database is not
 * available, set <code>DatabaseTest.nofail</code> system property to <code>true</code>.
 *
 * <p>This test loads in the test-databases.properties file as its defaults for database connecitivity info. You can set
 * system properties to override those defaults, if your test environment is different than these defaults.</p>
 *
 * @author John Mazzitelli
 *
 */
@Test
public class DatabaseTest extends AbstractDatabaseTestUtil {
    /**
     * Test DBSetup using a postgres DB.
     *
     * @throws Exception
     */
    public void testDbSetupPostgres() throws Exception {
        String db = "postgresql";

        // skip test if it is to be skipped
        if (getConnection(db) == null) {
            return;
        }

        DBSetup dbsetup = new DBSetup(getTestDatabaseConnectionUrl(db), getTestDatabaseConnectionUsername(db),
            getTestDatabaseConnectionPassword(db), false);

        try {
            dbsetup.setup("small-dbsetup.xml");

            Connection conn = getConnection(db);
            DatabaseType dbtype = DatabaseTypeFactory.getDatabaseType(conn);

            assert dbtype.checkTableExists(conn, "TEST_SMALL");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "ID");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYLONG");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYBIGDEC");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYLONGVARCHAR");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYDOUBLE");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYBOOLEAN");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYBYTES");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYVARCHAR2");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYCLOB");
            assert dbtype.checkColumnExists(conn, "TEST_SMALL", "MYBLOB");

            ResultSet results = conn.prepareCall("SELECT MYVARCHAR2 FROM TEST_SMALL").executeQuery();
            results.next();
            assert "abc-myvarchar2".equals(results.getString("MYVARCHAR2"));
        } finally {
            try {
                dbsetup.uninstall("small-dbsetup.xml");
            } catch (Exception e) {
                System.err.println("Cannot uninstall the test schema");
            }
        }

        return;
    }

    /**
     * Test DBSetup uninstall using a postgres DB
     *
     * @throws Exception
     */
    public void testDbUninstallPostgres() throws Exception {
        String db = "postgresql";

        // skip test if it is to be skipped
        if (getConnection(db) == null) {
            return;
        }

        DBSetup dbsetup = new DBSetup(getTestDatabaseConnectionUrl(db), getTestDatabaseConnectionUsername(db),
            getTestDatabaseConnectionPassword(db), false);

        dbsetup.setup("small-dbsetup.xml");

        Connection conn;
        DatabaseType dbtype;

        // get the connection, make sure the setup worked, and then uninstall the schema
        try {
            conn = getConnection(db);
            dbtype = DatabaseTypeFactory.getDatabaseType(conn);
            assert dbtype.checkTableExists(conn, "TEST_SMALL");
        } finally {
            dbsetup.uninstall("small-dbsetup.xml");
        }

        // make sure the uninstall worked
        assert !dbtype.checkTableExists(conn, "TEST_SMALL");
    }

    /**
     * Tests the database type factory's simple "is" checks.
     */
    public void testIsOraclePostgres() {
        DatabaseType oracle8 = new Oracle8DatabaseType();
        DatabaseType oracle9 = new Oracle9DatabaseType();
        DatabaseType oracle10 = new Oracle10DatabaseType();
        DatabaseType postgres7 = new Postgresql7DatabaseType();
        DatabaseType postgres8 = new Postgresql8DatabaseType();

        assert DatabaseTypeFactory.isOracle(oracle8);
        assert !DatabaseTypeFactory.isPostgres(oracle8);
        assert DatabaseTypeFactory.isOracle(oracle9);
        assert !DatabaseTypeFactory.isPostgres(oracle9);
        assert DatabaseTypeFactory.isOracle(oracle10);
        assert !DatabaseTypeFactory.isPostgres(oracle10);

        assert DatabaseTypeFactory.isPostgres(postgres7);
        assert !DatabaseTypeFactory.isOracle(postgres7);
        assert DatabaseTypeFactory.isPostgres(postgres8);
        assert !DatabaseTypeFactory.isOracle(postgres8);
    }

    /**
     * Tests postgres database.
     *
     * @throws Exception
     */
    public void testPostgres() throws Exception {
        Connection conn = getPostgresConnection();

        if (conn == null) {
            return;
        }

        DatabaseType dbtype = DatabaseTypeFactory.getDatabaseType(conn);

        assert DatabaseTypeFactory.isPostgres(conn);
        assert DatabaseTypeFactory.isPostgres(dbtype);
        assert dbtype.getVendor().equals("postgresql");

        assertPostgresTypes(dbtype);
    }

    /**
     * Tests postgres 8.x database.
     *
     * @throws Exception
     */
    public void testPostgres8() throws Exception {
        Connection conn = getPostgresConnection("8");
        if (conn == null) {
            return;
        }

        DatabaseType dbtype = DatabaseTypeFactory.getDatabaseType(conn);

        assert DatabaseTypeFactory.isPostgres(conn);
        assert DatabaseTypeFactory.isPostgres(dbtype);
        assert dbtype.getVendor().equals("postgresql") : dbtype;
        assert dbtype.getVersion().startsWith("8") : dbtype;
        assert dbtype.getName().startsWith("postgresql8") : dbtype;

        assertPostgresTypes(dbtype);
    }

    /**
     * Tests oracle database.
     *
     * @throws Exception
     */
    public void testOracle() throws Exception {
        Connection conn = getOracleConnection();
        if (conn == null) {
            return;
        }

        DatabaseType dbtype = DatabaseTypeFactory.getDatabaseType(conn);

        assert DatabaseTypeFactory.isOracle(conn);
        assert DatabaseTypeFactory.isOracle(dbtype);
        assert dbtype.getVendor().equals("oracle");

        assertOracleTypes(dbtype);
    }

    /**
     * Tests oracle version 10 database.
     *
     * @throws Exception
     */
    public void testOracle10() throws Exception {
        Connection conn = getOracleConnection("10");
        if (conn == null) {
            return;
        }

        DatabaseType dbtype = DatabaseTypeFactory.getDatabaseType(conn);

        assert DatabaseTypeFactory.isOracle(conn);
        assert DatabaseTypeFactory.isOracle(dbtype);
        assert dbtype.getVendor().equals("oracle") : dbtype;
        assert dbtype.getVersion().equals("10") : dbtype;
        assert dbtype.getName().equals("oracle10") : dbtype;

        assertOracleTypes(dbtype);
    }

    /**
     * Tests that the Postgres type mappings are correct. These are common across all Postgres versions.
     *
     * @param dbtype
     */
    private void assertPostgresTypes(DatabaseType dbtype) {
        assert dbtype instanceof PostgresqlDatabaseType;

        assert "INTEGER".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "INTEGER", dbtype)) : dbtype;
        assert "BIGINT".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "LONG", dbtype)) : dbtype;
        assert "NUMERIC(24,5)".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BIGDEC", dbtype)) : dbtype;
        assert "CHARACTER VARYING".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "VARCHAR2", dbtype)) : dbtype;
        assert "TEXT".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "LONGVARCHAR", dbtype)) : dbtype;
        assert "CHARACTER".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "CHAR", dbtype)) : dbtype;
        assert "FLOAT8".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "DOUBLE", dbtype)) : dbtype;
        assert "BOOLEAN".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BOOLEAN", dbtype)) : dbtype;
        assert "BYTEA".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BYTES", dbtype)) : dbtype;
        assert "BYTEA".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BLOB", dbtype)) : dbtype;
        assert "VARCHAR".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "CLOB", dbtype)) : dbtype;
    }

    /**
     * Tests that the Oracle type mappings are correct. These are common across all Oracle versions.
     *
     * @param dbtype
     */
    private void assertOracleTypes(DatabaseType dbtype) {
        assert dbtype instanceof OracleDatabaseType;

        assert "INTEGER".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "INTEGER", dbtype)) : dbtype;
        assert "NUMBER(19,0)".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "LONG", dbtype)) : dbtype;
        assert "NUMBER(24,5)".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BIGDEC", dbtype)) : dbtype;
        assert "VARCHAR2".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "VARCHAR2", dbtype)) : dbtype;
        assert "CLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "LONGVARCHAR", dbtype)) : dbtype;
        assert "CHAR".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "CHAR", dbtype)) : dbtype;
        assert "FLOAT(15)".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "DOUBLE", dbtype)) : dbtype;
        assert "NUMBER(1)".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BOOLEAN", dbtype)) : dbtype;
        assert "BLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BYTES", dbtype)) : dbtype;
        assert "BLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BLOB", dbtype)) : dbtype;
        assert "CLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "CLOB", dbtype)) : dbtype;
    }
}