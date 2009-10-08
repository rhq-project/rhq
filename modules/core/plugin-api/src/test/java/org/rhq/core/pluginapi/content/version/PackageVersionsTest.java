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
package org.rhq.core.pluginapi.content.version;

import java.io.File;
import org.testng.annotations.Test;

 /**
 * @author Jason Dobies
 */
public class PackageVersionsTest {
    private static final boolean TESTS_ENABLED = true;

    private static final String TARGET_DIR = "." + File.separator + "target" + File.separator;

    @Test(enabled = TESTS_ENABLED)
    public void persistAndLoad() {
        // Setup
        String pluginName = "persist-test";
        PackageVersions versions = new PackageVersions(pluginName, TARGET_DIR);
        versions.loadFromDisk();

        // Integrity check
        assert versions.getVersion("pkg1") == null : "Version for pkg1 found in supposedly new versions cache";

        versions.putVersion("pkg1", "version1");
        versions.putVersion("pkg2", "version2");

        // Test persisting
        versions.saveToDisk();

        // Verify
        String fullFileName = TARGET_DIR + PackageVersions.FILENAME;
        File persistedFile = new File(fullFileName);

        assert persistedFile.exists() : "Persisted file not found at: " + fullFileName;

        // Test loading
        versions = new PackageVersions(pluginName, TARGET_DIR);

        versions.loadFromDisk();

        String version = versions.getVersion("pkg1");
        assert version != null : "Version for pkg1 not found in versions loaded from disk";
        assert version.equals("version1") : "Incorrect version for pkg1. Expected: version1, Found: " + version; 

        // Cleanup
        persistedFile.delete();
        versions.unload();
    }

    @Test(enabled = TESTS_ENABLED)
    public void noLoadCall() {
        // Setup
        PackageVersions versions = new PackageVersions("no-load-call", TARGET_DIR);

        // Test
        try {
            versions.getVersion("pkg1");
            assert false : "Allowed to get a package version without loading";
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            versions.putVersion("pkg1", "version1");
            assert false : "Allowed to put a package version without loading";
        } catch (IllegalStateException e) {
            // Expected
        }
    }
}
