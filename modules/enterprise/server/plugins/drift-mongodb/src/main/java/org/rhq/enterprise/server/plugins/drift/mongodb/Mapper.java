/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.drift.dto.DriftDTO;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;

public class Mapper {
    
    public DriftChangeSetDTO toDTO(MongoDBChangeSet changeSet) {
        DriftChangeSetDTO dto = new DriftChangeSetDTO();
        dto.setId(changeSet.getId());
        dto.setResourceId(changeSet.getResourceId());
        dto.setDriftDefinitionId(changeSet.getDriftDefinitionId());
        dto.setDriftHandlingMode(changeSet.getDriftHandlingMode());
        dto.setVersion(changeSet.getVersion());
        dto.setCtime(changeSet.getCtime());
        dto.setCategory(changeSet.getCategory());

        return dto;
    }
    
    public DriftDTO toDTO(MongoDBChangeSetEntry entry) {
        DriftDTO dto = new DriftDTO();
        dto.setId(entry.getId());
        dto.setCategory(entry.getCategory());
        dto.setCtime(entry.getCtime());
        dto.setPath(entry.getPath());
        dto.setDirectory(entry.getDirectory());
        if (entry.getNewFileHash() != null) {
            dto.setNewDriftFile(newDriftFileDTO(entry.getNewFileHash()));
        }
        if (entry.getOldFileHash() != null) {
            dto.setOldDriftFile(newDriftFileDTO(entry.getOldFileHash()));
        }

        return null;
    }

    private DriftFileDTO newDriftFileDTO(String hash) {
        DriftFileDTO dto = new DriftFileDTO();
        dto.setHashId(hash);
        return dto;
    }

}
