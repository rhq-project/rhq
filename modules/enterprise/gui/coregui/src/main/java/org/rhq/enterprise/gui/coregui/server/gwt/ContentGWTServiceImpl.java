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

import java.util.List;

import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ContentGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ContentGWTServiceImpl extends AbstractGWTServiceImpl implements ContentGWTService {

    private static final long serialVersionUID = 1L;

    private ContentManagerLocal contentManager = LookupUtil.getContentManager();
    private ContentUIManagerLocal contentUiManager = LookupUtil.getContentUIManager();

    public void deletePackageVersion(int packageVersionId) {
        try {
            contentManager.deletePackageVersion(getSessionSubject(), packageVersionId);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<PackageVersion> findPackageVersionsByCriteria(PackageVersionCriteria criteria) {
        try {
            return SerialUtility.prepare(contentManager.findPackageVersionsByCriteria(getSessionSubject(), criteria),
                "ContentService.findPackageVersionsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    //    public PageList<InstalledPackageHistory> getInstalledPackageHistoryForResource(int resourceId, int count) {
    //        try {
    //            PageControl pc = new PageControl(0, count);
    //            return SerialUtility.prepare(contentUiManager.getInstalledPackageHistoryForResource(resourceId, pc),
    //                "ContentService.getInstalledPackageHistoryForResource");
    //        } catch (Exception e) {
    //            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
    //        }
    //    }

    public List<Architecture> getArchitectures() {
        try {
            return SerialUtility.prepare(contentManager.findArchitectures(getSessionSubject()),
                "ContentService.getArchitectures");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PackageType getResourceCreationPackageType(int resourceTypeId) {
        try {
            return SerialUtility.prepare(contentManager.getResourceCreationPackageType(resourceTypeId),
                "ContentService.getResourceCreationPackageType");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}
