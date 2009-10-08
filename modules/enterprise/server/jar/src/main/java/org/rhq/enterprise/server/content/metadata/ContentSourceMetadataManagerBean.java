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
package org.rhq.enterprise.server.content.metadata;

import java.util.HashSet;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;

/**
 * Used to work with metadata defining content source types.
 */
@Stateless
public class ContentSourceMetadataManagerBean implements ContentSourceMetadataManagerLocal {
    private final Log log = LogFactory.getLog(ContentSourceMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataManager;

    public void registerTypes(Set<ContentSourceType> typesToRegister) {
        Set<ContentSourceType> existingTypes = new HashSet<ContentSourceType>();

        Query q = entityManager.createNamedQuery(ContentSourceType.QUERY_FIND_ALL);
        existingTypes.addAll(q.getResultList());

        Set<ContentSourceType> typesToPersist = missingInFirstSet(existingTypes, typesToRegister);
        Set<ContentSourceType> typesToDelete = missingInFirstSet(typesToRegister, existingTypes);
        Set<ContentSourceType> typesToUpdate = intersection(existingTypes, typesToRegister); // order of params is important

        // persist any types that need to be registered but do not currently exist
        for (ContentSourceType typeToPersist : typesToPersist) {
            entityManager.persist(typeToPersist);
        }

        // Remove any types that currently exist but are not in the types to register.
        // Note that we only remove types that have no content sources associated with them.
        // If someone removes a plugin but fails to delete content sources associate with their types,
        // then we spit out a warning but leave the type definition in the database.
        for (ContentSourceType typeToDelete : typesToDelete) {
            Set<ContentSource> attachedSources = typeToDelete.getContentSources();
            if ((attachedSources == null) || (attachedSources.size() == 0)) {
                entityManager.remove(typeToDelete);
            } else {
                log.warn(typeToDelete.toString() + " is no longer supported by any deployed plugin, "
                    + "but it still has some content sources associated with it. "
                    + "Those content sources will no longer work.");
            }
        }

        // we now have to merge existing types with their new definitions
        for (ContentSourceType typeToUpdate : typesToUpdate) {
            for (ContentSourceType typeToRegister : typesToRegister) {
                if (typeToRegister.equals(typeToUpdate)) {
                    typeToUpdate.setDisplayName(typeToRegister.getDisplayName());
                    typeToUpdate.setDescription(typeToRegister.getDescription());
                    typeToUpdate.setContentSourceApiClass(typeToRegister.getContentSourceApiClass());
                    typeToUpdate.setDefaultSyncSchedule(typeToRegister.getDefaultSyncSchedule());
                    typeToUpdate.setDefaultLazyLoad(typeToRegister.isDefaultLazyLoad());
                    typeToUpdate.setDefaultDownloadMode(typeToRegister.getDefaultDownloadMode());
                    updateConfigurationDefinition(typeToRegister, typeToUpdate);
                    break;
                }
            }
        }

        return;
    }

    private void updateConfigurationDefinition(ContentSourceType newType, ContentSourceType existingType) {
        ConfigurationDefinition newConfig = newType.getContentSourceConfigurationDefinition();
        ConfigurationDefinition existingConfig = existingType.getContentSourceConfigurationDefinition();

        if (newConfig != null) {
            if (existingConfig == null) {
                // everything is new
                entityManager.persist(newConfig);
                existingType.setContentSourceConfigurationDefinition(newConfig);
            } else {
                // both new and existing had some kind of configuration, update the existing to match the new
                configurationMetadataManager.updateConfigurationDefinition(newConfig, existingConfig);
            }
        } else {
            // the new config is null -> remove the existing config
            if (existingConfig != null) {
                existingType.setContentSourceConfigurationDefinition(null);
                entityManager.remove(existingConfig);
            }
        }
    }

    /**
     * Return a set containing those element that are in reference, but not in first. Both input sets are not modified
     *
     * @param  first
     * @param  reference
     *
     * @return
     */
    private <T> Set<T> missingInFirstSet(Set<T> first, Set<T> reference) {
        Set<T> result = new HashSet<T>();

        if (reference != null) {
            // First collection is null -> everything is missing
            if (first == null) {
                result.addAll(reference);
                return result;
            }

            // else loop over the set and sort out the right items.
            for (T item : reference) {
                if (!first.contains(item)) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Return a new Set with elements both in first and second passed collection.
     *
     * @param  first  First set
     * @param  second Second set
     *
     * @return a new set (depending on input type) with elements in first and second
     */
    private <T> Set<T> intersection(Set<T> first, Set<T> second) {
        Set<T> result = new HashSet<T>();

        if ((first != null) && (second != null)) {
            result.addAll(first);
            result.retainAll(second);
        }

        return result;
    }
}