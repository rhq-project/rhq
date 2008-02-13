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
package org.rhq.plugins.platform.content.yum;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Represents a context in which the yum server objects can interface with the rest of the system. By insulating this
 * interface, the code can be utilized by any part of the system simply by re-implementing the context.
 *
 * @author jortel
 */
@SuppressWarnings("unchecked")
public class PluginContext implements YumContext {
    private final URL baseurl;
    private final ResourceContext resourceContext;
    private final ContentContext contentContext;

    public PluginContext(int port, ResourceContext resourceContext, ContentContext contentContext)
        throws MalformedURLException {
        this.resourceContext = resourceContext;
        this.contentContext = contentContext;
        this.baseurl = new URL("http://localhost:" + port + "/yum");
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#baseurl()
     */
    public URL baseurl() {
        return baseurl;
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#basepath()
     */
    public String basepath() {
        return baseurl.getPath();
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#port()
     */
    public int port() {
        return baseurl.getPort();
    }

    /* (non-Javadoc)
     * @see
     * org.jboss.on.plugins.platform.content.yum.YumContext#writePackageBits(org.jboss.on.domain.content.PackageDetailsKey,
     * java.io.OutputStream)
     */
    public long writePackageBits(PackageDetailsKey key, OutputStream ostr) {
        return getContentServices().downloadPackageBits(contentContext, key, ostr);
    }

    /* (non-Javadoc)
     * @see
     * org.jboss.on.plugins.platform.content.yum.YumContext#writePackageBits(org.jboss.on.domain.content.PackageDetailsKey,
     * long[], java.io.OutputStream)
     */
    public long writePackageBits(PackageDetailsKey key, long[] range, OutputStream ostr) {
        return getContentServices().downloadPackageBitsRange(contentContext, key, ostr, range[0], range[1]);
    }

    /* (non-Javadoc)
     * @see
     * org.jboss.on.plugins.platform.content.yum.YumContext#getPackageBitsLength(org.jboss.on.domain.content.PackageDetailsKey)
     */
    public long getPackageBitsLength(PackageDetailsKey key) {
        return getContentServices().getPackageBitsLength(contentContext, key);
    }

    /* (non-Javadoc)
     * @see
     * org.jboss.on.plugins.platform.content.yum.YumContext#getPackageVersionMetadata(org.jboss.on.domain.util.PageControl)
     */
    public PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(PageControl pc) {
        return getContentServices().getPackageVersionMetadata(contentContext, pc);
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#getResourceSubscriptionMD5()
     */
    public String getResourceSubscriptionMD5() {
        return getContentServices().getResourceSubscriptionMD5(contentContext);
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#getTemporaryDirectory()
     */
    public File getTemporaryDirectory() {
        return resourceContext.getTemporaryDirectory();
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#getPluginConfiguration()
     */
    public Configuration getPluginConfiguration() {
        return resourceContext.getPluginConfiguration();
    }

    /* (non-Javadoc)
     * @see org.jboss.on.plugins.platform.content.yum.YumContext#getMetadataCacheTimeout()
     */
    public long getMetadataCacheTimeout() {
        long time = 1800L;
        Configuration config = getPluginConfiguration();
        if (config != null) {
            PropertySimple p = config.getSimple("metadataCacheTimeout");
            if (p != null) {
                Long timeLong = p.getLongValue();
                if (timeLong != null) {
                    time = (timeLong.longValue());
                }
            }
        }

        return time;
    }

    private ContentServices getContentServices() {
        return contentContext.getContentServices();
    }
}