package org.rhq.server.rhaccess;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConfigServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Config conf = new Config();
        resp.getOutputStream().println("{\"userAgent\":\"" + conf.getUserAgent() + "\"}");
        resp.setContentType("application/json");
    }

}
