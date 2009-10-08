 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.pluginapi.util;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

/**
 * Test {@link FileUtils}.
 *
 * @author Ian Springer
 */
@Test
public class FileUtilsTest {
    private final Log log = LogFactory.getLog(this.getClass());

    public void testGetCanonicalPathUnix() throws Exception {
        if (File.separatorChar == '/') {
            File symlinkTarget = File.createTempFile("jon", null);
            File symlink = new File(symlinkTarget.getPath() + "-symlink");
            String command = "/bin/ln -s " + symlinkTarget + " " + symlink;
            log.info("Executing command [" + command + "]...");
            Runtime.getRuntime().exec(command);

            // NOTE: Do *not* assert symlink.exists() - File#exists is broken for symlinks.
            String path = symlink.getPath();
            String path2 = FileUtils.getCanonicalPath(path);
            assert path2.equals(path) : path2 + " != " + path;
        }
    }

    public void testGetCanonicalPathWindows() throws Exception {
        if (File.separatorChar == '\\') {
            String path = File.createTempFile("jon", null).getCanonicalPath();
            String capitalizedPath = path.toUpperCase();
            String path2 = FileUtils.getCanonicalPath(capitalizedPath);
            assert path2.equals(path) : path2 + " != " + path;
        }
    }
}