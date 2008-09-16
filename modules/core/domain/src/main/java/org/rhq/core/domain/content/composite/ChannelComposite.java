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
package org.rhq.core.domain.content.composite;

import java.io.Serializable;

import org.rhq.core.domain.content.Channel;

public class ChannelComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Channel channel;
    private long packageVersionCount;

    public ChannelComposite(Channel channel, long packageVersionCount) {
        super();
        this.channel = channel;
        this.packageVersionCount = packageVersionCount;
    }

    public Channel getChannel() {
        return channel;
    }

    public long getPackageVersionCount() {
        return packageVersionCount;
    }
}
