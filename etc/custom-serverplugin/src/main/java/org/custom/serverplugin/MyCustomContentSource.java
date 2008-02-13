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
package org.custom.serverplugin;

import java.io.InputStream;
import java.util.Collection;
import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.configuration.Configuration;

/**
 * This is a template example of a custom server-side plugin that can be used to start development of your own content
 * source.
 */
public class MyCustomContentSource implements ContentSourceAdapter {
    public void initialize(Configuration configuration) throws Exception {
    }

    public void shutdown() {
    }

    public void testConnection() throws Exception {
    }

    public InputStream getInputStream(String location) throws Exception {
        return null;
    }

    public void synchronizePackages(PackageSyncReport report, Collection<ContentSourcePackageDetails> existingPackages)
        throws Exception {
    }
}