package org.rhq.enterprise.client;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.exception.LoginException;

import java.util.Map;

public class Controller {

    private RHQServer rhqServer = new RHQServer();

    private ClientMain client;

    private RemoteClient remoteClient;

    private boolean loggedIn;

    public Controller(ClientMain client) {
        this.client = client;
    }

    public RHQServer getServer() {
        return rhqServer;
    }

    public void setServer(RHQServer rhqServer) {
        this.rhqServer = rhqServer;
    }

    public Map<String, Object> getManagers() {
        if (loggedIn) {
            return remoteClient.getManagers();
        }
        return null;
    }

    public Subject getSubject() {
        if (loggedIn) {
            return client.getRemoteClient().getSubject();
        }
        return null;
    }

    public Subject login(String username, String password) throws LoginException {
        remoteClient = new RemoteClient(rhqServer.getHost(), rhqServer.getPort());

        client.setHost(rhqServer.getHost());
        client.setPort(rhqServer.getPort());
        client.setUser(username);
        client.setPass(password);
        client.setRemoteClient(remoteClient);

        Subject subject = remoteClient.getSubjectManagerRemote().login(username, password);
        remoteClient.setSubject(subject);
        remoteClient.setLoggedIn(true);
        loggedIn = true;
        client.setSubject(subject);

        return subject;
    }

    public void logout() {
        loggedIn = false;
        client.setHost(null);
        client.setPort(0);
        client.getRemoteClient().setLoggedIn(false);
        client.setRemoteClient(null);
        client.setUser(null);
        client.setPass(null);
    }

}
