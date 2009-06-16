package org.rhq.enterprise.client.commands;

import java.util.ArrayList;
import java.util.List;

//import org.rhq.client.CliEngine;
//import org.rhq.client.RHQRemoteRegistry;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RHQRemoteClient;
//import org.rhq.enterprise.server.ws.Subject;
//import org.rhq.enterprise.server.ws.SubjectManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;

public class DeleteUserCommand implements ClientCommand {

    public boolean execute(ClientMain client, String[] args) {
        try {
            // check to see if user logged in
            if (client.getSubject() == null) {
                client.getPrintWriter().println("Unable to delete accounts until successfully logged in");
                return true;
            }

            // Retrieve the parameters passed in
            // String example="Ex. deleteAccount 09875";
            // String syntax = "deleteAccount (*)user";
            String userIdValue = args[1] + "";
            // do type checking
            int userId = Integer.valueOf(userIdValue);

            // retrieve RHQRegistry object
            RHQRemoteClient registry = client.getRemoteClient();
            Subject loggedInUser = client.getSubject();
            // Now make the connection successfully and store values
            // instantiate SLSB
            SubjectManagerRemote subjectManager = registry.getSubjectManagerRemote();
            List<Integer> userToDelete = new ArrayList<Integer>();
            userToDelete.add(userId);
            Integer[] usersToDelete = new Integer[userId];
//            subjectManager.deleteUsers(loggedInUser, userToDelete);
            subjectManager.deleteUsers(loggedInUser, usersToDelete);

            client.getPrintWriter().println("Account deletion successful");
        } catch (Exception e) {
            client.getPrintWriter().println("Account deletion failed: " + e.getMessage());
        }

        return true;
    }

    public String getDetailedHelp() {
        return "";
    }

    public String getHelp() {
        return "Deletes existing account provided logged in user has perissions";
    }

    public String getPromptCommandString() {
        return "deleteAccount";
    }

    public String getSyntax() {
        String example = "Ex. deleteAccount 198564";
        String syntax = "deleteAccount (*)user.id";

        return example + "\n" + syntax;
    }

}
