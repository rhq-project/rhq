package org.rhq.enterprise.server.content;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ContentTestHelper {

    public static ContentSource getTestContentSource() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        ContentSourceMetadataManagerLocal contentSourceMetadataManager = LookupUtil.getContentSourceMetadataManager();
        ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();
        ContentSourceType type = new ContentSourceType("testGetSyncResultsListCST");
        Set<ContentSourceType> types = new HashSet<ContentSourceType>();
        types.add(type);
        contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
        ContentSource contentSource = new ContentSource("testGetSyncResultsListCS", type);
        contentSource = contentSourceManager.simpleCreateContentSource(overlord, contentSource);
        return contentSource;
    }

    public static Repo getTestRepoWithContentSource() throws Exception {
        Repo repo = new Repo("testSyncRepos");
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        LookupUtil.getRepoManagerLocal().createRepo(overlord, repo);
        ContentSource contentSource = getTestContentSource();
        repo.addContentSource(contentSource);
        return repo;
    }

}
