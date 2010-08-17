/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.tagging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * @author Greg Hinkle
 */
@Stateless
public class TagManagerBean implements TagManagerLocal, TagManagerRemote {

    private final Log log = LogFactory.getLog(TagManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    public PageList<Tag> findTagsByCriteria(Subject subject, TagCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        CriteriaQueryRunner<Tag> queryRunner = new CriteriaQueryRunner<Tag>(criteria, generator, entityManager);

        return queryRunner.execute();
    }

    public Set<Tag> addTags(Subject subject, Set<Tag> tags) {
        Set<Tag> results = new HashSet<Tag>();

        // This isn't efficient, but then how many tags will you really be creating at once
        for (Tag tag : tags) {
            TagCriteria criteria = new TagCriteria();
            criteria.addFilterNamespace(tag.getNamespace());
            criteria.addFilterSemantic(tag.getSemantic());
            criteria.addFilterName(tag.getName());
            criteria.setStrict(true);
            List<Tag> found = findTagsByCriteria(subject, criteria);
            if (!found.isEmpty()) {
                assert found.size() == 1; // should never be more than one
                results.add(found.get(0));
            } else {
                entityManager.persist(tag);
                results.add(tag);
            }
        }
        return results;
    }

    public void removeTags(Subject subject, Set<Tag> tags) {
        // This isn't efficient, but then how many tags will you really be deleting at once
        for (Tag tag : tags) {
            entityManager.remove(entityManager.find(Tag.class, tag.getId()));
        }
    }

    public void updateResourceTags(Subject subject, int resourceId, Set<Tag> tags) {
        if (!authorizationManager.hasResourcePermission(subject, Permission.MODIFY_RESOURCE, resourceId)) {
            throw new PermissionException("You do not have permission to modify resource");
        }

        Set<Tag> definedTags = addTags(subject, tags);
        Resource resource = entityManager.find(Resource.class, resourceId);

        Set<Tag> previousTags = new HashSet<Tag>(resource.getTags());
        previousTags.removeAll(definedTags);
        for (Tag tag : previousTags) {
            tag.removeResource(resource);
        }

        for (Tag tag : definedTags) {
            tag.addResource(resource);
        }
    }

    public void updateResourceGroupTags(Subject subject, int resourceGroupId, Set<Tag> tags) {
        if (!authorizationManager.hasGroupPermission(subject, Permission.MODIFY_RESOURCE, resourceGroupId)) {
            throw new PermissionException("You do not have permission to modify group");
        }

        Set<Tag> definedTags = addTags(subject, tags);
        ResourceGroup group = entityManager.find(ResourceGroup.class, resourceGroupId);

        Set<Tag> previousTags = new HashSet<Tag>(group.getTags());
        previousTags.removeAll(definedTags);
        for (Tag tag : previousTags) {
            tag.removeResourceGroup(group);
        }

        for (Tag tag : definedTags) {
            tag.addResourceGroup(group);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    // todo verify
    public void updateBundleTags(Subject subject, int bundleId, Set<Tag> tags) {

        Set<Tag> definedTags = addTags(subject, tags);
        Bundle bundle = entityManager.find(Bundle.class, bundleId);

        Set<Tag> previousTags = new HashSet<Tag>(bundle.getTags());
        previousTags.removeAll(definedTags);
        for (Tag tag : previousTags) {
            tag.removeBundle(bundle);
        }

        for (Tag tag : definedTags) {
            tag.addBundle(bundle);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    // todo verify
    public void updateBundleVersionTags(Subject subject, int bundleVersionId, Set<Tag> tags) {

        Set<Tag> definedTags = addTags(subject, tags);
        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);

        Set<Tag> previousTags = new HashSet<Tag>(bundleVersion.getTags());
        previousTags.removeAll(definedTags);
        for (Tag tag : previousTags) {
            tag.removeBundleVersion(bundleVersion);
        }

        for (Tag tag : definedTags) {
            tag.addBundleVersion(bundleVersion);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    // todo verify
    public void updateBundleDeploymentTags(Subject subject, int bundleDeploymentId, Set<Tag> tags) {

        Set<Tag> definedTags = addTags(subject, tags);
        BundleDeployment bundleDeployment = entityManager.find(BundleDeployment.class, bundleDeploymentId);

        Set<Tag> previousTags = new HashSet<Tag>(bundleDeployment.getTags());
        previousTags.removeAll(definedTags);
        for (Tag tag : previousTags) {
            tag.removeBundleDeployment(bundleDeployment);
        }

        for (Tag tag : definedTags) {
            tag.addBundleDeployment(bundleDeployment);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    // todo verify
    public void updateBundleDestinationTags(Subject subject, int bundleDestinationId, Set<Tag> tags) {

        Set<Tag> definedTags = addTags(subject, tags);
        BundleDestination bundleDestination = entityManager.find(BundleDestination.class, bundleDestinationId);

        Set<Tag> previousTags = new HashSet<Tag>(bundleDestination.getTags());
        previousTags.removeAll(definedTags);
        for (Tag tag : previousTags) {
            tag.removeBundleDestination(bundleDestination);
        }

        for (Tag tag : definedTags) {
            tag.addBundleDestination(bundleDestination);
        }
    }

    public PageList<TagReportComposite> findTagReportCompositesByCriteria(Subject subject, TagCriteria tagCriteria) {
        // TODO criteria stuff

        Query query = entityManager.createNamedQuery(Tag.QUERY_TAG_COMPOSITE_REPORT);

        PageList<TagReportComposite> result = new PageList<TagReportComposite>();

        result.addAll(query.getResultList());

        return result;
    }

}
