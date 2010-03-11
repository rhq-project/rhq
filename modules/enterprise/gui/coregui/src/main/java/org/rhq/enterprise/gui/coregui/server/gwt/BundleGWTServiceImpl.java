/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;
import java.util.Map;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BundleGWTServiceImpl extends AbstractGWTServiceImpl implements BundleGWTService {
    private static final long serialVersionUID = 1L;

    private BundleManagerLocal bundleManager = LookupUtil.getBundleManager();

    public List<BundleType> getBundleTypes() {
        List<BundleType> bundleTypes = bundleManager.getAllBundleTypes(getSessionSubject());
        return SerialUtility.prepare(bundleTypes, "getBundleTypes");
    }

    public PageList<Bundle> findBundlesByCriteria(BundleCriteria criteria) {
        PageList<Bundle> results = bundleManager.findBundlesByCriteria(getSessionSubject(), criteria);
        return SerialUtility.prepare(results, "findBundlesByCriteria");
    }

    public PageList<BundleVersion> findBundleVersionsByCriteria(BundleVersionCriteria criteria) {
        PageList<BundleVersion> results = bundleManager.findBundleVersionsByCriteria(getSessionSubject(), criteria);
        return SerialUtility.prepare(results, "findBundleVersionsByCriteria");
    }

    public Bundle createBundle(String name, int bundleTypeId) throws Exception {
        Bundle results;
        try {
            results = bundleManager.createBundle(getSessionSubject(), name, bundleTypeId);
        } catch (Exception e) {
            throw new Exception(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "createBundle");
    }

    public BundleVersion createBundleVersion(int bundleId, String name, String version, String recipe) throws Exception {
        BundleVersion results;
        try {
            results = bundleManager.createBundleVersion(getSessionSubject(), bundleId, name, version, recipe);
        } catch (Exception e) {
            throw new Exception(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "createBundleVersion");
    }

    public BundleVersion createBundleAndBundleVersion(String bundleName, int bundleTypeId, String name, String version,
        String recipe) throws Exception {
        BundleVersion results;
        try {
            results = bundleManager.createBundleAndBundleVersion(getSessionSubject(), bundleName, bundleTypeId, name,
                version, recipe);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "createBundleAndBundleVersion");
    }

    public Map<String, Boolean> getAllBundleVersionFilenames(int bundleVersionId) throws Exception {
        Map<String, Boolean> results;
        try {
            results = bundleManager.getAllBundleVersionFilenames(getSessionSubject(), bundleVersionId);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "getAllBundleVersionFilenames");
    }

}