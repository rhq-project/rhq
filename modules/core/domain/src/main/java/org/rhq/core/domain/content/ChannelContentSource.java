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

/**
 * This is the many-to-many entity that correlates a channel with a content source that will fill the channel with
 * package versions. It is an explicit relationship mapping entity between {@link Channel} and {@link ContentSource}.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(ChannelContentSourcePK.class)
@NamedQueries( {
    @NamedQuery(name = ChannelContentSource.DELETE_BY_CONTENT_SOURCE_ID, query = "DELETE ChannelContentSource ccs WHERE ccs.contentSource.id = :contentSourceId"),
    @NamedQuery(name = ChannelContentSource.DELETE_BY_CHANNEL_ID, query = "DELETE ChannelContentSource ccs WHERE ccs.channel.id = :channelId") })
@Table(name = "RHQ_CHANNEL_CONTENT_SRC_MAP")
public class ChannelContentSource implements Serializable {
    public static final String DELETE_BY_CONTENT_SOURCE_ID = "ChannelContentSource.deleteByContentSourceId";
    public static final String DELETE_BY_CHANNEL_ID = "ChannelContentSource.deleteByChannelId";

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "CHANNEL_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Channel channel;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "CONTENT_SRC_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private ContentSource contentSource;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    protected ChannelContentSource() {
    }

    public ChannelContentSource(Channel channel, ContentSource contentSource) {
        this.channel = channel;
        this.contentSource = contentSource;
    }

    public ChannelContentSourcePK getChannelContentSourcePK() {
        return new ChannelContentSourcePK(channel, contentSource);
    }

    public void setChannelContentSourcePK(ChannelContentSourcePK pk) {
        this.channel = pk.getChannel();
        this.contentSource = pk.getContentSource();
    }

    /**
     * This is the epoch time when this mapping was first created; in other words, when the channel was first associated
     * with the content source.
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
        StringBuilder str = new StringBuilder("ChannelContentSource: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", ch=[").append(this.channel).append("]");
        str.append(", cs=[").append(this.contentSource).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((channel == null) ? 0 : channel.hashCode());
        result = (31 * result) + ((contentSource == null) ? 0 : contentSource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof ChannelContentSource))) {
            return false;
        }

        final ChannelContentSource other = (ChannelContentSource) obj;

        if (channel == null) {
            if (channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }

        if (contentSource == null) {
            if (contentSource != null) {
                return false;
            }
        } else if (!contentSource.equals(other.contentSource)) {
            return false;
        }

        return true;
    }
}