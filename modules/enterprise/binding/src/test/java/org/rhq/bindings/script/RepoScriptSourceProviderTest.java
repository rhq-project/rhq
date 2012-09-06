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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;

/**
 * @author Lukas Krejci
 */
@Test
public class RepoScriptSourceProviderTest {

    private static final String SCRIPT_CONTENTS = "java.lang.System.out.println('This works!');";
    private static final String CORRECT_REPO_NAME = "my-repo";
    private static final int CORRECT_REPO_ID = 1;
    private static final String CORRECT_PACKAGE_NAME = "my-script.js";
    private static final int CORRECT_PACKAGE_VERSION_ID = 2;

    public void testCanDownloadScript() throws Exception {
        RhqFacade rhqFacade = mock(RhqFacade.class);

        RepoManagerRemote repoManager = mock(RepoManagerRemote.class);
        ContentManagerRemote contentManager = mock(ContentManagerRemote.class);
        
        when(rhqFacade.getProxy(RepoManagerRemote.class)).thenReturn(repoManager);
        when(rhqFacade.getProxy(ContentManagerRemote.class)).thenReturn(contentManager);
        
        when(repoManager.findReposByCriteria(any(Subject.class), any(RepoCriteria.class))).then(
            new Answer<List<Repo>>() {
                @Override
                public List<Repo> answer(InvocationOnMock invocation) throws Throwable {
                    RepoCriteria crit = (RepoCriteria) invocation.getArguments()[1];

                    //this is so wrong...
                    Field f = RepoCriteria.class.getDeclaredField("filterName");
                    f.setAccessible(true);
                    String name = (String) f.get(crit);

                    if (CORRECT_REPO_NAME.equals(name)) {
                        Repo repo = new Repo(CORRECT_REPO_NAME);
                        repo.setId(CORRECT_REPO_ID);

                        PageList<Repo> ret = new PageList<Repo>(PageControl.getUnlimitedInstance());
                        ret.add(repo);
                        return ret;
                    } else {
                        return new PageList<Repo>(PageControl.getUnlimitedInstance());
                    }
                }
            });
        
        when(contentManager.findPackagesWithLatestVersion(any(Subject.class), any(PackageCriteria.class))).then(
            new Answer<List<PackageAndLatestVersionComposite>>() {
                @Override
                public List<PackageAndLatestVersionComposite> answer(InvocationOnMock invocation) throws Throwable {
                    PackageCriteria crit = (PackageCriteria) invocation.getArguments()[1];

                    //this is so wrong...
                    Field f = PackageCriteria.class.getDeclaredField("filterName");
                    f.setAccessible(true);
                    String name = (String) f.get(crit);

                    if (CORRECT_PACKAGE_NAME.equals(name)) {
                        PackageAndLatestVersionComposite composite = new PackageAndLatestVersionComposite();

                        composite
                            .setGeneralPackage(new org.rhq.core.domain.content.Package(CORRECT_PACKAGE_NAME, null));

                        PackageVersion pv = new PackageVersion();
                        pv.setId(CORRECT_PACKAGE_VERSION_ID);

                        composite.setLatestPackageVersion(pv);

                        PageList<PackageAndLatestVersionComposite> ret = new PageList<PackageAndLatestVersionComposite>(
                            PageControl.getUnlimitedInstance());
                        ret.add(composite);
                        return ret;
                    } else {
                        return new PageList<PackageAndLatestVersionComposite>(PageControl.getUnlimitedInstance());
                    }
                }
            });

        when(repoManager.getPackageVersionBytes(any(Subject.class), anyInt(), anyInt())).then(
            new Answer<byte[]>() {
                @Override
                public byte[] answer(InvocationOnMock invocation) throws Throwable {
                    int repoId = (Integer) invocation.getArguments()[1];
                    int packageId = (Integer) invocation.getArguments()[2];

                    if (repoId == CORRECT_REPO_ID && packageId == CORRECT_PACKAGE_VERSION_ID) {
                        return SCRIPT_CONTENTS.getBytes(Charset.forName("UTF-8"));
                    } else {
                        //TODO throw exceptions as the original method does
                        return null;
                    }
                }
            });

        RepoScriptSourceProvider provider = new RepoScriptSourceProvider();
        provider.rhqFacadeChanged(new StandardBindings(null, rhqFacade));

        URI uri = new URI("rhq://repositories/" + CORRECT_REPO_NAME + "/" + CORRECT_PACKAGE_NAME);

        Reader rdr = provider.getScriptSource(uri);

        StringBuilder bld = new StringBuilder();
        int c;
        while ((c = rdr.read()) != -1) {
            bld.append((char) c);
        }

        Assert.assertEquals(bld.toString(), SCRIPT_CONTENTS, "Unexpected script contents.");
    }
}
