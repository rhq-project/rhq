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

import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
@Remote
public interface TagManagerRemote {


    PageList<Tag> findTagsByCriteria(Subject subject, TagCriteria tagCriteria);

    Set<Tag> addTags(Subject subject, Set<Tag> tags);

    void removeTags(Subject subject, Set<Tag> tags);


    void updateResourceTags(Subject subject, int resourceId, Set<Tag> tags);

    void updateResourceGroupTags(Subject subject, int resourceGroupId, Set<Tag> tags);

    void updateBundleTags(Subject subject, int bundleId, Set<Tag> tags);

    void updateBundleVersionTags(Subject subject, int bundleVersionId, Set<Tag> tags);

    void updateBundleDeploymentTags(Subject subject, int bundleDeploymentId, Set<Tag> tags);

    void updateBundleDestinationTags(Subject subject, int bundleDestinationId, Set<Tag> tags);

    PageList<TagReportComposite> findTagReportCompositesByCriteria(Subject subject, TagCriteria tagCriteria);

}
