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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * An agent plugin.
 *
 * This object contains information about the plugin jar itself (e.g. its name and MD5).
 * It may also contain the jar contents ({@link #getContent()}).
 */
@DiscriminatorValue("AGENT")
@NamedQueries( {
//
    // this query is how you enable and disable plugins
    @NamedQuery(name = Plugin.UPDATE_PLUGIN_ENABLED_BY_ID, query = "" //
        + "UPDATE Plugin p " //
        + "   SET p.enabled = :enabled " //
        + " WHERE p.id = :id)"),

    // helps you determine if a plugin is installed or was deleted
    @NamedQuery(name = Plugin.QUERY_GET_STATUS_BY_NAME, query = "" //
        + " SELECT p.status " //
        + "   FROM Plugin AS p " //
        + "  WHERE p.name = :name)"), //

    // helps you determine which installed plugins are enabled or disabled
    @NamedQuery(name = Plugin.QUERY_GET_NAMES_BY_ENABLED, query = "" //
        + " SELECT p.name " //
        + "   FROM Plugin AS p " //
        + "  WHERE p.enabled = :enabled " //
        + "        AND p.status = 'INSTALLED' "), //

    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = Plugin.QUERY_FIND_BY_IDS, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.Plugin( " //
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
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM Plugin AS p " //
        + "  WHERE p.id IN (:ids) " //
        + "        AND p.status = 'INSTALLED' "), //

    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = Plugin.QUERY_FIND_ALL_BY_IDS, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.Plugin( " //
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
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM Plugin AS p " //
        + "  WHERE p.id IN (:ids) "), //

    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = Plugin.QUERY_FIND_BY_NAME, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.Plugin( " //
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
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM Plugin AS p " //
        + "  WHERE p.name=:name " //
        + "        AND p.status = 'INSTALLED' "), //

    // gets the plugin, even if it is deleted
    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = Plugin.QUERY_FIND_ANY_BY_NAME, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.Plugin( " //
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
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM Plugin AS p " //
        + "  WHERE p.name=:name "), //

    @NamedQuery(name = Plugin.QUERY_FIND_ALL, query =
          " SELECT new org.rhq.core.domain.plugin.Plugin( "
        + "        p.id, "
        + "        p.name, "
        + "        p.path, "
        + "        p.displayName, "
        + "        p.enabled, "
        + "        p.status, "
        + "        p.description, "
        + "        p.help, "
        + "        p.md5, "
        + "        p.version, "
        + "        p.ampsVersion, "
        + "        p.ctime, "
        + "        p.mtime) "
        + "   FROM Plugin AS p "),

    // finds all installed - ignores those plugins marked as deleted
    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = Plugin.QUERY_FIND_ALL_INSTALLED, query = "" //
        + " SELECT new org.rhq.core.domain.plugin.Plugin( " //
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
        + "        p.ctime, " //
        + "        p.mtime) " //
        + "   FROM Plugin AS p " //
        + "   WHERE p.status = 'INSTALLED' "), //

    @NamedQuery(name = Plugin.QUERY_FIND_ALL_DELETED, query =
          " SELECT new org.rhq.core.domain.plugin.Plugin( "
        + "        p.id, "
        + "        p.name, "
        + "        p.path, "
        + "        p.displayName, "
        + "        p.enabled, "
        + "        p.status, "
        + "        p.description, "
        + "        p.help, "
        + "        p.md5, "
        + "        p.version, "
        + "        p.ampsVersion, "
        + "        p.ctime, "
        + "        p.mtime) "
        + "   FROM Plugin AS p "
        + "   WHERE p.status = 'DELETED'"),

    // this query is how you enable and disable plugins
    @NamedQuery(name = Plugin.UPDATE_PLUGINS_ENABLED_BY_IDS, query = "" //
        + "UPDATE Plugin p " //
        + "   SET p.enabled = :enabled " //
        + " WHERE p.id IN (:ids)"),

    // this query does not load the content blob, but loads everything else
    @NamedQuery(name = Plugin.QUERY_FIND_BY_RESOURCE_TYPE_AND_CATEGORY, query = "" //
        + "  SELECT new org.rhq.core.domain.plugin.Plugin( " //
        + "         p.id, " //
        + "         p.name, " //
        + "         p.path, " //
        + "         p.displayName, " //
        + "         p.enabled, " //
        + "         p.status, " //
        + "         p.description, " //
        + "         p.help, " //
        + "         p.md5, " //
        + "         p.version, " //
        + "         p.ampsVersion, " //
        + "         p.ctime, " //
        + "         p.mtime) " //
        + "    FROM Plugin p " //
        + "   WHERE p.status = 'INSTALLED' AND " //
        + "         p.name IN ( SELECT rt.plugin " //
        + "                       FROM Resource res " //
        + "                       JOIN res.resourceType rt " //
        + "                      WHERE ( rt.category = :resourceCategory OR :resourceCategory IS NULL ) " //
        + "                        AND ( rt.name = :resourceTypeName OR :resourceTypeName IS NULL ) ) " //
        + " ORDER BY p.name"), //

    @NamedQuery(name = Plugin.QUERY_FIND_ALL_TO_PURGE, query = ""
        + "  SELECT new org.rhq.core.domain.plugin.Plugin( " //
        + "         p.id, "
        + "         p.name, "
        + "         p.path, "
        + "         p.displayName, "
        + "         p.enabled, "
        + "         p.status, "
        + "         p.description, "
        + "         p.help, "
        + "         p.md5, "
        + "         p.version, "
        + "         p.ampsVersion, "
        + "         p.ctime, "
        + "         p.mtime) "
        + "    FROM Plugin p "
        + "    WHERE p.ctime = -1 AND p.status = 'DELETED'"
    ),

    @NamedQuery(
        name = Plugin.QUERY_UNACKED_DELETED_PLUGINS,
        query = "SELECT p FROM Plugin p WHERE p.status = 'DELETED' AND :serverId NOT MEMBER OF p.serversAcknowledgedDelete"
    )

})
@Entity
public class Plugin extends AbstractPlugin {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_GET_STATUS_BY_NAME = "Plugin.queryGetStatusByName";
    public static final String QUERY_GET_NAMES_BY_ENABLED = "Plugin.queryGetNamesByEnabled";
    public static final String QUERY_FIND_BY_IDS = "Plugin.findByIds";
    public static final String QUERY_FIND_ALL_BY_IDS = "Plugin.findAllByIds";
    public static final String QUERY_FIND_BY_NAME = "Plugin.findByName";
    public static final String QUERY_FIND_ANY_BY_NAME = "Plugin.findAnyByName";
    public static final String QUERY_FIND_ALL = "Plugin.findAll";
    public static final String QUERY_FIND_ALL_INSTALLED = "Plugin.findAllInstalled";
    public static final String QUERY_FIND_ALL_DELETED = "Plugin.findAllDeleted";
    public static final String QUERY_FIND_ALL_TO_PURGE = "Plugin.findAllToPurge";
    public static final String UPDATE_PLUGINS_ENABLED_BY_IDS = "Plugin.updatePluginsEnabledByIds";
    public static final String QUERY_FIND_BY_RESOURCE_TYPE_AND_CATEGORY = "Plugin.findByResourceType";
    public static final String UPDATE_PLUGIN_ENABLED_BY_ID = "Plugin.updatePluginEnabledById";
    public static final String QUERY_UNACKED_DELETED_PLUGINS = "Plugin.unackedDeletedPlugins";

    /**
     * @deprecated This used to identify a named query that no longer exist. Do not use this for anything.
     */
    @Deprecated
    public static final String PURGE_PLUGINS = "Plugin.purgePlugins";

    /**
     * @deprecated This is no longer used and means nothing now.
     */
    @Deprecated
    public static final long PURGED = -1;

    public Plugin() {
        super();
        setDeployment(PluginDeploymentType.AGENT);
    }

    public Plugin(String name, String path) {
        super(name, path);
        setDeployment(PluginDeploymentType.AGENT);
    }

    public Plugin(String name, String path, String md5) {
        super(name, path, md5);
        setDeployment(PluginDeploymentType.AGENT);
    }

    public Plugin(int id, String name, String path, String displayName, boolean enabled, PluginStatusType status,
        String description, String help, String md5, String version, String ampsVersion, long ctime, long mtime) {

        super(id, name, path, displayName, enabled, status, description, help, md5, version, ampsVersion,
            PluginDeploymentType.AGENT, ctime, mtime);
    }

    @Override
    public void setDeployment(PluginDeploymentType deployment) {
        if (deployment != PluginDeploymentType.AGENT) {
            throw new IllegalArgumentException("Plugin can only ever have deployment type of AGENT: " + deployment);
        }
        super.setDeployment(deployment);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof Plugin)) {
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
        return "AgentPlugin " + super.toString();
    }
}
