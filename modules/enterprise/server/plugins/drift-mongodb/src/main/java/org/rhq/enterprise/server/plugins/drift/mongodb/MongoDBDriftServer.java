package org.rhq.enterprise.server.plugins.drift.mongodb;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.drift.dto.DriftDTO;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.ZipUtil;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

import static org.rhq.enterprise.server.util.LookupUtil.getResourceManager;

public class MongoDBDriftServer implements DriftServerPluginFacet {

    private final Log log = LogFactory.getLog(MongoDBDriftServer.class);

    private Mongo connection;

    private Morphia morphia;

    private Datastore ds;

    static int changeSetVersions = 0;

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        connection = new Mongo("localhost");
        morphia = new Morphia()
            .map(MongoDBChangeSet.class)
            .map(MongoDBChangeSetEntry.class)
            .map(MongoDBFile.class);
        ds = morphia.createDatastore(connection, "rhq");
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void saveChangeSet(final int resourceId, final File changeSetZip) throws Exception {
        ZipUtil.walkZipFile(changeSetZip, new ZipUtil.ZipEntryVisitor() {
            @Override
            public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {
                ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(stream)));

                Headers headers = reader.getHeaders();
                MongoDBChangeSet changeSet = new MongoDBChangeSet();
                changeSet.setCategory(headers.getType());
                changeSet.setResourceId(resourceId);
                // TODO Figure out how best to handle drift config reference
                changeSet.setDriftConfigurationId(1);
                changeSet.setVersion(changeSetVersions++);

                for (DirectoryEntry dirEntry : reader) {
                    for (FileEntry fileEntry : dirEntry) {
                        MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry();
                        entry.setCategory(fileEntry.getType());
                        entry.setPath(fileEntry.getFile());
                        changeSet.add(entry);
                    }
                }

                ds.save(changeSet);
                return true;
            }
        });
    }

    @Override
    public void saveChangeSetFiles(File changeSetFilesZip) throws Exception {

    }

    @Override
    public PageList<DriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria) {
        return new PageList<DriftChangeSet>();
    }

    @Override
    public PageList<Drift> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        String[] idFields = criteria.getFilterId().split(":");
        ObjectId changeSetId = new ObjectId(idFields[0]);
        String path = idFields[1];
        Query<MongoDBChangeSet> query = ds.createQuery(MongoDBChangeSet.class)
            .filter("id = ", changeSetId)
            .filter("files.path = ", path);

        PageList results = new PageList<DriftDTO>();

        for (MongoDBChangeSet changeSet : query) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                results.add((toDTO(entry, changeSetDTO)));
            }
        }

        return (PageList<Drift>) results;
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {
        Query<MongoDBChangeSet> query = ds.createQuery(MongoDBChangeSet.class)
            .filter("files.category in ", criteria.getFilterCategories())
            .filter("resourceId in", criteria.getFilterResourceIds());

        PageList<DriftComposite> results = new PageList<DriftComposite>();
        Map<Integer, Resource> resources = loadResourceMap(subject, criteria.getFilterResourceIds());

        for (MongoDBChangeSet changeSet : query) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                results.add(new DriftComposite(toDTO(entry, changeSetDTO), resources.get(changeSet.getResourceId())));
            }
        }

        return results;
    }

    @Override
    public Snapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria) {
        return null;
    }

    Map<Integer, Resource> loadResourceMap(Subject subject, List<Integer> resourceIds) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterIds(resourceIds.toArray(new Integer[resourceIds.size()]));

        ResourceManagerLocal resourceMgr = getResourceManager();
        PageList<Resource> resources = resourceMgr.findResourcesByCriteria(subject, criteria);

        Map<Integer, Resource> map = new HashMap<Integer, Resource>();
        for (Resource r : resources) {
            map.put(r.getId(), r);
        }

        return map;
    }

    DriftChangeSetDTO toDTO(MongoDBChangeSet changeSet) {
        DriftChangeSetDTO dto = new DriftChangeSetDTO();
        dto.setId(changeSet.getId());
        dto.setDriftConfigurationId(changeSet.getDriftConfigurationId());
        dto.setVersion(changeSet.getVersion());
        dto.setCtime(changeSet.getCtime());
        dto.setCategory(changeSet.getCategory());

        return dto;
    }

    DriftDTO toDTO(MongoDBChangeSetEntry entry, DriftChangeSetDTO changeSetDTO) {
        DriftDTO dto = new DriftDTO();
        dto.setChangeSet(changeSetDTO);
        dto.setId(entry.getId());
        dto.setCtime(entry.getCtime());
        dto.setPath(entry.getPath());
        dto.setCategory(entry.getCategory());

        DriftFileDTO fileDTO = new DriftFileDTO();
        fileDTO.setHashId("1a2b3c4e5f");

        dto.setOldDriftFile(fileDTO);
        dto.setNewDriftFile(fileDTO);

        return dto;
    }

}
