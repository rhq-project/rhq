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

package org.rhq.enterprise.client.test;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.HashMap;

import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.client.RhqDownloadsScriptSourceProvider;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class RhqDownloadsScriptSourceProviderTest {

    private File tmpDir;
    
    private static final String EXPECTED_CONTENTS = "println('Hello, World!')";
    @BeforeClass
    public void createTmpDir() throws Exception {
        tmpDir = FileUtil.createTempDirectory(getClass().getName(), null, null);
        File downloadsDir = new File(new File(new File(new File(tmpDir, "deploy"), "rhq.ear"), "rhq-downloads"), "script-modules");
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
        SystemManagerLocal systemManager = Mockito.mock(SystemManagerLocal.class);
        SubjectManagerLocal subjectManager = Mockito.mock(SubjectManagerLocal.class);
                
        ServerDetails details = new ServerDetails();
        HashMap<ServerDetails.Detail, String> map = new HashMap<ServerDetails.Detail, String>();
        map.put(ServerDetails.Detail.SERVER_HOME_DIR, tmpDir.getAbsolutePath());
        details.setDetails(map);
                
        Mockito.when(systemManager.getServerDetails(Mockito.<Subject>any())).thenReturn(details);
        
        RhqDownloadsScriptSourceProvider provider = new RhqDownloadsScriptSourceProvider(systemManager, subjectManager);
        
        URI location = new URI("rhq://downloads/test-script.js");
        
        Reader rdr = provider.getScriptSource(location);
        
        try {
            String contents = StreamUtil.slurp(rdr);
            assertEquals(contents, EXPECTED_CONTENTS, "Unexpected script loaded");
        } finally {        
            rdr.close();
        }
    }
}
