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
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.rhq.core.domain.resource.Resource;

/**
 * A channel represents a set of related {@link PackageVersion}s. The packages in this channel are populated by its
 * {@link ContentSource}s. The relationship with content sources is weak; that is, content sources can come and go, even
 * as the packages contained in the channel remain.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Channel.QUERY_FIND_ALL, query = "SELECT c FROM Channel c"),
    @NamedQuery(name = Channel.QUERY_FIND_BY_IDS, query = "SELECT c FROM Channel c WHERE c.id IN ( :ids )"),
    @NamedQuery(name = Channel.QUERY_FIND_BY_CONTENT_SOURCE_ID_FETCH_CCS, query = "SELECT c FROM Channel c LEFT JOIN FETCH c.channelContentSources ccs WHERE ccs.contentSource.id = :id"),
    @NamedQuery(name = Channel.QUERY_FIND_BY_CONTENT_SOURCE_ID, query = "SELECT c FROM Channel c LEFT JOIN c.channelContentSources ccs WHERE ccs.contentSource.id = :id"),
    @NamedQuery(name = Channel.QUERY_FIND_SUBSCRIBER_RESOURCES, query = "SELECT rc.resource FROM ResourceChannel rc WHERE rc.channel.id = :id"),
    @NamedQuery(name = Channel.QUERY_FIND_CHANNELS_BY_RESOURCE_ID, query = "SELECT rc.channel FROM ResourceChannel rc WHERE rc.resource.id = :resourceId"),
    @NamedQuery(name = Channel.QUERY_FIND_AVAILABLE_CHANNELS_BY_RESOURCE_ID, query = "SELECT DISTINCT c FROM Channel as c WHERE c.id NOT IN ( SELECT rc.channel.id FROM ResourceChannel rc WHERE rc.resource.id = :resourceId )") }) 
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CHANNEL_ID_SEQ")
@Table(name = "RHQ_CHANNEL")
public class Channel implements Serializable {
    // Constants  --------------------------------------------

    public static final String QUERY_FIND_ALL = "Channel.findAll";
    public static final String QUERY_FIND_BY_IDS = "Channel.findByIds";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID_FETCH_CCS = "Channel.findByContentSourceIdFetchCCS";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID = "Channel.findByContentSourceId";
    public static final String QUERY_FIND_SUBSCRIBER_RESOURCES = "Channel.findSubscriberResources";
    public static final String QUERY_FIND_CHANNELS_BY_RESOURCE_ID = "Channel.findChannelsByResourceId";
    public static final String QUERY_FIND_AVAILABLE_CHANNELS_BY_RESOURCE_ID = "Channel.findAvailableChannelsByResourceId";

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "CREATION_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "LAST_MODIFIED_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;

    @OneToMany(mappedBy = "channel", fetch = FetchType.LAZY)
    private Set<ResourceChannel> resourceChannels;

    @OneToMany(mappedBy = "channel", fetch = FetchType.LAZY)
    private Set<ChannelContentSource> channelContentSources;

    @OneToMany(mappedBy = "channel", fetch = FetchType.LAZY)
    private Set<ChannelPackageVersion> channelPackageVersions;

    // Constructor ----------------------------------------

    public Channel() {
        // for JPA use
    }

    public Channel(String name) {
        this.name = name;
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Programmatic name of the channel.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * User specified description of the channel.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Timestamp of when this channel was created.
     */
    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Timestamp of the last time the {@link #getContentSources() sources} of this channel was changed. It is not
     * necessarily the last time any other part of this channel object was changed (for example, this last modified date
     * does not necessarily correspond to the time when the description was modified).
     */
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getResources()
     */
    public Set<ResourceChannel> getResourceChannels() {
        return resourceChannels;
    }

    /**
     * The resources subscribed to this channel.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated resources, use
     * {@link #getResourceChannels()} or {@link #addResource(Resource)}, {@link #removeResource(Resource)}.</p>
     */
    public Set<Resource> getResources() {
        HashSet<Resource> resources = new HashSet<Resource>();

        if (resourceChannels != null) {
            for (ResourceChannel rc : resourceChannels) {
                resources.add(rc.getResourceChannelPK().getResource());
            }
        }

        return resources;
    }

    /**
     * Directly subscribe a resource to this channel.
     *
     * @param  resource
     *
     * @return the mapping that was added
     */
    public ResourceChannel addResource(Resource resource) {
        if (this.resourceChannels == null) {
            this.resourceChannels = new HashSet<ResourceChannel>();
        }

        ResourceChannel mapping = new ResourceChannel(resource, this);
        this.resourceChannels.add(mapping);
        return mapping;
    }

    /**
     * Unsubscribes the resource from this channel, if it exists. If it was already subscribed, the mapping that was
     * removed is returned; if not, <code>null</code> is returned.
     *
     * @param  resource the resource to unsubscribe from this channel
     *
     * @return the mapping that was removed or <code>null</code> if the resource was not subscribed to this channel
     */
    public ResourceChannel removeResource(Resource resource) {
        if ((this.resourceChannels == null) || (resource == null)) {
            return null;
        }

        ResourceChannel doomed = null;

        for (ResourceChannel rc : this.resourceChannels) {
            if (resource.equals(rc.getResourceChannelPK().getResource())) {
                doomed = rc;
                break;
            }
        }

        if (doomed != null) {
            this.resourceChannels.remove(doomed);
        }

        return doomed;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getContentSources()
     */
    public Set<ChannelContentSource> getChannelContentSources() {
        return channelContentSources;
    }

    /**
     * The content sources that this channel serves up. These are the content sources that provide or provided packages
     * for this channel. This relationship is weak; a content source may not be in this set but the packages it loaded
     * into this channel may still exist.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated content sources,
     * use {@link #getChannelContentSources()} or {@link #addContentSource(ContentSource)},
     * {@link #removeContentSource(ContentSource)}.</p>
     */
    public Set<ContentSource> getContentSources() {
        HashSet<ContentSource> contentSources = new HashSet<ContentSource>();

        if (channelContentSources != null) {
            for (ChannelContentSource ccs : channelContentSources) {
                contentSources.add(ccs.getChannelContentSourcePK().getContentSource());
            }
        }

        return contentSources;
    }

    /**
     * Directly assign a content source to this channel.
     *
     * @param  contentSource
     *
     * @return the mapping that was added
     */
    public ChannelContentSource addContentSource(ContentSource contentSource) {
        if (this.channelContentSources == null) {
            this.channelContentSources = new HashSet<ChannelContentSource>();
        }

        ChannelContentSource mapping = new ChannelContentSource(this, contentSource);
        this.channelContentSources.add(mapping);
        return mapping;
    }

    /**
     * Removes the content source from this channel, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given content source did not exist as one that is a member of this channel, <code>null</code> is
     * returned.
     *
     * @param  contentSource the content source to remove from this channel
     *
     * @return the mapping that was removed or <code>null</code> if the content source was not mapped to this channel
     */
    public ChannelContentSource removeContentSource(ContentSource contentSource) {
        if ((this.channelContentSources == null) || (contentSource == null)) {
            return null;
        }

        ChannelContentSource doomed = null;

        for (ChannelContentSource ccs : this.channelContentSources) {
            if (contentSource.equals(ccs.getChannelContentSourcePK().getContentSource())) {
                doomed = ccs;
                break;
            }
        }

        if (doomed != null) {
            this.channelContentSources.remove(doomed);
        }

        return doomed;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getPackageVersions()
     */
    public Set<ChannelPackageVersion> getChannelPackageVersions() {
        return channelPackageVersions;
    }

    /**
     * The package versions that this channel serves up. Subscribers to this channel will have access to the returned
     * set of package versions. These are package versions that were directly assigned to the channel and those that
     * were assigned via its relationship with its content sources. This is the relationship that should be consulted
     * when determining what package versions this channel exposes - do not look at the indirect relationship from
     * content sources to package versions. When content sources are assigned to this channel, this package version
     * relationship will be automatically managed.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated package versions,
     * use {@link #getChannelPackageVersions()} or {@link #addPackageVersion(PackageVersion)},
     * {@link #removePackageVersion(PackageVersion)}.</p>
     */
    public Set<PackageVersion> getPackageVersions() {
        HashSet<PackageVersion> packageVersions = new HashSet<PackageVersion>();

        if (channelPackageVersions != null) {
            for (ChannelPackageVersion cpv : channelPackageVersions) {
                packageVersions.add(cpv.getChannelPackageVersionPK().getPackageVersion());
            }
        }

        return packageVersions;
    }

    /**
     * Directly assign a package version to this channel.
     *
     * @param  packageVersion
     *
     * @return the mapping that was added
     */
    public ChannelPackageVersion addPackageVersion(PackageVersion packageVersion) {
        if (this.channelPackageVersions == null) {
            this.channelPackageVersions = new HashSet<ChannelPackageVersion>();
        }

        ChannelPackageVersion mapping = new ChannelPackageVersion(this, packageVersion);
        this.channelPackageVersions.add(mapping);
        return mapping;
    }

    /**
     * Removes the package version from this channel, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given package version did not exist as one that is a member of this channel, <code>null</code>
     * is returned.
     *
     * @param  packageVersion the package version to remove from this channel
     *
     * @return the mapping that was removed or <code>null</code> if the package version was not mapped to this channel
     */
    public ChannelPackageVersion removePackageVersion(PackageVersion packageVersion) {
        if ((this.channelPackageVersions == null) || (packageVersion == null)) {
            return null;
        }

        ChannelPackageVersion doomed = null;

        for (ChannelPackageVersion cpv : this.channelPackageVersions) {
            if (packageVersion.equals(cpv.getChannelPackageVersionPK().getPackageVersion())) {
                doomed = cpv;
                break;
            }
        }

        if (doomed != null) {
            this.channelPackageVersions.remove(doomed);
        }

        return doomed;
    }

    @Override
    public String toString() {
        return "Channel: id=[" + this.id + "], name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof Channel))) {
            return false;
        }

        final Channel other = (Channel) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }

    @PrePersist
    void onPersist() {
        if (this.creationDate == null) {
            this.creationDate = new Date();
        }

        if (this.lastModifiedDate == null) {
            this.lastModifiedDate = this.creationDate;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedDate = new Date();
    }
}