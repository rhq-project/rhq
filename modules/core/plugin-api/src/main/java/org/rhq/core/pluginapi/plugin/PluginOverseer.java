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
package org.rhq.core.pluginapi.plugin;

/**
 * Performs global initialization and shutdown of a plugin. This class will
 * be notified when the plugin is loaded and unloaded. This provides a place
 * for plugin developers to allocate global resources needed by all plugin components and
 * a place to clean up those resources.
 *
 * @author John Mazzitelli
 */
public interface PluginOverseer {
    /**
     * When a plugin is being loaded, this method will be called.
     * When this is called, no plugin resource or discovery components have been instantiated
     * or invoked yet. This is merely called to notify the plugin that it is about to be
     * placed into service and its components will soon be asked to discovery and manage
     * resources.
     * 
     * @param context a context with information about the runtime plugin environment
     * 
     * @throws Exception if the plugin finds that it should not be loaded, an exception should be thrown.
     *                   If thrown, this plugin will not be loaded and will be considered disabled. If
     *                   other plugins are dependent on this failed plugin, those other plugins will
     *                   fail to operate correctly.
     */
    void initialize(PluginContext context) throws Exception;

    /**
     * When the plugin container shuts down and it unloads the plugin, this method will be called.
     * This provides an opportunity for the plugin to clean up any global resources it has previously
     * allocated. After this method is called, invocations of this plugin's discovery or
     * resource components will no longer occur. 
     */
    void shutdown();
}
