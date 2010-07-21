package org.rhq.enterprise.agent.promptcmd;

import java.io.PrintWriter;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.enterprise.agent.AgentMain;

/**
 * Allows the user to add new ResourceTypes via the agent console interface
 * 
 * @author Alexander Kiefer
 */
public class AddResourceTypePromptCommand implements AgentPromptCommand {

    @Override
    public boolean execute(AgentMain agent, String[] args) {

        //PrintWriter for enabling output to the user
        PrintWriter out = agent.getOut();

        //Get an instance of PluginContainer
        PluginContainer pc = PluginContainer.getInstance();

        // the PC must be started, otherwise return 
        if (!agent.isStarted() || !pc.isStarted()) {
            out.println("Agent or Plugin Conatiner have not been started!!");
            return false;
        }

        // process the inventory
        InventoryManager inventoryManager = pc.getInventoryManager();

        //Add new ResourceType
        inventoryManager.createNewResourceType("AlexTestType", "AlexTestTypeMetric");

        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public String getDetailedHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPromptCommandString() {

        String addResourceTypeCommand = "addType";

        return addResourceTypeCommand;
    }

    @Override
    public String getSyntax() {
        // TODO Auto-generated method stub
        return null;
    }

}
