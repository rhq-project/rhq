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

package org.rhq.enterprise.gui.legacy.action.resource.common.events;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Acknowledge {@link Event}s
 * @author Heiko W. Rupp
 *
 */
public class AckEventsAction extends DispatchAction {

    public ActionForward ackEvent(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        int eventId = WebUtility.getRequiredIntRequestParameter(request, "eventId");
        EventManagerLocal eventManager = LookupUtil.getEventManager();
        Subject subject = WebUtility.getSubject(request);

        //EventComposite comp = eventManager.ackEvent(subject, eventId);

        StringBuffer buf = new StringBuffer();
        buf.append("<div>");
        // buf.append(comp.getAckUser() + "/ " + comp.getAckTime());
        buf.append("</div>");

        Writer w = response.getWriter();
        w.append(buf.toString());
        w.flush();

        return null;
    }
}
