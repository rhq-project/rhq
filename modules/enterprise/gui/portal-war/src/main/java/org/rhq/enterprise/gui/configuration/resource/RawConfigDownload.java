package org.rhq.enterprise.gui.configuration.resource;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

@Name("rawConfigDownload")
@Scope(ScopeType.PAGE)
public class RawConfigDownload {

    @Logger
    private Log log;

    @In(value = "#{rawConfigCollection}")
    RawConfigCollection rawConfigCollection;

    @In(value = "#{facesContext}")
    FacesContext facesContext;

    @In(value = "#{facesContext.externalContext}")
    private ExternalContext extCtx;

    @Create
    public void init() {
        log.error("starting");
    }

    public String download() {
        HttpServletResponse response = (HttpServletResponse) extCtx.getResponse();
        response.setContentType("text/plain");
        response.addHeader("Content-disposition", "attachment; filename=\"testing.txt\"");
        try {
            ServletOutputStream os = response.getOutputStream();
            os.write(rawConfigCollection.getCurrent().getContents());
            os.flush();
            os.close();
            facesContext.responseComplete();
        } catch (Exception e) {
            log.error("\nFailure : " + e.toString() + "\n");
        }

        return null;
    }
}