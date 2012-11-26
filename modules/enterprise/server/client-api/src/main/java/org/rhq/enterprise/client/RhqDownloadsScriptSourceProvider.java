/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.script.BaseRhqSchemeScriptSourceProvider;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Lukas Krejci
 */
public class RhqDownloadsScriptSourceProvider extends BaseRhqSchemeScriptSourceProvider {

    private static final Log LOG = LogFactory.getLog(RhqDownloadsScriptSourceProvider.class);

    private static final String AUTHORITY = "downloads";

    private SystemManagerLocal systemManager;
    private SubjectManagerLocal subjectManager;

    public RhqDownloadsScriptSourceProvider() {
        this(LookupUtil.getSystemManager(), LookupUtil.getSubjectManager());
    }
    
    public RhqDownloadsScriptSourceProvider(SystemManagerLocal systemManager, SubjectManagerLocal subjectManager) {
        super(AUTHORITY);
        this.systemManager = systemManager;
        this.subjectManager = subjectManager;
    }
    
    @Override
    protected Reader doGetScriptSource(URI scriptUri) {
        String path = scriptUri.getPath().substring(1); //remove the leading /

        Subject overlord = subjectManager.getOverlord();

        ServerDetails serverDetails = systemManager.getServerDetails(overlord);

        String serverHomeDir = serverDetails.getDetails().get(ServerDetails.Detail.SERVER_HOME_DIR);

        File downloads = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/script-modules");

        File file = new File(downloads, path);

        try {
            return new InputStreamReader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOG.debug("Failed to locate the download file: " + scriptUri, e);
            return null;
        }
    }

}
