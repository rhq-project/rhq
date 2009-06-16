package org.rhq.enterprise.client.commands;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RHQRemoteClient;
//import org.rhq.enterprise.server.ws.Subject;
//import org.rhq.enterprise.server.ws.SubjectManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;

public class CreateUserCommand implements ClientCommand {

    public boolean execute(ClientMain client, String[] args) {
        try {
            // check to see if user logged in
            if (client.getSubject() == null) {
                client.getPrintWriter().println("Unable to create accounts until successfully logged in");
                return true;
            }

            // Retrieve the parameters passed in
            // String example="Ex. createAccount user1 pass1 TestFName TestLName active test@none.org";
            // String syntax = "createAccount (*)user (*)pass (*)[FirstName] (*)[LastName] (*)active|inactive";
            // syntax+=" [emailAddress]";
            String user = args[1] + "";
            String pass = args[2] + "";
            String fName = args[3] + "";
            String lName = args[4] + "";
            String active = args[5] + "";
            String email = args[6] + "";

            // retrieve RHQRegistry object
            RHQRemoteClient registry = client.getRemoteClient();
            Subject loggedInUser = client.getSubject();
            // Now make the connection successfully and store values
            // instantiate SLSB
            SubjectManagerRemote subjectManager = registry.getSubjectManagerRemote();
            // DURING POPULATION we should catch some type errors or insert more rigorous checking here
            // Populate Subject then Create account
            Subject account = new Subject();
            account.setName(user);
            account.setFirstName(fName);
            account.setLastName(lName);
            account.setFactive(active.equalsIgnoreCase("active"));
            account.setEmailAddress(email);
            subjectManager.createSubject(loggedInUser, account);
            // TODO: something wierd here ... :-/
            // //Now update the account with the password.
            // subjectManager.changePassword(loggedInUser, user, pass);

            client.getPrintWriter().println("Account creation successful");
        } catch (Exception e) {
            client.getPrintWriter().println("Account creation failed: " + e.getMessage());
        }

        return true;
    }

    public String getDetailedHelp() {
        return "";
    }

    public String getHelp() {
        return "Creates a new account with supplied account details";
    }

    public String getPromptCommandString() {
        return "createAccount";
    }

    public String getSyntax() {
        String example = "Ex. createAccount user1 pass1 TestFName TestLName active test@none.org";
        String syntax = "createAccount (*)user (*)pass  (*)[FirstName] (*)[LastName] (*)active|inactive";
        syntax += " [emailAddress]";

        return example + "\n" + syntax;
    }

}
