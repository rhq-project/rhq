/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.enterprise.client.script;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 * @since 4.10.0
 */
@Test
public class ModulesDirectoryScriptSourceProviderTest {

    private File scriptFile;

    @BeforeClass
    public void createScriptFile() throws IOException {
        scriptFile = File.createTempFile("modules-test", ".js", new File("."));
        PrintWriter wrt = new PrintWriter(new FileOutputStream(scriptFile));
        try {
            wrt.write("var a = 2;");
        } finally {
            wrt.close();
        }
    }

    @AfterClass
    public void deleteScriptFile() {
        scriptFile.delete();
    }

    public void testLoad() throws URISyntaxException {
        ModulesDirectoryScriptSourceProvider provider = new ModulesDirectoryScriptSourceProvider(new File("."));

        File f = provider.getFile(new URI("modules:/" + scriptFile.getName()));

        assertEquals(f, scriptFile, "Unexpected file loaded");
    }
}
