package org.rhq.enterprise.agent;

import java.io.FileReader;
import java.io.IOException;

import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Uses the SIGAR API when appropriate to read in console input.
 * This falls back to the Java-only superclass implementation if
 * the native SIGAR API is not available.
 * 
 * @author John Mazzitelli
 */
public class SigarAgentInputReader extends JavaAgentInputReader {

    private final AgentMain agent;

    public SigarAgentInputReader(AgentMain agent) throws IOException {
        super();
        this.agent = agent;
    }

    public SigarAgentInputReader(AgentMain agent, FileReader fr) throws IOException {
        super(fr);
        this.agent = agent;
    }

    @Override
    public String readLine() throws IOException {
        String input;

        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        if ((systemInfo == null) || !systemInfo.isNative() || !isConsole()) {
            input = super.readLine();
        } else {
            input = systemInfo.readLineFromConsole(false);
        }

        return input;
    }

    @Override
    public String readLineNoEcho() throws IOException {
        String input = null;

        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        if ((systemInfo == null) || !systemInfo.isNative() || !isConsole()) {
            input = super.readLine();
        } else {
            while (true) {
                // get the answer the first time
                input = systemInfo.readLineFromConsole(true);

                // get the answer a second time
                systemInfo.writeLineToConsole(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM));
                String confirmation = systemInfo.readLineFromConsole(true);
                systemInfo.writeLineToConsole("\n");

                // make sure the first and second answers match; otherwise, ask again
                if (input.equals(confirmation)) {
                    break;
                }

                systemInfo.writeLineToConsole(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM_FAILED));
            }
        }

        return input;
    }
}
