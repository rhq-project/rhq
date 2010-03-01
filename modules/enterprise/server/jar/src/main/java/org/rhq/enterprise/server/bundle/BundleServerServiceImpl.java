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
package org.rhq.enterprise.server.bundle;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.bundle.BundleStatusUpdate;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Server-side implementation of the <code>BundleServerService</code>. This implmentation simply forwards
 * the requests to the appropriate session bean.
 *
 * @author John Mazzitelli
 */
public class BundleServerServiceImpl implements BundleServerService {
    private final Log log = LogFactory.getLog(this.getClass());

    public void updateStatus(BundleStatusUpdate update) {
        // TODO Auto-generated method stub
    }

    public List<PackageVersion> getAllBundleVersionPackageVersions(int bundleVersionId) {
        try {
            BundleManagerLocal bm = LookupUtil.getBundleManager();
            BundleVersionCriteria c = new BundleVersionCriteria();
            Subject subject = LookupUtil.getSubjectManager().getOverlord();
            c.addFilterId(bundleVersionId);
            c.fetchBundleFiles(true);
            List<BundleVersion> bundleVersions = bm.findBundleVersionsByCriteria(subject, c);
            List<BundleFile> bundleFiles = bundleVersions.get(0).getBundleFiles();
            List<PackageVersion> packageVersions = new ArrayList<PackageVersion>(bundleFiles.size());
            for (BundleFile bundleFile : bundleFiles) {
                packageVersions.add(bundleFile.getPackageVersion());
            }
            return packageVersions;
        } catch (Exception e) {
            log.error("Failed to obtain bundle files for bundle version id: " + bundleVersionId, e);
            throw new WrappedRemotingException(e);
        }
    }

    public long downloadPackageBits(PackageVersion packageVersion, OutputStream outputStream) {
        try {
            ContentSourceManagerLocal csm = LookupUtil.getContentSourceManager();
            long size = csm.outputPackageVersionBits(packageVersion, outputStream);
            return size;
        } catch (Exception e) {
            log.error("Failed to obtain package version bits for package version: " + packageVersion, e);
            throw new WrappedRemotingException(e);
        }
    }
}