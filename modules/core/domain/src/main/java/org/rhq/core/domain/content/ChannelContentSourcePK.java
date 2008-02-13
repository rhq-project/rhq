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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * This is the composite primary key for the {@link ChannelContentSource} entity. That entity is an explicit
 * many-to-many mapping table, so this composite key is simply the foreign keys to both ends of that relationship.
 *
 * @author John Mazzitelli
 */
public class ChannelContentSourcePK implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings here,
     * even though this class is an @IdClass and it should not need the mappings here.  The mappings belong in the
     * entity itself.
     */

    @JoinColumn(name = "CHANNEL_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Channel channel;

    @JoinColumn(name = "CONTENT_SRC_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ContentSource contentSource;

    public ChannelContentSourcePK() {
    }

    public ChannelContentSourcePK(Channel channel, ContentSource contentSource) {
        this.channel = channel;
        this.contentSource = contentSource;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ContentSource getContentSource() {
        return contentSource;
    }

    public void setContentSource(ContentSource contentSource) {
        this.contentSource = contentSource;
    }

    @Override
    public String toString() {
        return "ChannelContentSourcePK: channel=[" + channel + "]; contentSource=[" + contentSource + "]";
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

        if ((obj == null) || (!(obj instanceof ChannelContentSourcePK))) {
            return false;
        }

        final ChannelContentSourcePK other = (ChannelContentSourcePK) obj;

        if (channel == null) {
            if (other.channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }

        if (contentSource == null) {
            if (other.contentSource != null) {
                return false;
            }
        } else if (!contentSource.equals(other.contentSource)) {
            return false;
        }

        return true;
    }
}