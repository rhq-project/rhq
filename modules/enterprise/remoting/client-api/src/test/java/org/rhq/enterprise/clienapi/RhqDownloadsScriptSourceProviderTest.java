/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.clienapi;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.net.URI;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.bindings.StandardBindings;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.clientapi.RemoteClient;
import org.rhq.enterprise.clientapi.RhqDownloadsScriptSourceProvider;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
@PrepareForTest(RhqDownloadsScriptSourceProvider.class)
public class RhqDownloadsScriptSourceProviderTest {

    private File tmpDir;
    
    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }
    
    private static final String EXPECTED_CONTENTS = "println('Hello, World!')";
    
    @BeforeClass
    public void createTmpDir() throws Exception {
        tmpDir = FileUtil.createTempDirectory(getClass().getName(), null, null);
        
        File downloadsDir = new File(new File(tmpDir, "downloads"), "script-modules");
        downloadsDir.mkdirs();
        
        File testScript = new File(downloadsDir, "test-script.js");
        
        FileOutputStream out = new FileOutputStream(testScript);
        
        try {
            out.write(EXPECTED_CONTENTS.getBytes());
        } finally {
            out.close();
        }
    }
    
    @AfterClass
    public void deleteTempDir() {
        if (tmpDir != null) {
            FileUtil.purge(tmpDir, true);
        }
    }
    
    public void canLocateScripts() throws Exception {
        RemoteClient client = Mockito.mock(RemoteClient.class); 
        
        //this is akin to what the remote client actually returns as the remote URI
        URI remoteURI = new URI("socket://localhost:7080");
        Mockito.when(client.getRemoteURI()).thenReturn(remoteURI);
        
        URI real = tmpDir.toURI().resolve("downloads/script-modules/test-script.js");
        
        RhqDownloadsScriptSourceProvider provider = new RhqDownloadsScriptSourceProvider();

        StandardBindings bindings = new StandardBindings(null, client);
        
        provider.rhqFacadeChanged(bindings);
        
        URI location = new URI("rhq://downloads/test-script.js");
        
        //let's actually cheat - the source provider will try to locate the script using HTTP
        //but we subvert that and use the local file URL anyway...
        PowerMockito.whenNew(URI.class).withArguments("http", "localhost:7080", "/downloads/script-modules/test-script.js", null, null).thenReturn(real);

        Reader rdr = provider.getScriptSource(location);
        
        try {
            String contents = StreamUtil.slurp(rdr);
            assertEquals(contents, EXPECTED_CONTENTS, "Unexpected script loaded");
        } finally {        
            rdr.close();
        }
    }
}
