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
package org.rhq.core.domain.plugin;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;

/**
 * A server plugin.
 * 
 * This object contains information about the plugin jar itself (e.g. its name and MD5).
 * It may also contain the jar contents ({@link #getContent()}).
 */
@DiscriminatorValue("SERVER")
@NamedQueries( {
//
    // helps you determine if a plugin is installed or was deleted
    @NamedQuery(name = ServerPlugin.QUERY_GET_STATUS_BY_NAME, query = "" //
        + " SELECT p.status " //
        + "   FROM ServerPlugin AS p " //
        + "  WHERE p.name = :name)"), //

    // helps you determine which installed plugins are enabled or disabled
    @NamedQuery(name = ServerPlugin.QUERY_GET_NAMES_BY_ENABLED, query = "" //
        + " SELECT p.name " //
        + "   FROM ServerPlugin AS p " //
        + "  WHERE p.enabled = :enabled " // 
        + "        AND p.status = 'INSTALLED' "), //

    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = ServerPlugin.QUERY_FIND_BY_IDS, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.ServerPlugin( " //
        + "        p.id, " //
        + "        p.name, " //
        + "        p.path, " //
        + "        p.displayName, " //
        + "        p.enabled, " //
        + "        p.status, " //
        + "        p.description, " //
        + "        p.help, " //
        + "        p.md5, " //
        + "        p.version, " //
        + "        p.ampsVersion, " //
        + "        p.deployment, " //
        + "        p.pluginConfiguration, " //
        + "        p.scheduledJobsConfiguration, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " // 
        + "        LEFT JOIN p.pluginConfiguration " // 
        + "        LEFT JOIN p.scheduledJobsConfiguration " // 
        + "  WHERE p.id IN (:ids) " //
        + "        AND p.status = 'INSTALLED' "), //

    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = ServerPlugin.QUERY_FIND_BY_NAME, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.ServerPlugin( " //
        + "        p.id, " //
        + "        p.name, " //
        + "        p.path, " //
        + "        p.displayName, " //
        + "        p.enabled, " //
        + "        p.status, " //
        + "        p.description, " //
        + "        p.help, " //
        + "        p.md5, " //
        + "        p.version, " //
        + "        p.ampsVersion, " //
        + "        p.deployment, " //
        + "        p.pluginConfiguration, " //
        + "        p.scheduledJobsConfiguration, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " // 
        + "        LEFT JOIN p.pluginConfiguration " // 
        + "        LEFT JOIN p.scheduledJobsConfiguration " // 
        + "  WHERE p.name=:name " //
        + "        AND p.status = 'INSTALLED' "), //

    // gets the plugin, even if it is deleted
    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = ServerPlugin.QUERY_FIND_ANY_BY_NAME, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.ServerPlugin( " //
        + "        p.id, " //
        + "        p.name, " //
        + "        p.path, " //
        + "        p.displayName, " //
        + "        p.enabled, " //
        + "        p.status, " //
        + "        p.description, " //
        + "        p.help, " //
        + "        p.md5, " //
        + "        p.version, " //
        + "        p.ampsVersion, " //
        + "        p.deployment, " //
        + "        p.pluginConfiguration, " //
        + "        p.scheduledJobsConfiguration, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " // 
        + "        LEFT JOIN p.pluginConfiguration " // 
        + "        LEFT JOIN p.scheduledJobsConfiguration " // 
        + "  WHERE p.name=:name "), //

    // finds all installed - ignores those plugins marked as deleted
    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = ServerPlugin.QUERY_FIND_ALL_INSTALLED, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.ServerPlugin( " //
        + "        p.id, " //
        + "        p.name, " //
        + "        p.path, " //
        + "        p.displayName, " //
        + "        p.enabled, " //
        + "        p.status, " //
        + "        p.description, " //
        + "        p.help, " //
        + "        p.md5, " //
        + "        p.version, " //
        + "        p.ampsVersion, " //
        + "        p.deployment, " //
        + "        p.pluginConfiguration, " //
        + "        p.scheduledJobsConfiguration, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " //
        + "        LEFT JOIN p.pluginConfiguration " // 
        + "        LEFT JOIN p.scheduledJobsConfiguration " //
        + "   WHERE p.status = 'INSTALLED' "), //

    // this query is how you enable and disable plugins
    @NamedQuery(name = ServerPlugin.UPDATE_PLUGINS_ENABLED_BY_IDS, query = "" //
        + "UPDATE ServerPlugin p " //
        + "   SET p.enabled = :enabled " //
        + " WHERE p.id IN (:ids)")

})
@Entity
public class ServerPlugin extends AbstractPlugin {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_GET_STATUS_BY_NAME = "ServerPlugin.queryGetStatusByName";
    public static final String QUERY_GET_NAMES_BY_ENABLED = "ServerPlugin.queryGetNamesByEnabled";
    public static final String QUERY_FIND_BY_IDS = "ServerPlugin.findByIds";
    public static final String QUERY_FIND_BY_NAME = "ServerPlugin.findByName";
    public static final String QUERY_FIND_ANY_BY_NAME = "ServerPlugin.findAnyByName";
    public static final String QUERY_FIND_ALL_INSTALLED = "ServerPlugin.findAllInstalled";
    public static final String UPDATE_PLUGINS_ENABLED_BY_IDS = "ServerPlugin.updatePluginsEnabledByIds";

    public ServerPlugin() {
        super();
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(@NotNull String name, String path) {
        super(name, path);
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(String name, String path, String md5) {
        super(name, path, md5);
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(String name, String path, byte[] content) {
        super(name, path, content);
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(int id, String name, String path, String displayName, boolean enabled, PluginStatusType status,
        String description, String help, String md5, String version, String ampsVersion,
        PluginDeploymentType deployment, Configuration pluginConfig, Configuration scheduledJobsConfig, long ctime,
        long mtime) {

        super(id, name, path, displayName, enabled, status, description, help, md5, version, ampsVersion, deployment,
            pluginConfig, scheduledJobsConfig, ctime, mtime);

        if (deployment != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("ServerPlugin must only ever be of deployment type == SERVER: "
                + deployment);
        }
    }

    @Override
    public void setDeployment(PluginDeploymentType deployment) {
        if (deployment != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("ServerPlugin can only ever have deployment type of SERVER: "
                + deployment);
        }
        super.setDeployment(deployment);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof ServerPlugin)) {
            return false;
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "ServerPlugin " + super.toString();
    }
}
