/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc;

/**
 * Performs global initialization and shutdown of a server side plugin. This provides a place
 * for plugin developers to allocate global resources needed by all plugin components and
 * a place to clean up those resources.
 * 
 * Lifecycle listeners are required of generic server plugins because they provide
 * the only mechanism to start and stop.
 *
 * @author John Mazzitelli
 */
public interface ServerPluginLifecycleListener {
    /**
     * When a plugin's container loads a plugin, this method will be called.
     * When this is called, no other plugin components have been instantiated
     * or invoked yet. This is merely called to notify the plugin that it is about to be
     * placed into service and its components will soon be asked to perform work.
     * 
     * If this method returns normally, the plugin is saying it is ready and able to {@link #start()}.
     * 
     * @param context a context with information about the runtime plugin environment
     * 
     * @throws Exception if the plugin finds that it should not be loaded, an exception should be thrown.
     *                   If thrown, this plugin will not be started and will be considered disabled. If
     *                   other plugins are dependent on this failed plugin, those other plugins will
     *                   fail to operate correctly.
     */
    void initialize(ServerPluginContext context) throws Exception;

    /**
     * When a plugin container has been started (which occurs after all plugins are loaded), this
     * method is called to inform the plugin is can begin performing work.
     */
    void start();

    /**
     * When a plugin container has been stopped (but before it unloads plugins), this
     * method is called to inform the plugin it should stop performing work and prepare
     * to shutdown.
     */
    void stop();

    /**
     * When the plugin container shuts down and it unloads the plugin, this method will be called.
     * This provides an opportunity for the plugin to clean up any global resources it has previously
     * allocated. After this method is called, invocations of this plugin's discovery or
     * resource components will no longer occur. 
     */
    void shutdown();
}
