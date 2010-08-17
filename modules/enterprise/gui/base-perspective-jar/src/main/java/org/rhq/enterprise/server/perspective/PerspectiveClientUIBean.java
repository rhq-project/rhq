package org.rhq.enterprise.server.perspective;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Conversation;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;

/**
 * @author Ian Springer
 */
@Name(PerspectiveClientUIBean.NAME)
@Scope(ScopeType.CONVERSATION)
@AutoCreate
public class PerspectiveClientUIBean {
    public static final String NAME = "PerspectiveClientUIBean";

    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 7080;
    private static final String DEFAULT_USERNAME = "rhqadmin";
    private static final String DEFAULT_PASSWORD = "rhqadmin";

    private final Log log = LogFactory.getLog(this.getClass());

    @RequestParameter
    private String rhqServerHost;
    private String serverHost;

    @RequestParameter
    private Integer rhqServerPort;
    private Integer serverPort;

    @RequestParameter
    private String rhqUsername;
    private String username;

    @RequestParameter
    private String rhqPassword;
    private String password;

    @RequestParameter
    private Integer rhqSessionId;

    private RemoteClient remoteClient;
    private Subject subject;
    private String coreGuiBaseUrl;

    @NotNull
    public RemoteClient getRemoteClient() throws Exception {
        if (this.remoteClient == null) {
            this.remoteClient = new RemoteClient(null, getServerHost(), getServerPort());
        }
        if (!this.remoteClient.isConnected()) {
            try {
                this.remoteClient.connect();
            } catch (Exception e) {
                this.remoteClient = null;
                log.info("Ending perspective client Conversation with id " + Conversation.instance().getId() + "...");
                Conversation.instance().end();
                throw e;
            }
            Conversation.instance().begin();
            log.info("Began perspective client Conversation with id " + Conversation.instance().getId() + ".");
        }
        return this.remoteClient;
    }

    /**
     * Returns the currently logged in RHQ user.
     *
     * @return the currently logged in RHQ user
     *
     * @throws Exception if we are unable to obtain the Subject - hopefully this will encourage callers to set a
     *         FacesMessage and return null to give the user a friendlier error
     */
    @NotNull
    public Subject getSubject() throws Exception {
        if (subject == null) {
            RemoteClient remoteClient = getRemoteClient();
            // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
            SubjectManagerRemote subjectManager = remoteClient.getSubjectManagerRemote();
            if (this.rhqSessionId != null) {
                log.info("Retrieving subject for user [" + getUsername() + "] and sessionId [" + this.rhqSessionId
                    + "]...");
                this.subject = subjectManager.getSubjectByNameAndSessionId(getUsername(), this.rhqSessionId);
            } else {
                log.info("Logging in as user [" + getUsername() + "] with password [" + getPassword() + "]...");
                this.subject = subjectManager.login(getUsername(), getPassword());
            }
        }
        return this.subject;
    }

    // NOT Currently In Use as we've pulled out perspectives for the 3.0.0 release. Commenting out due to
    // the use of the currently removed PerspectiveManagerRemote
    /*
    @NotNull
    public String getCoreGuiBaseUrl() throws Exception {
        if (this.coreGuiBaseUrl == null) {
            RemoteClient remoteClient = getRemoteClient();
            Subject subject = getSubject();
            PerspectiveManagerRemote perspectiveManager = remoteClient.getPerspectiveManagerRemote();
            this.coreGuiBaseUrl = perspectiveManager.getRootUrl(subject, true, false);
        }
        return this.coreGuiBaseUrl;
    }
    */

    @NotNull
    private String getServerHost() {
        if (this.serverHost == null) {
            if (this.rhqServerHost != null) {
                this.serverHost = this.rhqServerHost;
            } else {
                return DEFAULT_SERVER_HOST;
            }
        } else {
            if (this.rhqServerHost != null && this.serverHost.equals(this.rhqServerHost)) {

            }
        }
        return this.serverHost;
    }

    private int getServerPort() {
        if (this.serverPort == null) {
            if (this.rhqServerPort != null) {
                this.serverPort = this.rhqServerPort;
            } else {
                return DEFAULT_SERVER_PORT;
            }
        }
        return this.serverPort;
    }

    @NotNull
    private String getUsername() {
        if (this.username == null) {
            if (this.rhqUsername != null) {
                this.username = this.rhqUsername;
            } else {
                return DEFAULT_USERNAME;
            }
        }
        return this.username;
    }

    @NotNull
    private String getPassword() {
        if (this.password == null) {
            if (this.rhqPassword != null) {
                this.password = this.rhqPassword;
            } else {
                return DEFAULT_PASSWORD;
            }
        }
        return this.password;
    }
}
