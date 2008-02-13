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
package org.rhq.core.db.ant.dbupdate;

import org.testng.annotations.Test;
import org.rhq.core.db.ant.dbupgrade.SchemaVersion;

/**
 * Tests {@link SchemaVersion}.
 *
 * @author John Mazzitelli
 *
 */
@Test
public class SchemaVersionTest {
    /**
     * Tests valid and invalid schema versions.
     */
    public void testSchemaVersionParsing() {
        new SchemaVersion("1");
        new SchemaVersion("1.1");
        new SchemaVersion("1.1.1");
        new SchemaVersion("1.0.0");
        new SchemaVersion("1.2.3");
        new SchemaVersion("33.22.11");

        try {
            new SchemaVersion("1.0.x");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests checking between versions.
     */
    public void testBetween() {
        assert new SchemaVersion("2").between(new SchemaVersion("1"), new SchemaVersion("3"));
        assert new SchemaVersion("222").between(new SchemaVersion("111"), new SchemaVersion("333"));
        assert new SchemaVersion("1.0.2").between(new SchemaVersion("1.0.0"), new SchemaVersion("1.0.3"));
        assert new SchemaVersion("1.0.2").between(new SchemaVersion("1.0"), new SchemaVersion("1.1"));
        assert new SchemaVersion("1.2.0").between(new SchemaVersion("1.1.0"), new SchemaVersion("1.2.0")); // end is inclusive

        assert !new SchemaVersion("1").between(new SchemaVersion("1"), new SchemaVersion("1.1")); // start is exclusive
    }
}