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

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.testng.annotations.Test;

/**
 * Performs database tests that require ANT to run the actual tests (like the DBUpgrade stuff).
 *
 * @author John Mazzitelli
 *
 */
@Test
public class AntDatabaseTest extends AbstractDatabaseTestUtil {
    /**
     * Tests upgrading schema on postgres DB.
     *
     * @throws Exception
     */
    public void testDbUpgradePostgres() throws Exception {
        String db = "postgresql";

        // skip test if it is to be skipped
        if (getConnection(db) == null) {
            return;
        }

        String test_resources_dir = System.getProperty("AntDatabaseTest.test-resources");

        System.setProperty("test.upgrade.dbsetup.xmlfile", "preupgrade-dbsetup.xml");
        System.setProperty("test.upgrade.target.schema.version", "2.0.0");
        System.setProperty("test.upgrade.jdbc.url", getTestDatabaseConnectionUrl(db));
        System.setProperty("test.upgrade.jdbc.user", getTestDatabaseConnectionUsername(db));
        System.setProperty("test.upgrade.jdbc.password", getTestDatabaseConnectionPassword(db));
        System.setProperty("basedir", test_resources_dir);

        startAnt(test_resources_dir + File.separator + "test-upgrade.xml");
    }

    /**
     * Launches ANT and runs the default target in the given build file.
     *
     * @param  build_file_str
     *
     * @throws RuntimeException
     */
    private void startAnt(String build_file_str) {
        Project project = new Project();
        File build_file = new File(build_file_str);

        try {
            project.setCoreLoader(getClass().getClassLoader());
            project.init();
            new ProjectHelper2().parse(project, build_file);
            project.executeTarget(project.getDefaultTarget());
        } catch (BuildException e) {
            throw new RuntimeException("Cannot run ANT on script [" + build_file_str + "]. Cause: " + e, e);
        }
    }
}