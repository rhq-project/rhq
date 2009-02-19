package org.rhq.enterprise.gui.legacy;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class WebUserTrackingFilter extends BaseFilter {

    private static Log log = LogFactory.getLog(WebUserTrackingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        // only record GET requests, resubmitting to POST pages is dangerous
        String method = request.getMethod();
        if (method.equals("GET")) {
            // do not create a session if one does not exist, that is the job of the AuthenticationFilter
            HttpSession session = request.getSession(false);
            WebUser webUser = SessionUtils.getWebUser(session); // will handle null session argument
            if (webUser == null) {
                chain.doFilter(req, res);
                return;
            }

            String lastURL = getRequestURL(request);
            WebUserPreferences preferences = webUser.getWebPreferences();

            preferences.addLastVisitedURL(lastURL);
            if (log.isDebugEnabled()) {
                log.debug("User [" + webUser.getSubject().getName() + "] visited [" + lastURL + "]");
            }
        }

        try {
            chain.doFilter(req, res);
        } catch (IOException e) {
            log.warn("Caught IO Exception from client " + request.getRemoteAddr() + ": " + e.getMessage());
        }
    }

    public String getRequestURL(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null) {
            url.append('?');
            url.append(request.getQueryString());
        }
        return url.toString();
    }

}
