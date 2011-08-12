/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift;

import java.io.File;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.toFile;

public class FilterFileVisitorTest {

    File basedir;

    @BeforeMethod
    public void setUp() throws Exception {
        File root = toFile(getClass().getResource("."));
        basedir = new File(root, "basedir");
        deleteDirectory(basedir);
        basedir.mkdirs();
    }

    @Test
    public void callVisitorWhenFileMatchesIncludeFilter() {
//        touch(new );
//        List<Filter> includes = asList(new Filter(""));
    }

}
