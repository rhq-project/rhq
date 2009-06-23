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

import java.util.List;

import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.CollectionResource;

/**
 * @author Greg Hinkle
 */
public class WebDAVResourceFactory implements com.bradmcevoy.http.ResourceFactory {


    public Resource getResource(String host, String url) {
        FolderResource folder;

        String[] paths = url.split("/");

        if ("resource".equals(paths[2])) {

            if (paths.length < 4 || paths[3] == null) {

                return new RootFolder();

            } else {

                Resource res = new RootFolder();

                for (int i = 3; i < paths.length; i++) {
                    String idString = paths[i];

                    boolean found = false;

                    if (res instanceof CollectionResource) {
                        List<? extends Resource> children = ((CollectionResource) res).getChildren();
                        for (Resource ch : children) {
                            if (ch.getName().equals(paths[i])) {
                                res = ch;
                                found = true;
                            }
                        }
                    }

                    if (!found) {
                        System.out.println("Couldn't find [" + paths[i] + "] of " + url);
                        return null;
                    }

                }

                return res;

            }
        }

        return null;
    }


    public String getSupportedLevels() {
        return "1";
    }
}
