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
package org.rhq.core.domain.plugin;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

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
    @NamedQuery(name = ServerPlugin.QUERY_GET_KEYS_BY_ENABLED, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.PluginKey( " //
        + "        p.deployment, " //
        + "        p.type, " //
        + "        p.name) " //
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
        + "        pluginConfig, " //
        + "        jobsConfig, " //
        + "        p.type, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " //
        + "        LEFT JOIN p.pluginConfiguration AS pluginConfig " //
        + "        LEFT JOIN p.scheduledJobsConfiguration AS jobsConfig " //
        + "  WHERE p.id IN (:ids) " //
        + "        AND p.status = 'INSTALLED' "), //

    // gets plugins that are both installed and deleted
    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = ServerPlugin.QUERY_FIND_ALL_BY_IDS, query = "" //
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
        + "        pluginConfig, " //
        + "        jobsConfig, " //
        + "        p.type, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " // 
        + "        LEFT JOIN p.pluginConfiguration AS pluginConfig " //
        + "        LEFT JOIN p.scheduledJobsConfiguration AS jobsConfig " //
        + "  WHERE p.id IN (:ids) "), //

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
        + "        pluginConfig, " //
        + "        jobsConfig, " //
        + "        p.type, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p" //
        + "        LEFT JOIN p.pluginConfiguration AS pluginConfig " //
        + "        LEFT JOIN p.scheduledJobsConfiguration AS jobsConfig " //
        + "  WHERE p.name = :name " //
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
        + "        pluginConfig, " //
        + "        jobsConfig, " //
        + "        p.type, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " //
        + "        LEFT JOIN p.pluginConfiguration AS pluginConfig " //
        + "        LEFT JOIN p.scheduledJobsConfiguration AS jobsConfig " //
        + "  WHERE p.name=:name "), //

    // finds all installed AND deleted
    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = ServerPlugin.QUERY_FIND_ALL, query = "" //
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
        + "        pluginConfig, " //
        + "        jobsConfig, " //
        + "        p.type, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " //
        + "        LEFT JOIN p.pluginConfiguration AS pluginConfig " //
        + "        LEFT JOIN p.scheduledJobsConfiguration AS jobsConfig "),

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
        + "        pluginConfig, " //
        + "        jobsConfig, " //
        + "        p.type, " //
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM ServerPlugin AS p " //
        + "        LEFT JOIN p.pluginConfiguration AS pluginConfig " //
        + "        LEFT JOIN p.scheduledJobsConfiguration AS jobsConfig " //
        + "   WHERE p.status = 'INSTALLED' "), //

    // finds all installed - ignores those plugins marked as deleted
    // this query does not load the content blob or plugin or schedule configurations
    @NamedQuery(name = ServerPlugin.QUERY_FIND_DELETED, query = "" //
        + " SELECT p " //
        + "   FROM ServerPlugin AS p " //
        + "   WHERE p.status = 'DELETED' "), //

    // returns all installed plugins, both enabled and disabled
    // this is faster than QUERY_FIND_ALL_INSTALLED because it doesn't join configs
    @NamedQuery(name = ServerPlugin.QUERY_FIND_ALL_INSTALLED_KEYS, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.PluginKey( " //
        + "        p.deployment, " //
        + "        p.type, " //
        + "        p.name) " //
        + "   FROM ServerPlugin AS p " //
        + "  WHERE p.status = 'INSTALLED' "), //

    // returns all installed plugins, both enabled and disabled
    // this is faster than QUERY_FIND_BY_IDS because it doesn't join configs
    @NamedQuery(name = ServerPlugin.QUERY_FIND_KEYS_BY_IDS, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.PluginKey( " //
        + "        p.deployment, " //
        + "        p.type, " //
        + "        p.name) " //
        + "   FROM ServerPlugin AS p " //
        + "  WHERE p.id IN (:ids) " //
        + "        AND p.status = 'INSTALLED' "), //

    // returns two epoch millis - when plugin config and schedule jobs were last changed
    @NamedQuery(name = ServerPlugin.QUERY_GET_CONFIG_MTIMES, query = "" //
        + " SELECT pc.mtime, " // 
        + "        sjc.mtime " //
        + "   FROM ServerPlugin AS p " //
        + "        LEFT JOIN p.pluginConfiguration pc " //
        + "        LEFT JOIN p.scheduledJobsConfiguration sjc " //
        + "  WHERE p.id = :id"), //

    // this query is how you enable and disable plugins
    @NamedQuery(name = ServerPlugin.UPDATE_PLUGIN_ENABLED_BY_ID, query = "" //
        + "UPDATE ServerPlugin p " //
        + "   SET p.enabled = :enabled " //
        + " WHERE p.id = :id)"),

    @NamedQuery(
        name = ServerPlugin.QUERY_UNACKED_DELETED_PLUGINS,
        query = "SELECT p FROM ServerPlugin p WHERE p.status = 'DELETED' AND :serverId NOT MEMBER OF p.serversAcknowledgedDelete"
    )


})
@Entity
public class ServerPlugin extends AbstractPlugin {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_GET_STATUS_BY_NAME = "ServerPlugin.queryGetStatusByName";
    public static final String QUERY_GET_KEYS_BY_ENABLED = "ServerPlugin.queryGetKeysByEnabled";
    public static final String QUERY_FIND_BY_IDS = "ServerPlugin.findByIds";
    public static final String QUERY_FIND_ALL_BY_IDS = "ServerPlugin.findAllByIds";
    public static final String QUERY_FIND_BY_NAME = "ServerPlugin.findByName";
    public static final String QUERY_FIND_ANY_BY_NAME = "ServerPlugin.findAnyByName";
    public static final String QUERY_FIND_ALL = "ServerPlugin.findAll";
    public static final String QUERY_FIND_ALL_INSTALLED = "ServerPlugin.findAllInstalled";
    public static final String QUERY_FIND_DELETED = "ServerPlugin.findDeleted";
    public static final String QUERY_FIND_ALL_INSTALLED_KEYS = "ServerPlugin.findAllInstalledKeys";
    public static final String QUERY_FIND_KEYS_BY_IDS = "ServerPlugin.findKeysByIds";
    public static final String QUERY_GET_CONFIG_MTIMES = "ServerPlugin.getConfigMTimes";
    public static final String UPDATE_PLUGIN_ENABLED_BY_ID = "ServerPlugin.updatePluginEnabledById";
    public static final String QUERY_UNACKED_DELETED_PLUGINS = "ServerPlugin.unackedDeletedPlugins";

