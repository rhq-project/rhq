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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftConfigurationCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.util.PageList;

import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;

public class DriftServerServiceImpl implements DriftServerService {
    @Override
    public void sendChangesetZip(int resourceId, long zipSize, InputStream zipStream) {
        try {
            DriftManagerLocal driftManager = getDriftManager();
            driftManager.addChangeSet(resourceId, zipSize, zipStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendFilesZip(int resourceId, long zipSize, InputStream zipStream) {
        try {
            DriftManagerLocal driftManager = getDriftManager();
            driftManager.addFiles(resourceId, zipSize, zipStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<Integer, List<DriftConfiguration>> getDriftConfigurations(Set<Integer> resourceIds) {
        DriftConfigurationCriteria criteria = new DriftConfigurationCriteria();
        criteria.addFilterResourceIds(resourceIds.toArray(new Integer[resourceIds.size()]));
        criteria.fetchConfiguration(true);

        Subject overlord = getSubjectManager().getOverlord();
        PageList<DriftConfiguration> configs = getDriftManager().findDriftConfigurationsByCriteria(overlord, criteria);

        Map<Integer, List<DriftConfiguration>> map = new HashMap<Integer, List<DriftConfiguration>>();
        for (Integer resourceId : resourceIds) {
            map.put(resourceId, new ArrayList<DriftConfiguration>());
        }
        for (DriftConfiguration c : configs) {
            List<DriftConfiguration> list = map.get(c.getResource().getId());
            list.add(c);
            map.put(c.getResource().getId(), list);
        }

        return map;
    }

    @Override
    public DriftSnapshot getCurrentSnapshot(int driftConfigurationId) {
        DriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftConfigurationId(driftConfigurationId);

        Subject overlord = getSubjectManager().getOverlord();

        try {
            return getDriftManager().createSnapshot(overlord, criteria);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
