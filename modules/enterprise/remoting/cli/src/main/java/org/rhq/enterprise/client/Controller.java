package org.rhq.enterprise.client;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.commands.LoginCommand;
import org.rhq.enterprise.client.commands.LogoutCommand;
import org.rhq.enterprise.server.exception.LoginException;

public class Controller {

    private ClientMain client;

    public Controller(ClientMain client) {
        this.client = client;
    }

    public Subject login(String username, String password) throws Exception {
        LoginCommand cmd = (LoginCommand) client.getCommands().get("login");
        return cmd.execute(client, username, password);
    }

    public Subject login(String username, String password, String host, int port) throws LoginException {
        LoginCommand cmd = (LoginCommand) client.getCommands().get("login");
        return cmd.execute(client, username, password, host, port);
    }

    public void logout() {
        LogoutCommand cmd = (LogoutCommand) client.getCommands().get("logout");
        cmd.execute(client);
    }

}
