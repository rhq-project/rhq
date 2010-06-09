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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
public interface TagGWTService extends RemoteService {


    PageList<Tag> findTagsByCriteria(TagCriteria tagCriteria);

    Set<Tag> addTags(Set<Tag> tags);

    void removeTags(Set<Tag> tags);

    void updateResourceTags(int resourceId, Set<Tag> tags);

    void updateResourceGroupTags(int resourceGroupId, Set<Tag> tags);

    void updateBundleTags(int bundleId, Set<Tag> tags);

    void updateBundleVersionTags(int bundleVersionId, Set<Tag> tags);

    void updateBundleDeploymentTags(int bundleDeploymentId, Set<Tag> tags);

    void updateBundleDestinationTags(int bundleDestinationId, Set<Tag> tags);

    PageList<TagReportComposite> findTagReportCompositesByCriteria(TagCriteria tagCriteria);
}
