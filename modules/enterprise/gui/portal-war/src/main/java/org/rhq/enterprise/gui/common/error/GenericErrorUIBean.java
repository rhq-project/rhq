package org.rhq.enterprise.gui.common.error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.rhq.core.util.exception.ThrowableUtil;

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
