package org.rhq.enterprise.client.commands;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

public class StopJBossAsCommand implements ClientCommand {

    public boolean execute(ClientMain client, String[] args) {
        try {
            // check to see if user logged in
            if (client.getSubject() == null) {
                client.getPrintWriter().println("Unable to stop JBossAS instances until successfully logged in");
                return true;
            }

            // Retrieve the parameters passed in
            // String example="Ex. stopJBossAS 09875";
            // String syntax = "stopJBossAS (*)resource.id";
            String applicationServerResourcecId = args[1] + "";
            // do type checking
            int asId = Integer.valueOf(applicationServerResourcecId);

            // retrieve RHQRegistry object
            RemoteClient registry = client.getRemoteClient();
            Subject loggedInUser = client.getSubject();
            // instantiate SLSB
            ResourceManagerRemote res = client.getRemoteClient().getResourceManagerRemote();
            OperationManagerRemote operationManager = client.getRemoteClient().getOperationManagerRemote();
            ResourceOperationSchedule schedule = null;
            // check for instance
            Resource resource = res.getResourceById(client.getSubject(), asId);
            if ((resource != null) && (resource.getCurrentAvailability() != null)) {
                // check for up
                if (resource.getCurrentAvailability().getAvailabilityType().compareTo(AvailabilityType.UP) == 0) {
                    // send the command
                    try {
                        schedule = operationManager.scheduleResourceOperation(client.getSubject(), resource.getId(),
                            "shutdown", 0L, 0L, 0, 0, null, "Remote Client from:" + client.getHost());
                    } catch (Exception ex) {
                        // TODO: fix .. eat for now.
                    }
                    client.getPrintWriter().println(
                        "The command to shutdown JBossAS instance with id '" + asId
                            + "' has been sent. Please allow for a few moments for the server to be shutdown.");
                } else {// Instance already has availability status of DOWN
                    client.getPrintWriter().println(
                        "The JBossAS instance with id '" + asId + "' is already shut down. No action taken.");
                    return true;
                }
            }
        } catch (Exception e) {
            client.getPrintWriter().println("Attempt to stop JBossAS instance failed: " + e.getMessage());
        }

        return true;
    }

    public String getDetailedHelp() {
        String detailedHelp = "";
        return detailedHelp;
    }

    public String getHelp() {
        return "Stops a running (non-RHQ) AS instance.";
    }

    public String getPromptCommandString() {
        return "stopJBossAS";
    }

    public String getSyntax() {
        String example = "Ex. stopJBossAS 09875";
        String syntax = "stopJBossAS (*)resource.id";

        return example + "\n" + syntax;
    }

}
