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

package org.rhq.enterprise.server.plugins.drift.mongodb;

import static org.rhq.enterprise.server.util.LookupUtil.getAgentManager;
import static org.rhq.enterprise.server.util.LookupUtil.getResourceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.drift.dto.DriftDTO;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.FileDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

public class MongoDBDriftServer implements DriftServerPluginFacet, ServerPluginComponent {

    //private final Log log = LogFactory.getLog(MongoDBDriftServer.class);

    private Mongo connection;

    private Morphia morphia;

    private Datastore ds;

    private FileDAO fileDAO;

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        connection = new Mongo("localhost");
        morphia = new Morphia().map(MongoDBChangeSet.class).map(MongoDBChangeSetEntry.class).map(MongoDBFile.class);
        ds = morphia.createDatastore(connection, "rhq");
        fileDAO = new FileDAO(ds.getDB());
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
    public DriftChangeSetSummary saveChangeSet(final Subject subject, final int resourceId, final File changeSetZip)
        throws Exception {

        final DriftChangeSetSummary summary = new DriftChangeSetSummary();

        ZipUtil.walkZipFile(changeSetZip, new ZipUtil.ZipEntryVisitor() {
            @Override
            public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {
                ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(stream)));
                Headers headers = reader.getHeaders();

                List<DriftFileDTO> missingContent = new LinkedList<DriftFileDTO>();

                MongoDBChangeSet changeSet = new MongoDBChangeSet();
                changeSet.setCategory(headers.getType());
                changeSet.setResourceId(resourceId);
                changeSet.setDriftDefinitionId(headers.getDriftDefinitionId());
                changeSet.setDriftHandlingMode(DriftHandlingMode.normal);
                changeSet.setVersion(headers.getVersion());

                summary.setCategory(headers.getType());
                summary.setResourceId(resourceId);
                summary.setDriftDefinitionName(headers.getDriftDefinitionName());
                summary.setCreatedTime(changeSet.getCtime());

                for (FileEntry fileEntry : reader) {
                    String path = FileUtil.useForwardSlash(fileEntry.getFile());
                    MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry();
                    entry.setCategory(fileEntry.getType());
                    entry.setPath(path);

                    switch (fileEntry.getType()) {
                    case FILE_ADDED:
                        entry.setNewFileHash(fileEntry.getNewSHA());
                        if (fileDAO.findOne(fileEntry.getNewSHA()) == null) {
                            missingContent.add(newDriftFile(fileEntry.getNewSHA()));
                        }
                        break;
                    case FILE_CHANGED:
                        entry.setOldFileHash(fileEntry.getOldSHA());
                        entry.setNewFileHash(fileEntry.getNewSHA());
                        if (fileDAO.findOne(fileEntry.getNewSHA()) == null) {
                            missingContent.add(newDriftFile(fileEntry.getNewSHA()));
                        }
                        if (fileDAO.findOne(fileEntry.getOldSHA()) == null) {
                            missingContent.add(newDriftFile(fileEntry.getNewSHA()));
                        }
                        break;
                    default: // FILE_REMOVED
                        if (fileDAO.findOne(fileEntry.getOldSHA()) == null) {
                            missingContent.add(newDriftFile(fileEntry.getOldSHA()));
                        }
                    }
                    changeSet.add(entry);

                    // we are taking advantage of the fact that we know the summary is only used by the server
                    // if the change set is a DRIFT report. If its a coverage report, it is not used (we do
                    // not alert on coverage reports) - so don't waste memory by collecting all the paths
                    // when we know they aren't going to be used anyway.
                    if (headers.getType() == DriftChangeSetCategory.DRIFT) {
                        summary.addDriftPathname(path);
                    }
                }

                ds.save(changeSet);

                if (!missingContent.isEmpty()) {
                    AgentClient agent = getAgentManager().getAgentClient(subject, resourceId);
                    DriftAgentService driftService = agent.getDriftAgentService();
                    driftService.requestDriftFiles(resourceId, headers, missingContent);
                }

                return true;
            }
        });

        return summary;
    }

    private DriftFileDTO newDriftFile(String hash) {
        DriftFileDTO file = new DriftFileDTO();
        file.setHashId(hash);
        return file;
    }

    @Override
    public void saveChangeSetFiles(final Subject subject, final File changeSetFilesZip) throws Exception {
        String zipFileName = changeSetFilesZip.getName();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File dir = new File(tmpDir, zipFileName.substring(0, zipFileName.indexOf(".")));
        dir.mkdir();

        ZipUtil.unzipFile(changeSetFilesZip, dir);
        for (File file : dir.listFiles()) {
            fileDAO.save(file);
            file.delete();
        }

    }

    @Override
    public PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject,
        DriftChangeSetCriteria criteria) {
        Query<MongoDBChangeSet> query = ds.createQuery(MongoDBChangeSet.class).filter("resourceId =",
            criteria.getFilterResourceId());

        PageList<DriftChangeSetDTO> results = new PageList<DriftChangeSetDTO>();
        for (MongoDBChangeSet changeSet : query) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            Set<DriftDTO> entries = new HashSet<DriftDTO>();
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                entries.add(toDTO(entry, changeSetDTO));
            }
            changeSetDTO.setDrifts(entries);
            results.add(changeSetDTO);
        }

        return results;
        //return new PageList<DriftChangeSet>();
    }

    @Override
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        Query<MongoDBChangeSet> query = ds.createQuery(MongoDBChangeSet.class);
        boolean changeSetIdFiltered = false;

        if (criteria.getFilterId() != null) {
            String[] idFields = criteria.getFilterId().split(":");
            ObjectId changeSetId = new ObjectId(idFields[0]);
            String path = idFields[1];

            query.filter("files.path = ", path);
            query.filter("id = ", changeSetId);
        }

        if (!changeSetIdFiltered && criteria.getFilterChangeSetId() != null) {
            query.filter("id = ", new ObjectId(criteria.getFilterChangeSetId()));
        }

        PageList<DriftDTO> results = new PageList<DriftDTO>();

        for (MongoDBChangeSet changeSet : query) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                results.add((toDTO(entry, changeSetDTO)));
            }
        }

        return results;
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {
        Query<MongoDBChangeSet> query = ds.createQuery(MongoDBChangeSet.class).filter("files.category in ",
            criteria.getFilterCategories()).filter("resourceId in", criteria.getFilterResourceIds());

        PageList<DriftComposite> results = new PageList<DriftComposite>();
        Map<Integer, Resource> resources = loadResourceMap(subject, criteria.getFilterResourceIds());

        for (MongoDBChangeSet changeSet : query) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                // TODO: need to access config name
                results.add(new DriftComposite(toDTO(entry, changeSetDTO), resources.get(changeSet.getResourceId()),
                    "TODO"));
            }
        }

        return results;
    }

    @Override
    public DriftFile getDriftFile(Subject subject, String hashId) throws Exception {
        // TODO
        return null;
    }

    @Override
    public void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception {
        // TODO implement me!        
    }

    @Override
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        // TODO implement me!
        return 0;
    }

    @Override
    public String getDriftFileBits(Subject subject, String hash) {
        return null;
    }

    Map<Integer, Resource> loadResourceMap(Subject subject, Integer[] resourceIds) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterIds(resourceIds);

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
        // TODO copy resource id
        dto.setDriftDefinitionId(changeSet.getDriftDefinitionId());
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

    @Override
    public String persistChangeSet(Subject subject, DriftChangeSet<?> changeSet) {
        return null;
        // TODO Auto-generated method stub
    }

    @Override
    public String copyChangeSet(Subject subject, String changeSetId, int driftDefId, int resourceId) {
        // TODO Auto-generated method stub
        return null;
    }
}
