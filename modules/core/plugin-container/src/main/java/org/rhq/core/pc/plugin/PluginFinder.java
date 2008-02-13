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

/**
 * Implementations of this object have the sole purpose of finding plugins and reporting where they are found. These
 * objects do not do anything like parsing the plugin descriptors or even loading the jars. They just find the jars and
 * report on their whereabouts.
 *
 * @author Jason Dobies
 */
public interface PluginFinder {
    /**
     * Finds all plugins accessible to the plugin container and returns URLs that point to each plugin jar that is
     * found.
     *
     * @return collection of URLS pointing to each plugin to be loaded
     */
    Collection<URL> findPlugins();
}