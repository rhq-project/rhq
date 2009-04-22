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
package org.rhq.enterprise.gui.common.error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author Joseph Marques
 */
public class GenericErrorUIBean {

    String summary;
    String details;
    List<String> trace;

    public GenericErrorUIBean() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> sessionMap = externalContext.getSessionMap();
        trace = new ArrayList<String>();
        Throwable ex = (Exception) sessionMap.remove("GLOBAL_RENDER_ERROR");
        trace = Arrays.asList(ThrowableUtil.getAllMessagesArray(ex));
        while (ex.getCause() != null) {
            ex = ex.getCause();
        }
        summary = ex.getClass().getSimpleName();
        details = ex.getMessage();
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }

    public List<String> getTrace() {
        return trace;
    }
}
