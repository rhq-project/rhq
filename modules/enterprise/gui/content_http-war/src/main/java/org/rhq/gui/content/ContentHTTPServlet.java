package org.rhq.gui.content;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlets.DefaultServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ContentHTTPServlet extends DefaultServlet {
    private final Log log = LogFactory.getLog(ContentHTTPServlet.class);

    public ContentHTTPServlet() {
        super();
    }

    public void init() throws ServletException {
        log.info("** *** ** ** ** *** Inside of ContentHTTPServlet::init()");
        super.init();
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws IOException, ServletException {
        log.info("** ** ** Inside of ContentHTTPServlet::doGet()");
        super.doGet(request, response);
    }
}

