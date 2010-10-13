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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.Set;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.TagGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.tagging.TagManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class TagGWTServiceImpl extends AbstractGWTServiceImpl implements TagGWTService {

    private static final long serialVersionUID = 1L;

    private TagManagerLocal tagManager = LookupUtil.getTagManager();

    public PageList<Tag> findTagsByCriteria(TagCriteria tagCriteria) {
        try {
            return SerialUtility.prepare(tagManager.findTagsByCriteria(getSessionSubject(), tagCriteria),
                "TagService.findTagsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Set<Tag> addTags(Set<Tag> tags) {
        try {
            return SerialUtility.prepare(tagManager.addTags(getSessionSubject(), tags), "TagService.addTags");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void removeTags(Set<Tag> tags) {
        try {
            tagManager.removeTags(getSessionSubject(), tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateResourceTags(int resourceId, Set<Tag> tags) {
        try {
            tagManager.updateResourceTags(getSessionSubject(), resourceId, tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateResourceGroupTags(int resourceGroupId, Set<Tag> tags) {
        try {
            tagManager.updateResourceGroupTags(getSessionSubject(), resourceGroupId, tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateBundleTags(int bundleId, Set<Tag> tags) {
        try {
            tagManager.updateBundleTags(getSessionSubject(), bundleId, tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateBundleVersionTags(int bundleVersionId, Set<Tag> tags) {
        try {
            tagManager.updateBundleVersionTags(getSessionSubject(), bundleVersionId, tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateBundleDeploymentTags(int bundleDeploymentId, Set<Tag> tags) {
        try {
            tagManager.updateBundleDeploymentTags(getSessionSubject(), bundleDeploymentId, tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateBundleDestinationTags(int bundleDestinationId, Set<Tag> tags) {
        try {
            tagManager.updateBundleDestinationTags(getSessionSubject(), bundleDestinationId, tags);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<TagReportComposite> findTagReportCompositesByCriteria(TagCriteria tagCriteria) {
        try {
            return SerialUtility.prepare(
                tagManager.findTagReportCompositesByCriteria(getSessionSubject(), tagCriteria),
                "TagService.findTagReportCompositesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}
