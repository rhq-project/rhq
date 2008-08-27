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
package org.rhq.core.gui.configuration;

import java.util.Iterator;

import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

/**
 * A JSF managed bean that is used for the action methods for all buttons rendered for {@link ConfigUIComponent}s that
 * take the user to a new page. Note, none of the action methods actually do anything - their only purpose is so they
 * can be used to define navigation rules that map the corresponding buttons to the appropriate pages.
 *
 * @author Ian Springer
 */
public class ConfigHelperUIBean {
    private static final String PROCEED_OUTCOME = "proceed";

    public String accessMap() {
        return PROCEED_OUTCOME;
    }

    public String addNewMap() {
        return PROCEED_OUTCOME;
    }

    public String addNewOpenMapMemberProperty() {
        return PROCEED_OUTCOME;
    }

    /**
     * Returns true if the given component has any messages (warning, error, success or failure messages), false
     * otherwise.
     *
     * @param clientId the client ID for the component being checked for messages
     *
     * @return true if there are messages, false otherwise
     */
     public boolean hasFacesMessage(String clientId) {
         Iterator<FacesMessage> messagesIterator = FacesContext.getCurrentInstance().getMessages(clientId);
         return messagesIterator.hasNext();
     }
}