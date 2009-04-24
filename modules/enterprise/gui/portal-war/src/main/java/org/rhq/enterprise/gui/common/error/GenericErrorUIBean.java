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
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;

/**
 * @author Joseph Marques
 */
public class GenericErrorUIBean {

    private final Log log = LogFactory.getLog(GenericErrorUIBean.class);

    String summary;
    String details;
    List<Tuple<String, String>> trace;

    public GenericErrorUIBean() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> sessionMap = externalContext.getSessionMap();

        trace = new ArrayList<Tuple<String, String>>();
        Throwable ex = (Exception) sessionMap.remove("GLOBAL_RENDER_ERROR");

        // let's put this in the server log along with showing the user
        log.error("Error processing user request", ex);

        String message = ex.getLocalizedMessage();
        String stack = StringUtil.getFirstStackTrace(ex);
        trace.add(new Tuple<String, String>(message, stack));

        while (ex.getCause() != null) {
            ex = ex.getCause();

            message = ex.getLocalizedMessage();
            stack = StringUtil.getFirstStackTrace(ex);
            trace.add(new Tuple<String, String>(message, stack));
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

    public List<Tuple<String, String>> getTrace() {
        return trace;
    }
}
