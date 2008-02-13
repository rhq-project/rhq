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
package org.rhq.core.pc.plugin;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

/**
 * Holds a collection of URLs retrieved elsewhere and returns them as the found plugin URLs.
 *
 * @author Jason Dobies
 */
public class SimplePluginFinder implements PluginFinder {
    private Collection<URL> urls = new HashSet<URL>();

    public SimplePluginFinder() {
    }

    public SimplePluginFinder(URL url) {
        urls.add(url);
    }

    public SimplePluginFinder(Collection<URL> startingUrls) {
        urls.addAll(startingUrls);
    }

    public void addUrl(URL url) {
        urls.add(url);
    }

    public Collection<URL> findPlugins() {
        return urls;
    }
}