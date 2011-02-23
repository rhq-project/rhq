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
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.InstalledPackageHistoryCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ContentGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Simeon Pinder
 * @author Greg Hinkle
 */
public class ContentGWTServiceImpl extends AbstractGWTServiceImpl implements ContentGWTService {

    private static final long serialVersionUID = 1L;

    private ContentManagerLocal contentManager = LookupUtil.getContentManager();

    //TODO: spinder. should the become it's own GWTService?
    private ContentUIManagerLocal contentUiManager = LookupUtil.getContentUIManager();

    public void deletePackageVersion(int packageVersionId) throws RuntimeException {
        try {
            contentManager.deletePackageVersion(getSessionSubject(), packageVersionId);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<PackageVersion> findPackageVersionsByCriteria(PackageVersionCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(contentManager.findPackageVersionsByCriteria(getSessionSubject(), criteria),
                "ContentService.findPackageVersionsByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<InstalledPackageHistory> findInstalledPackageHistoryByCriteria(
        InstalledPackageHistoryCriteria criteria) throws RuntimeException {
        try {
            PageList<InstalledPackageHistory> results = SerialUtility.prepare(contentUiManager
                .findInstalledPackageHistoryByCriteria(getSessionSubject(), criteria),
                "ContentService.findInstalledPackageHistoryByCriteria");
            return results;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<InstalledPackageHistory> getInstalledPackageHistoryForResource(int resourceId, int count)
        throws RuntimeException {
        try {
            PageControl pc = new PageControl(0, count);
            return SerialUtility.prepare(contentUiManager.getInstalledPackageHistoryForResource(resourceId, pc),
                "ContentService.getInstalledPackageHistoryForResource");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<Architecture> getArchitectures() throws RuntimeException {
        try {
            return SerialUtility.prepare(contentManager.findArchitectures(getSessionSubject()),
                "ContentService.getArchitectures");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PackageType getResourceCreationPackageType(int resourceTypeId) throws RuntimeException {
        try {
            return SerialUtility.prepare(contentManager.getResourceCreationPackageType(resourceTypeId),
                "ContentService.getResourceCreationPackageType");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }
}
