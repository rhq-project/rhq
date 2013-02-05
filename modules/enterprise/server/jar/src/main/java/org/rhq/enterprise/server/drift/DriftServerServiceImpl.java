/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.server.drift;

import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;

public class DriftServerServiceImpl implements DriftServerService {

    private Log log = LogFactory.getLog(DriftServerServiceImpl.class);

    @Override
    public void sendChangesetZip(int resourceId, long zipSize, InputStream zipStream) {
        try {
            DriftManagerLocal driftManager = getDriftManager();
            Subject overlord = getSubjectManager().getOverlord();
            driftManager.addChangeSet(overlord, resourceId, zipSize, zipStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendFilesZip(int resourceId, String driftDefinitionName, String token, long zipSize,
        InputStream zipStream) {
        try {
            DriftManagerLocal driftManager = getDriftManager();
            Subject overlord = getSubjectManager().getOverlord();
            driftManager.addFiles(overlord, resourceId, driftDefinitionName, token, zipSize, zipStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void repeatChangeSet(int resourceId, String driftDefName, int version) {
        DriftManagerLocal driftManager = getDriftManager();
        driftManager.processRepeatChangeSet(resourceId, driftDefName, version);
    }

    @Override
    public Map<Integer, List<DriftDefinition>> getDriftDefinitions(Set<Integer> resourceIds) {
        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        criteria.addFilterResourceIds(resourceIds.toArray(new Integer[resourceIds.size()]));
        criteria.fetchConfiguration(true);
        criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

        Subject overlord = getSubjectManager().getOverlord();
        PageList<DriftDefinition> definitions = getDriftManager().findDriftDefinitionsByCriteria(overlord, criteria);

        Map<Integer, List<DriftDefinition>> result = new HashMap<Integer, List<DriftDefinition>>();

        for (DriftDefinition driftDef : definitions) {
            Integer resourceId = driftDef.getResource().getId();
            List<DriftDefinition> list = result.get(resourceId);
            if (null == list) {
                list = new ArrayList<DriftDefinition>();
                result.put(resourceId, list);
            }
            list.add(driftDef);
        }

        return result;
    }

    @Override
    public DriftSnapshot getCurrentSnapshot(int driftDefinitionId) {
        Subject overlord = getSubjectManager().getOverlord();

        return getDriftManager().getSnapshot(overlord, new DriftSnapshotRequest(driftDefinitionId));
    }

    @Override
    public DriftSnapshot getSnapshot(int driftDefinitionId, int startVersion, int endVersion) {
        Subject overlord = getSubjectManager().getOverlord();

        return getDriftManager().getSnapshot(overlord,
            new DriftSnapshotRequest(driftDefinitionId, startVersion, endVersion));
    }

    @Override
    public void updateCompliance(int resourceId, String drfitDefName, DriftComplianceStatus complianceStatus) {
        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        criteria.addFilterResourceIds(resourceId);
        criteria.addFilterName(drfitDefName);
        criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

        DriftManagerLocal driftMgr = getDriftManager();
        SubjectManagerLocal subjectMgr = getSubjectManager();
        Subject overlord = subjectMgr.getOverlord();

        PageList<DriftDefinition> definitions = driftMgr.findDriftDefinitionsByCriteria(overlord, criteria);

        if (definitions.isEmpty()) {
            log.warn("Cannot update compliance for [resourceId: " + resourceId + ", driftDefinitionName: " +
                drfitDefName + "]. Could not find drift definition.");
            return;
        }

        DriftDefinition definition = definitions.get(0);
        definition.setComplianceStatus(complianceStatus);
        driftMgr.updateDriftDefinition(overlord, definition);
    }
}
