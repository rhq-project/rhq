package org.rhq.sample.perspective;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;

import org.jboss.seam.annotations.web.RequestParameter;

/**
 * A base class for Seam components that utilize the RHQ remote API.
 *
 * @author Ian Springer
 */
public abstract class AbstractPerspectiveUIBean {
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 7080;
    private static final String DEFAULT_USERNAME = "rhqadmin";
    private static final String DEFAULT_PASSWORD = "rhqadmin";

    private final Log log = LogFactory.getLog(this.getClass());

    @RequestParameter("rhqServerHost")
    private String serverHost;

    @RequestParameter("rhqServerPort")
    private Integer serverPort;

    @RequestParameter("rhqUsername")
    private String username;

    @RequestParameter("rhqPassword")
    private String password;

    @RequestParameter("rhqSessionId")
    private Integer sessionId;

    private RemoteClient remoteClient;
    private Subject subject;

    protected RemoteClient getRemoteClient() throws Exception {
        if (this.remoteClient == null) {
            this.remoteClient = new RemoteClient(null, getServerHost(), getServerPort());
        }
        if (!this.remoteClient.isConnected()) {
            this.remoteClient.connect();
        }
        return this.remoteClient;
    }

    protected Subject getSubject() throws Exception {
        if (subject == null) {
            RemoteClient remoteClient = getRemoteClient();
            // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
            SubjectManagerRemote subjectManager = remoteClient.getSubjectManagerRemote();
            if (this.sessionId != null) {
                log.info("Retrieving subject for user [" + getUsername() + "] and sessionId [" + this.sessionId + "]...");
                this.subject = subjectManager.getSubjectByNameAndSessionId(getUsername(), this.sessionId);
            } else {
                log.info("Logging in as user [" + getUsername() + "] with password [" + getPassword() + "]...");
                this.subject = subjectManager.login(getUsername(), getPassword());
            }
        }
        return this.subject;
    }

    private String getServerHost() {
        return (this.serverHost != null) ? this.serverHost : DEFAULT_SERVER_HOST;
    }

    private int getServerPort() {
        return (this.serverPort != null) ? this.serverPort : DEFAULT_SERVER_PORT;
    }

    private String getUsername() {
        return (this.username != null) ? this.username : DEFAULT_USERNAME;
    }

    private String getPassword() {
        return (this.password != null) ? this.password : DEFAULT_PASSWORD;
    }
}