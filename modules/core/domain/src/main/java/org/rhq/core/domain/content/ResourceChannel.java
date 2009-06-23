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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Resource;

/**
 * This is the many-to-many entity that correlates a channel with one of its subscribers. It is an explicit relationship
 * mapping entity between {@link Channel} and {@link Resource}.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(ResourceChannelPK.class)
@NamedQueries( {
    @NamedQuery(name = ResourceChannel.DELETE_BY_RESOURCES, query = "DELETE ResourceChannel rc WHERE rc.resource.id IN ( :resourceIds )"),
    @NamedQuery(name = ResourceChannel.DELETE_BY_RESOURCE_ID, query = "DELETE ResourceChannel rc WHERE rc.resource.id = :resourceId"),
    @NamedQuery(name = ResourceChannel.DELETE_BY_CHANNEL_ID, query = "DELETE ResourceChannel rc WHERE rc.channel.id = :channelId") })
@Table(name = "RHQ_CHANNEL_RESOURCE_MAP")
public class ResourceChannel implements Serializable {
    public static final String DELETE_BY_RESOURCES = "ResourceChannel.deleteByResources";
    public static final String DELETE_BY_RESOURCE_ID = "ResourceChannel.deleteByResourceId";
    public static final String DELETE_BY_CHANNEL_ID = "ResourceChannel.deleteByChannelId";

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Resource resource;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "CHANNEL_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Channel channel;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    protected ResourceChannel() {
    }

    public ResourceChannel(Resource resource, Channel channel) {
        this.resource = resource;
        this.channel = channel;
    }

    public ResourceChannelPK getResourceChannelPK() {
        return new ResourceChannelPK(resource, channel);
    }

    public void setResourceChannelPK(ResourceChannelPK pk) {
        this.resource = pk.getResource();
        this.channel = pk.getChannel();
    }

    /**
     * This is the epoch time when this mapping was first created; in other words, when the resource was subscribed to
     * the channel.
     *
     * @return the time the resource was subscribed to the channel
     */
    public long getCreatedTime() {
        return createdTime;
    }

    @PrePersist
    void onPersist() {
        this.createdTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceChannel: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", re=[").append(this.resource).append("]");
        str.append(", ch=[").append(this.channel).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((resource == null) ? 0 : resource.hashCode());
        result = (31 * result) + ((channel == null) ? 0 : channel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof ResourceChannel))) {
            return false;
        }

        final ResourceChannel other = (ResourceChannel) obj;

        if (resource == null) {
            if (resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        if (channel == null) {
            if (channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }

        return true;
    }
}