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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Represents a real connection to an external source system. The type of external source system is described by the
 * {@link ContentSourceType} attribute of this class. The other attributes in this class serve to configure and control
 * the connection to the package source.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries({
    @NamedQuery(name = ContentSource.QUERY_FIND_ALL, query = "SELECT cs FROM ContentSource cs "),
    @NamedQuery(name = ContentSource.QUERY_FIND_ALL_WITH_CONFIG, query = "SELECT cs FROM ContentSource cs LEFT JOIN FETCH cs.configuration"),
    @NamedQuery(name = ContentSource.QUERY_FIND_BY_NAME_AND_TYPENAME, query = "SELECT cs " + "  FROM ContentSource cs "
        + "       LEFT JOIN FETCH cs.configuration" + " WHERE cs.name = :name "
        + "   AND cs.contentSourceType.name = :typeName "),
    @NamedQuery(name = ContentSource.QUERY_FIND_BY_ID_WITH_CONFIG, query = "SELECT cs " + "  FROM ContentSource cs "
        + "       LEFT JOIN FETCH cs.configuration" + " WHERE cs.id = :id "),
    @NamedQuery(name = ContentSource.QUERY_FIND_BY_REPO_ID, // do not do a fetch join here
    query = "SELECT cs FROM ContentSource cs LEFT JOIN cs.repoContentSources ccs WHERE ccs.repo.id = :id"),
    @NamedQuery(name = ContentSource.QUERY_FIND_AVAILABLE_BY_REPO_ID, //
    query = "SELECT cs " //  
        + "    FROM ContentSource AS cs " // 
        + "   WHERE cs.id NOT IN " //
        + "       ( SELECT ccs.contentSource.id " // 
        + "           FROM RepoContentSource ccs " //
        + "          WHERE ccs.repo.id = :repoId ) ") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONTENT_SOURCE_ID_SEQ")
@Table(name = "RHQ_CONTENT_SOURCE")
public class ContentSource implements Serializable {
    public static final String QUERY_FIND_ALL = "ContentSource.findAll";
    public static final String QUERY_FIND_ALL_WITH_CONFIG = "ContentSource.findAllWithConfig";
    public static final String QUERY_FIND_BY_NAME_AND_TYPENAME = "ContentSource.findByNameAndTypeName";
    public static final String QUERY_FIND_BY_ID_WITH_CONFIG = "ContentSource.findByIdWithConfig";
    public static final String QUERY_FIND_BY_REPO_ID = "ContentSource.findByRepoId";
    public static final String QUERY_FIND_AVAILABLE_BY_REPO_ID = "ContentSource.findAvailableByRepoId";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "CONTENT_SOURCE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ContentSourceType contentSourceType;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "LAZY_LOAD", nullable = false)
    private boolean lazyLoad;

    @Column(name = "DOWNLOAD_MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DownloadMode downloadMode = DownloadMode.DATABASE;

    @Column(name = "SYNC_SCHEDULE", nullable = true)
    private String syncSchedule = "0 0 3 * * ?";

    @Column(name = "LOAD_ERROR_MESSAGE", nullable = true)
    private String loadErrorMessage;

    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Configuration configuration;

    @Column(name = "CREATION_TIME", nullable = false)
    private long creationDate;

    @Column(name = "LAST_MODIFIED_TIME", nullable = false)
    private long lastModifiedDate;

    @OneToMany(mappedBy = "contentSource", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @OrderBy("startTime DESC")
    // latest appears first, oldest last
    private List<ContentSourceSyncResults> syncResults;

    @OneToMany(mappedBy = "contentSource", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<RepoContentSource> repoContentSources;

    public ContentSource() {
        // for JPA use
    }

    public ContentSource(String name, ContentSourceType contentSourceType) {
        this.name = name;
        this.contentSourceType = contentSourceType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Describes the capabilities of this content source.
     */
    public ContentSourceType getContentSourceType() {
        return contentSourceType;
    }

    public void setContentSourceType(ContentSourceType contentSourceType) {
        this.contentSourceType = contentSourceType;
    }

    /**
     * User defined programmatic name of this repo source. This name should not contain spaces or special characters.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * User defined description to help distinguish the content source in the UI.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * If <code>true</code>, the content bits for all packages coming from this content source will only be loaded on
     * demand. This means the package contents will only be loaded when they are asked for the very first time. If
     * <code>false</code>, the content source should attempt to download all packages as soon as possible.
     */
    public boolean isLazyLoad() {
        return lazyLoad;
    }

    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    /**
     * Download mode indicates where (and even if) package bits are downloaded, when they are downloaded. Note that if
     * this value is {@link DownloadMode#NEVER}, {@link #isLazyLoad()} is effectively ignored since package bits will
     * never be loaded.
     *
     * @return the download mode
     */
    public DownloadMode getDownloadMode() {
        return downloadMode;
    }

    public void setDownloadMode(DownloadMode downloadMode) {
        this.downloadMode = downloadMode;
    }

    /**
     * Periodically, the content source plugin adapter will be asked to synchronize with the remote content source
     * repository. This gives the adapter a chance to see if any content has been added, updated or removed from the
     * remote repository. This attribute defines the schedule, as a cron string. The default will be set for everyday at
     * 3:00am local time. If this content source should never automatically sync, this should be null, but
     * an empty string would also indicate this, too.
     *
     * @return sync schedule as a cron string or null if the sync should not automatically occur
     */
    public String getSyncSchedule() {
        return syncSchedule;
    }

    public void setSyncSchedule(String syncSchedule) {
        if (syncSchedule != null && syncSchedule.trim().length() == 0) {
            syncSchedule = null;
        }
        this.syncSchedule = syncSchedule;
    }

    /**
     * If not <code>null</code>, this will be an error message to indicate what errors occurred during the last attempt
     * to load package bits from this content source. If <code>null</code>, either no error occurred or this content
     * source has yet to attempt to load package bits for any of its packages.
     */
    public String getLoadErrorMessage() {
        return loadErrorMessage;
    }

    public void setLoadErrorMessage(String loadErrorMessage) {
        this.loadErrorMessage = loadErrorMessage;
    }

    /**
     * Values that dictate how this content source will function. These values fulfill the configuration definition
     * defined in the {@link #getContentSourceType() content source type} associated with this instance. In other words,
     * these values indicate what underlying source of packages to use and how to connect to it.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Timestamp of when this content source was created.
     */
    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Timestamp of the last time the {@link #getConfiguration() configuration} of this content source was changed. It
     * is not necessarily the last time any other part of this content source object was changed (for example, this last
     * modified date does not necessarily correspond to the time when the description was modified).
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * The list of sync results - order not ensured
     */
    public List<ContentSourceSyncResults> getSyncResults() {
        return syncResults;
    }

    public void addSyncResult(ContentSourceSyncResults syncResult) {
        if (this.syncResults == null) {
            this.syncResults = new ArrayList<ContentSourceSyncResults>();
        }

        this.syncResults.add(syncResult);
        syncResult.setContentSource(this);
    }

    public void setSyncResults(List<ContentSourceSyncResults> syncResults) {
        this.syncResults = syncResults;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getRepoContentSources()
     */
    public Set<RepoContentSource> getRepoContentSources() {
        return repoContentSources;
    }

    /**
     * The repos that this content source provides content to.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated repos, use
     * {@link #getRepoContentSources()} or {@link #addRepo(Repo)}, {@link #removeRepo(Repo)}.</p>
     */
    public Set<Repo> getRepos() {
        HashSet<Repo> repos = new HashSet<Repo>();

        if (repoContentSources != null) {
            for (RepoContentSource ccs : repoContentSources) {
                repos.add(ccs.getRepoContentSourcePK().getRepo());
            }
        }

        return repos;
    }

    /**
     * Directly assign a repo to this content source.
     *
     * @param  repo
     *
     * @return the mapping that was added
     */
    public RepoContentSource addRepo(Repo repo) {
        if (this.repoContentSources == null) {
            this.repoContentSources = new HashSet<RepoContentSource>();
        }

        RepoContentSource mapping = new RepoContentSource(repo, this);
        this.repoContentSources.add(mapping);
        repo.addContentSource(this);
        return mapping;
    }

    /**
     * Removes the repo from this content source, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given repo did not exist as one that this content source is a member of, <code>null</code> is
     * returned.
     *
     * @param  repo the repo to remove from this content source
     *
     * @return the mapping that was removed or <code>null</code> if the repo was not mapped to this content source
     */
    public RepoContentSource removeRepo(Repo repo) {
        if ((this.repoContentSources == null) || (repo == null)) {
            return null;
        }

        RepoContentSource doomed = null;

        for (RepoContentSource ccs : this.repoContentSources) {
            if (repo.equals(ccs.getRepoContentSourcePK().getRepo())) {
                doomed = ccs;
                repo.removeContentSource(this);
                break;
            }
        }

        if (doomed != null) {
            this.repoContentSources.remove(doomed);
        }

        return doomed;
    }

    @PrePersist
    void onPersist() {
        this.lastModifiedDate = this.creationDate = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedDate = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "ContentSource: id=[" + this.id + "], name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((contentSourceType == null) ? 0 : contentSourceType.hashCode());
        result = (31 * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof ContentSource))) {
            return false;
        }

        final ContentSource other = (ContentSource) obj;

        if (contentSourceType == null) {
            if (other.contentSourceType != null) {
                return false;
            }
        } else if (!contentSourceType.equals(other.contentSourceType)) {
            return false;
        }

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