    @JoinColumn(name = "JOBS_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, optional = true)
    private Configuration scheduledJobsConfiguration;

    @JoinColumn(name = "PLUGIN_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, optional = true)
    private Configuration pluginConfiguration;

    @Column(name = "PTYPE")
    private String type;

    public ServerPlugin() {
        super();
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(String name, String path) {
        super(name, path);
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(String name, String path, String md5) {
        super(name, path, md5);
        setDeployment(PluginDeploymentType.SERVER);
    }

    public ServerPlugin(int id, String name, String path, String displayName, boolean enabled, PluginStatusType status,
        String description, String help, String md5, String version, String ampsVersion, Configuration pluginConfig,
        Configuration scheduledJobsConfig, String type, long ctime, long mtime) {

        super(id, name, path, displayName, enabled, status, description, help, md5, version, ampsVersion,
            PluginDeploymentType.SERVER, ctime, mtime);
        this.pluginConfiguration = pluginConfig;
        this.scheduledJobsConfiguration = scheduledJobsConfig;
        this.type = type;
    }

    @Override
    public void setDeployment(PluginDeploymentType deployment) {
        if (deployment != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("ServerPlugin can only ever have deployment type of SERVER: "
                + deployment);
        }
        super.setDeployment(deployment);
    }

    /**
     * If the plugin, itself, has configuration associated with it, this is that configuration.
     *
     * @return the configuration associated with the plugin itself
     */
    public Configuration getPluginConfiguration() {
        return this.pluginConfiguration;
    }

    public void setPluginConfiguration(Configuration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    /**
     * If the plugin has jobs associated with it, this is the configuration for those jobs.
     *
     * @return scheduled job configuration for jobs that the plugin defined.
     */
    public Configuration getScheduledJobsConfiguration() {
        return this.scheduledJobsConfiguration;
    }

    public void setScheduledJobsConfiguration(Configuration scheduledJobsConfiguration) {
        this.scheduledJobsConfiguration = scheduledJobsConfiguration;
    }

    /**
     * Plugin type string.
     *
     * @return plugin type
     */
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
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
