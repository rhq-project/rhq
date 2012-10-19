 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.platform.content.yum;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

public interface YumContext {
    /**
     * Get the base URL for yum requests.
     *
     * @return The base URL for yum requests.
     */
    public abstract URL baseurl();

    /**
     * Get the <i>path</i> part of the base URL for yum requests.
     *
     * @return The <i>path</i> part of the base URL.
     */
    public abstract String basepath();

    /**
     * Get the <i>port</i> specified in the base URL for yum requests.
     *
     * @return The <i>port</i> specified in the base URL.
     */
    public abstract int port();

    /**
     * Write the specified package bits to the specified stream.
     *
     * @param  key  The package key used to specify the package version.
     * @param  ostr An open output stream.
     *
     * @return The number of bytes written to the stream.
     */
    public abstract long writePackageBits(PackageDetailsKey key, OutputStream ostr);

    /**
     * Write the specified package bits to the specified stream.
     *
     * @param  key   The package key used to specify the package version.
     * @param  range The byte range to write (range[0]=first-byte, range[1]=last-byte).
     * @param  ostr  An open output stream.
     *
     * @return The number of bytes written to the stream.
     */
    public abstract long writePackageBits(PackageDetailsKey key, long[] range, OutputStream ostr);

    /**
     * Get the length in bytes of the specified package.
     *
     * @param  key The package key used to specify the package version.
     *
     * @return The length in bytes.
     */
    public abstract long getPackageBitsLength(PackageDetailsKey key);

    /**
     * Get a list of all metadata entries for all packages mapped to the specified resource.
     *
     * @param  pc A page control.
     *
     * @return A collection of package version metadata blobs.
     */
    public abstract PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(PageControl pc);

    /**
     * Get the MD5 of the resource's content subscription.
     *
     * @return MD5 hashcode that identifies the resource content subscription contents
     */
    public abstract String getResourceSubscriptionMD5();

    /**
     * Get a reference to the temporary directory to be used for file construction and staging.
     *
     * @return The temporary file location.
     */
    public abstract File getTemporaryDirectory();

    /**
     * Returns the plugin configuration.
     *
     * @return plugin configuration
     */
    public abstract Configuration getPluginConfiguration();

    /**
     * Get the metadata timeout, in <i>milliseconds</i>. We assume cached metadata is still valid.
     *
     * @return The timeout, in <i>milliseconds</i>
     */
    public abstract long getMetadataCacheTimeout();
}