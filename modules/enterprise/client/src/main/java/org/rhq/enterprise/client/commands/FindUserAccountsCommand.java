package org.rhq.enterprise.client.commands;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RHQRemoteClient;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.SubjectManagerRemote;

public class FindUserAccountsCommand implements ClientCommand {

  public boolean execute(ClientMain client, String[] args) {
      try {
    	  //check to see if user logged in
    	  if(client.getSubject()==null){
    		  client.getPrintWriter().println("Unable to view accounts until successfully logged in");
    		  return true;
    	  }

    	  //Retrieve the parameters passed in
//    	  String example="Ex. findAccounts user1";
//    	  String syntax = "findAccounts (*)user ";
          String user = args[1]+"";

          RHQRemoteClient registry = client.getRemoteClient();
          Subject loggedInUser = client.getSubject();
          //Now make the connection successfully and store values
          //instantiate SLSB
          SubjectManagerRemote subjectManager = registry.getSubjectManagerRemote();
        //DURING POPULATION we should catch some type errors or insert more rigorous checking here
          Subject located = subjectManager.findSubjectByName(loggedInUser, user);

          client.getPrintWriter().println("Account search successful.");
          if(located!=null){
            client.getPrintWriter().println("Account '"+user+"' located with id: "+located.getId());
          }else{
        	client.getPrintWriter().println("No account located with id: '"+user);
          }
      } catch (Exception e) {
          client.getPrintWriter().println("Account search failed: " + e.getMessage());
      }

      return true;
  }

  public String getDetailedHelp() {
      return null;
  }

  public String getHelp() {
      return "Locates an account using supplied account name";
  }

  public String getPromptCommandString() {
      return "findAccount";
  }

  public String getSyntax() {
	  String example="Ex. findAccount user1";
	  String syntax = "findAccount (*)account.name";

      return example+"\n"+syntax;
  }

}
