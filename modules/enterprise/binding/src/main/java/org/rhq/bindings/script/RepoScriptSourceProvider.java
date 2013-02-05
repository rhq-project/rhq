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

package org.rhq.bindings.script;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;

/**
 * The implementation of script source provider that is able to locate the script files
 * in the repositories on the RHQ server.
 * <p>
 * The URI looks like:<br>
 * <code>
 * rhq://repositories/repositoryName/scriptFile
 * </code>
 *  
 * @author Lukas Krejci
 */
public class RepoScriptSourceProvider extends BaseRhqSchemeScriptSourceProvider implements
    StandardBindings.RhqFacadeChangeListener {

    private static final Log LOG = LogFactory.getLog(RepoScriptSourceProvider.class);

    private static final String AUTHORITY = "repositories";

    private RhqFacade rhqFacade;

    public RepoScriptSourceProvider() {
        super(AUTHORITY);
    }
    
    @Override
    public void rhqFacadeChanged(StandardBindings bindings) {
        this.rhqFacade = bindings.getAssociatedRhqFacade();
    }

    @Override
    protected Reader doGetScriptSource(URI scriptUri) {
        if (rhqFacade == null) {
            return null;
        }

        String path = scriptUri.getPath().substring(1); //remove the leading / from the path

        int slashIdx = path.indexOf('/');

        if (slashIdx == -1) {
            return null;
        }

        String repoName = path.substring(0, slashIdx);
        String scriptName = path.substring(slashIdx + 1);

        try {
            RepoManagerRemote repoManager = rhqFacade.getProxy(RepoManagerRemote.class);

            RepoCriteria repoCrit = new RepoCriteria();
            repoCrit.addFilterName(repoName);
            repoCrit.clearPaging();//disable paging as the code assumes all the results will be returned.
            List<Repo> repos = repoManager.findReposByCriteria(rhqFacade.getSubject(), repoCrit);

            if (repos.isEmpty()) {
                return null;
            }

            ContentManagerRemote contentManager = rhqFacade.getProxy(ContentManagerRemote.class);

            for (Repo repo : repos) {
                PackageCriteria pCrit = new PackageCriteria();
                pCrit.addFilterName(scriptName);
                pCrit.addFilterRepoId(repo.getId());
                pCrit.clearPaging();//disable paging as the code assumes all the results will be returned.

                List<PackageAndLatestVersionComposite> pvs = contentManager.findPackagesWithLatestVersion(
                    rhqFacade.getSubject(), pCrit);

                if (!pvs.isEmpty()) {
                    PackageAndLatestVersionComposite pv = pvs.get(0);

                    byte[] bytes = repoManager.getPackageVersionBytes(rhqFacade.getSubject(), repo.getId(), pv
                        .getLatestPackageVersion().getId());

                    return new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to download bytes for the script: " + scriptUri.toString(), e);
        }

        return null;
    }

}
