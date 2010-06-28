package org.rhq.enterprise.agent.promptcmd;

import java.io.PrintWriter;
import java.util.Map;

import mazz.i18n.Msg;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.StringUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

public class SchedulesPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.SCHEDULES);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        try {
            if (args.length != 2) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            } else {
                try {
                    int resourceId = Integer.parseInt(args[1]);
                    MeasurementManager measurementManager = PluginContainer.getInstance().getMeasurementManager();
                    Map<String, Object> nameValuePairs = measurementManager
                        .getMeasurementScheduleInfoForResource(resourceId);

                    if (nameValuePairs == null) {
                        // take string value so resourceId is rendered literally, instead of numerical formatting
                        out.println(MSG.getMsg(AgentI18NResourceKeys.SCHEDULES_UNKNOWN_RESOURCE, String
                            .valueOf(resourceId)));
                    } else {
                        out.println(StringUtil.justifyKeyValueStrings(nameValuePairs));
                    }
                } catch (NumberFormatException nfe) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                }
            }
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.SCHEDULES_FAILURE));
            e.printStackTrace(out);
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.SCHEDULES_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SCHEDULES_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return getHelp();
    }
}
