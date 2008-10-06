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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.rhq.core.domain.resource.Resource;

/**
 * This is the composite primary key for the {@link ResourceChannel} entity. That entity is an explicit many-to-many
 * mapping table, so this composite key is simply the foreign keys to both ends of that relationship.
 *
 * @author John Mazzitelli
 */
public class ResourceChannelPK implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings here,
     * even though this class is an @IdClass and it should not need the mappings here.  The mappings belong in the
     * entity itself.
     */
    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    @JoinColumn(name = "CHANNEL_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Channel channel;

    public ResourceChannelPK() {
    }

    public ResourceChannelPK(Resource resource, Channel channel) {
        this.resource = resource;
        this.channel = channel;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "ResourceChannelPK: resource=[" + resource + "]; channel=[" + channel + "]";
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

        if ((obj == null) || (!(obj instanceof ResourceChannelPK))) {
            return false;
        }

        final ResourceChannelPK other = (ResourceChannelPK) obj;

        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        if (channel == null) {
            if (other.channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }

        return true;
    }
}