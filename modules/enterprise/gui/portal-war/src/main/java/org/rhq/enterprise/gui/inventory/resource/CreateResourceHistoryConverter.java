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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * JSF converter used to handle <code>CreateResourceHistory</code> objects.
 *
 * @author Jason Dobies
 */
public class CreateResourceHistoryConverter implements Converter {
    // Converter Implementation  --------------------------------------------

    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String string) {
        int historyId = Integer.parseInt(string);

        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        CreateResourceHistory historyItem = resourceFactoryManager.getCreateHistoryItem(historyId);

        return historyItem;
    }

    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object object) {
        CreateResourceHistory history = (CreateResourceHistory) object;
        return Integer.toString(history.getId());
    }
}