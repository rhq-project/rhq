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
package org.rhq.enterprise.gui.inventory.resource;

import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class PostbackPhaseListener implements PhaseListener {

    public static final String POSTBACK_ATTRIBUTE_NAME = PostbackPhaseListener.class.getName();

    public void afterPhase(PhaseEvent event) {
    }

    public void beforePhase(PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        Map requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(POSTBACK_ATTRIBUTE_NAME, Boolean.TRUE);
    }

    public PhaseId getPhaseId() {
        return PhaseId.APPLY_REQUEST_VALUES;
    }

    public static boolean isPostback() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null) {
                return Boolean.TRUE.equals(
                        externalContext.getRequestMap().get(POSTBACK_ATTRIBUTE_NAME));
            }
        }

        return false;
    }

}