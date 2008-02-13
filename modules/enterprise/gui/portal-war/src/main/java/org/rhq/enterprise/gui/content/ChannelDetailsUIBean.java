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
package org.rhq.enterprise.gui.content;

import javax.faces.application.FacesMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ChannelDetailsUIBean {
    private Channel channel;

    public Channel getChannel() {
        loadChannel();
        return this.channel;
    }

    public String edit() {
        return "edit";
    }

    public String save() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

        manager.updateChannel(subject, channel);

        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The channel has been updated.");
        return "success";
    }

    public String cancel() {
        return "success";
    }

    private void loadChannel() {
        if (this.channel == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
            ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();
            this.channel = manager.getChannel(subject, id);
        }
    }
}