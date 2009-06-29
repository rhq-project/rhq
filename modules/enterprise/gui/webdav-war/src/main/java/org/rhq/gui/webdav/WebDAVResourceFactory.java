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
package org.rhq.gui.webdav;

import java.util.Arrays;
import java.util.List;

import com.bradmcevoy.http.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The main entry point when a WebDAV request comes in. This will examine
 * the URL being requested and convert that URL into a series of WebDAV
 * resources for the user to see.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class WebDAVResourceFactory implements com.bradmcevoy.http.ResourceFactory {

    private static final Log LOG = LogFactory.getLog(WebDAVResourceFactory.class);

    public Resource getResource(String host, String url) {
        Resource foundResource = null;

        // URL == /webdav/resource/<parent-resource-name>/<child-resource-name>/...
        // paths[] = [, webdav, resource, <parent-resource-name>, <child-resource-name>, ...]
        String[] paths = url.split("/");

        if (paths.length >= 3) {
            String webdavResourceType = paths[2];
            if ("resource".equals(webdavResourceType)) {
                foundResource = getResourceResource(getSubarray(paths, 3));
            }
            // TODO: some ideas for addition resource types
            // else if "group".equals(webdavResourceType)
            // else if "user".equals(webdavResourceType)
            // else if "role".equals(webdavResourceType)
            // else if "plugin".equals(webdavResourceType)
            // else if "systemconfig".equals(webdavResourceType)
            // else if "agentupdate/download".equals(webdavResourceType)
            // else if "agentupdate/version".equals(webdavResourceType)
        }

        if (foundResource == null) {
            LOG.warn("Couldn't find resource associated with webdav url [" + url + "]");
        }

        return foundResource;
    }

    public String getSupportedLevels() {
        return "1";
    }

    private String[] getSubarray(String[] full, int index) {
        return Arrays.copyOfRange(full, index, full.length);
    }

    public Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }

    private Resource getResourceResource(String[] paths) {
        if (paths.length == 0 || paths[0] == null || paths[0].length() == 0) {
            return new RootFolder();
        } else {
            Resource resourceFolder = new RootFolder();
            for (int i = 0; i < paths.length; i++) {
                boolean found = false;
                if (resourceFolder instanceof AuthenticatedCollectionResource) {
                    List<? extends Resource> children;
                    children = ((AuthenticatedCollectionResource) resourceFolder).getChildren(getOverlord());
                    for (Resource child : children) {
                        if (child.getName().equals(paths[i])) {
                            resourceFolder = child;
                            found = true;
                        }
                    }
                }
                if (!found) {
                    return null;
                }
            }
            return resourceFolder;
        }
    }
}
