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

import java.io.InputStream;
import java.util.Collection;
import org.testng.annotations.Test;

/**
 * Tests the {@link TypeMap}.
 *
 * @author John Mazzitelli
 *
 */
@Test
public class TypeMapTest {
    /**
     * Tests the "java" mappings.
     */
    public void testKnownJdbcTypes() {
        assert "INTEGER".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "INTEGER", null));
        assert "BIGINT".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "LONG", null));
        assert "DOUBLE".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BIGDEC", null));
        assert "VARCHAR".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "VARCHAR2", null));
        assert "LONGVARCHAR".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "LONGVARCHAR", null));
        assert "CHAR".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "CHAR", null));
        assert "FLOAT".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "DOUBLE", null));
        assert "BOOLEAN".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BOOLEAN", null));
        assert "BLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BYTES", null));
        assert "BLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "BLOB", null));
        assert "CLOB".equals(TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), "CLOB", null));
    }

    /**
     * Tests the known mappings.
     */
    public void testLoadKnown() {
        Collection<TypeMap> mapping = TypeMap.loadKnownTypeMaps();
        assert mapping.size() == 13 : "Wrong number of mappings returned: " + mapping.size();

        DatabaseType oracle = new Oracle10DatabaseType();
        DatabaseType postgres = new Postgresql8DatabaseType();

        assert "BIGINT".equals(TypeMap.getMappedType(mapping, "LONG", null));
        assert "BIGINT".equals(TypeMap.getMappedType(mapping, "LONG", postgres));
        assert "NUMBER(19,0)".equals(TypeMap.getMappedType(mapping, "LONG", oracle));

        assert "VARCHAR".equals(TypeMap.getMappedType(mapping, "VARCHAR2", null));
        assert "CHARACTER VARYING".equals(TypeMap.getMappedType(mapping, "VARCHAR2", postgres));
        assert "VARCHAR2".equals(TypeMap.getMappedType(mapping, "VARCHAR2", oracle));
    }

    /**
     * Tests some test mappings that also have DB version specific mappings.
     *
     * @throws Exception
     */
    public void testLoadTestMappings() throws Exception {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("test-db-typemaps.xml");
        Collection<TypeMap> mapping = TypeMap.loadTypeMapsFromStream(is);
        assert mapping.size() == 1 : "Wrong number of mappings returned: " + mapping.size();

        TypeMap map = mapping.iterator().next();
        map.getMappedType("FOO", null).equals("JAVAFOO");
        map.getMappedType("FOO", new Oracle8DatabaseType()).equals("ORACLEFOO");
        map.getMappedType("FOO", new Postgresql7DatabaseType()).equals("POSTGRES7FOO");
        map.getMappedType("FOO", new Postgresql8DatabaseType()).equals("POSTGRES8FOO");
    }
}