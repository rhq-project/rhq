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
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * Defines an adapter that can speak to a particular external package source. This definition includes information on
 * how to connect/configure the external source as well as the Java implementation to use to perform the actual
 * connection. Each source type represents a different mechanism for loading package information into the server.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = ContentSourceType.QUERY_FIND_ALL, query = "SELECT cst " + "  FROM ContentSourceType cst "
        + "       LEFT JOIN FETCH cst.contentSourceConfigurationDefinition d " + "       LEFT JOIN FETCH d.templates "),
    @NamedQuery(name = ContentSourceType.QUERY_FIND_BY_NAME_WITH_CONFIG_DEF, query = "SELECT cst "
        + "  FROM ContentSourceType cst " + "       LEFT JOIN FETCH cst.contentSourceConfigurationDefinition d "
        + "       LEFT JOIN FETCH d.templates " + " WHERE cst.name = :name"),
    @NamedQuery(name = ContentSourceType.QUERY_FIND_BY_NAME, query = "SELECT cst " + "  FROM ContentSourceType cst "
        + " WHERE cst.name = :name") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONTENT_SOURCE_TYPE_ID_SEQ")
@Table(name = "RHQ_CONTENT_SOURCE_TYPE")
public class ContentSourceType implements Serializable {

    public static final String QUERY_FIND_ALL = "ContentSourceType.findAll";
    public static final String QUERY_FIND_BY_NAME_WITH_CONFIG_DEF = "ContentSourceType.findByNameWithConfigDef";
    public static final String QUERY_FIND_BY_NAME = "ContentSourceType.findByName";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DISPLAY_NAME", nullable = true)
    private String displayName;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "PLUGIN_NAME", nullable = true)
    private String pluginName; // in the future, we might want to add this to the natural key

    @Column(name = "DEFAULT_LAZY_LOAD", nullable = false)
    private boolean defaultLazyLoad;

    @Column(name = "DEFAULT_DOWNLOAD_MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DownloadMode defaultDownloadMode = DownloadMode.DATABASE;

    @Column(name = "DEFAULT_SYNC_SCHEDULE", nullable = true)
    private String defaultSyncSchedule = "0 0 3 * * ?";

    @JoinColumn(name = "SOURCE_CONFIG_DEF_ID", nullable = true)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @XmlTransient
    private ConfigurationDefinition contentSourceConfigurationDefinition;

    @Column(name = "API_CLASS", nullable = false)
    private String contentSourceApiClass;

    @OneToMany(mappedBy = "contentSourceType", fetch = FetchType.LAZY)
    private Set<ContentSource> contentSources;

    public ContentSourceType() {
        // for JPA use
    }

    public ContentSourceType(String name) {
        this.name = name;
        this.contentSourceApiClass = "undefined";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Programmatic name of the repo source type.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Name of this repo source type that is suitable for display to the user in the UI.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Free text description of this repo source type.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The name of the plugin that defined this content source type.
     */
    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    /**
     * If <code>true</code>, the content bits for all packages coming from content sources of this type will only be
     * loaded on demand. This means the package contents will only be loaded when they are asked for the very first
     * time. If <code>false</code>, the content source should attempt to download all packages as soon as possible. <i>
     * Note:</i> this is only a default, suggested value - the user will be given the chance to override this setting
     * when creating content sources.
     */
    public boolean isDefaultLazyLoad() {
        return defaultLazyLoad;
    }

    public void setDefaultLazyLoad(boolean defaultLazyLoad) {
        this.defaultLazyLoad = defaultLazyLoad;
    }

    /**
     * The default download mode for instances of this content source type. Download mode indicates where (and even if)
     * package bits are downloaded.
     *
     * @return the download mode
     */
    public DownloadMode getDefaultDownloadMode() {
        return defaultDownloadMode;
    }

    public void setDefaultDownloadMode(DownloadMode defaultDownloadMode) {
        this.defaultDownloadMode = defaultDownloadMode;
    }

    /**
     * Periodically, the content source plugin adapter will be asked to synchronize with the remote content source
     * repository. This gives the adapter a chance to see if any content has been added, updated or removed from the
     * remote repository. This attribute defines the schedule, as a cron string. The default will be set for everyday at
     * 3:00am local time. If content sources of this type should never automatically sync, this should be null.
     * Note that individual content sources of this type can override this setting.
     *
     * @return sync schedule as a cron string or <code>null</code> if the sync should not automatically occur
     */
    public String getDefaultSyncSchedule() {
        return defaultSyncSchedule;
    }

    public void setDefaultSyncSchedule(String syncSchedule) {
        if (syncSchedule != null && syncSchedule.trim().length() == 0) {
            syncSchedule = null;
        }

        this.defaultSyncSchedule = syncSchedule;
    }

    /**
     * Defines the configuration properties that must be set when creating a content source of this type. Typically,
     * this will define the properties necessary for specifying how to connect to the underlying source.
     */
    public ConfigurationDefinition getContentSourceConfigurationDefinition() {
        return contentSourceConfigurationDefinition;
    }

    public void setContentSourceConfigurationDefinition(ConfigurationDefinition contentSourceConfigurationDefinition) {
        this.contentSourceConfigurationDefinition = contentSourceConfigurationDefinition;
    }

    /**
     * Indicates the Java class that should be instantiated to be the content source and used to connect to the
     * underlying external source.
     * 
     * This class will be an implementation of {@link org.rhq.enterprise.server.plugin.pc.content.ContentProvider}.
     */
    public String getContentSourceApiClass() {
        return contentSourceApiClass;
    }

    public void setContentSourceApiClass(String contentSourceApiClass) {
        this.contentSourceApiClass = contentSourceApiClass;
    }

    /**
     * Content sources of this type.
     */
    public Set<ContentSource> getContentSources() {
        return contentSources;
    }

    public void addContentSource(ContentSource contentSource) {
        if (this.contentSources == null) {
            this.contentSources = new HashSet<ContentSource>();
        }

        this.contentSources.add(contentSource);
        contentSource.setContentSourceType(this);
    }

    public void setContentSources(Set<ContentSource> contentSources) {
        this.contentSources = contentSources;
    }

    @Override
    public String toString() {
        return "ContentSourceType: name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof ContentSourceType))) {
            return false;
        }

        final ContentSourceType other = (ContentSourceType) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }
}