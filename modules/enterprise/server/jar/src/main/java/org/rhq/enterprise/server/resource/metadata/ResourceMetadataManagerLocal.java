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
package org.rhq.enterprise.server.resource.metadata;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Provides functionality surrounding agent plugins and their resource metadata.
 */
@Local
public interface ResourceMetadataManagerLocal {

    void removeObsoleteTypes(Subject subject, String pluginName, PluginMetadataManager pluginMetadataMgr);

    /** Exists only to for transactional boundary reasons. Not for general consumption. */
    void removeObsoleteSubCategories(Subject subject, ResourceType newType, ResourceType existingType);

    /** Exists only to for transactional boundary reasons. Not for general consumption. */
    void getPluginTypes(Subject subject, String pluginName, Set<ResourceType> legitTypes,
        Set<ResourceType> obsoleteTypes, PluginMetadataManager pluginMetadataMgr);

    /** Exists only to for transactional boundary reasons. Not for general consumption. */
    void completeRemoveResourceType(Subject subject, ResourceType existingType);

    void updateTypes(Set<ResourceType> resourceTypes) throws Exception;
}