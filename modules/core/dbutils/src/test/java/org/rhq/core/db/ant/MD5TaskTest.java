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
package org.rhq.core.db.ant;

import org.apache.tools.ant.Project;
import org.testng.annotations.Test;

/**
 * Tests MD5 calculations by the ANT task.
 */
@Test
public class MD5TaskTest {
    /**
     * Tests that the MD5 task can generate proper MD5 codes.
     *
     * @throws Exception
     */
    public void testMD5() throws Exception {
        // make sure no one used our test properties before
        System.clearProperty("md5prop_one");
        System.clearProperty("md5prop_two");

        MD5Task md5task = new MD5Task();
        Project project = new Project();
        md5task.setProject(project);

        md5task.setProperty("md5prop_one");
        md5task.setValue("encode this string\r\n");
        md5task.setBase64(false);
        md5task.execute();
        assert "bc29ebdf4969fc5a413e0567b4566538".equals(project.getProperty("md5prop_one"));

        md5task.setProperty("md5prop_two");
        md5task.setValue("abc~!@#$%^&*()xyz\r\n");
        md5task.setBase64(false);
        md5task.execute();
        assert "c0c0bb102d9f092b504c055fe47b576c".equals(project.getProperty("md5prop_two"));
    }

    /**
     * Tests that the MD5 task can generate proper MD5 codes that are base64 encoded.
     *
     * @throws Exception
     */
    public void testMD5Base64() throws Exception {
        // make sure no one used our test properties before
        System.clearProperty("md5base64prop_one");
        System.clearProperty("md5base64prop_two");

        MD5Task md5task = new MD5Task();
        Project project = new Project();
        md5task.setProject(project);

        // tests that the default base64 encoding is enabled
        md5task.setProperty("md5base64prop_one");
        md5task.setValue("encode this string\r\n");
        md5task.execute();

        String md5base64prop_one = project.getProperty("md5base64prop_one");
        assert "vCnr30lp/FpBPgVntFZlOA==".equals(md5base64prop_one) : "Bad MD5/base64: " + md5base64prop_one;

        md5task.setProperty("md5base64prop_two");
        md5task.setValue("abc~!@#$%^&*()xyz\r\n");
        md5task.setBase64(true);
        md5task.execute();

        String md5base64prop_two = project.getProperty("md5base64prop_two");
        assert "wMC7EC2fCStQTAVf5HtXbA==".equals(md5base64prop_two) : "Bad MD5/base64: " + md5base64prop_two;
    }
}