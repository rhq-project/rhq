/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.agent.promptcmd;

import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.AgentManagement;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.util.StringUtil;

/**
 * Displays the current metrics emitted by the agent itself.
 *
 * @author John Mazzitelli
 */
public class MetricsPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.METRICS);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        // There are other, easier ways to do this, but I want to go through the JMX interface.
        // Since this is the interface used by the agent plugin to get metric data, this prompt
        // command can be used to verify the exact data the agent plugin is getting.
        AgentManagement agent_mbean = agent.getAgentManagementMBean();

        if (agent_mbean != null) {
            try {
                MBeanServer the_mbs = agent_mbean.getMBeanServer();
                displayMetrics(agent_mbean.getObjectName(), out, the_mbs);
            } catch (Exception e) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.METRICS_EXCEPTION, e));
            }
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.METRICS_NO_SERVICES));
        }

        return true;
    }

    /**
     * Queries all the agent management MBean attributes and displays them.
     *
     * @param  object_name the name of the management MBean
     * @param  out         the output to write the information
     * @param  the_mbs     the MBeanServer where the management MBean services are
     *
     * @throws Exception
     */
    private void displayMetrics(ObjectName object_name, PrintWriter out, MBeanServer the_mbs) throws Exception {
        // get all the attributes the management MBean exposes and then ask for all their values
        MBeanInfo mbeaninfo = the_mbs.getMBeanInfo(object_name);
        MBeanAttributeInfo[] attrib_infos = mbeaninfo.getAttributes();
        String[] attrib_names = new String[attrib_infos.length];

        for (int i = 0; i < attrib_infos.length; i++) {
            attrib_names[i] = attrib_infos[i].getName();
        }

        HashMap<String, Object> name_value_pairs = new HashMap<String, Object>();
        AttributeList attribs = the_mbs.getAttributes(object_name, attrib_names);

        for (Iterator iter = attribs.iterator(); iter.hasNext();) {
            Attribute attrib = (Attribute) iter.next();
            String name = attrib.getName();
            Object value = attrib.getValue();

            // some attributes we do not want to display; skip them as we find them
            if (name.equals("AgentConfiguration")) {
                continue; // skip because this makes the output too big and noisy - user can use getconfig to see this data
            }

            // we might want to format some attributes into more human readable forms
            if (name.equals("CurrentTime")) {
                value = new Date(((Long) value).longValue());
            } else if (name.equals("Uptime")) {
                value = formatSeconds(((Long) value).longValue());
            } else if (name.equals("JVMTotalMemory") || name.equals("JVMFreeMemory")) {
                value = formatBytes(((Long) value).longValue());
            }

            name_value_pairs.put(name, value);
        }

        out.println(MSG.getMsg(AgentI18NResourceKeys.METRICS_HEADER));
        out.println(StringUtil.justifyKeyValueStrings(name_value_pairs));

        return;
    }

    /**
     * Formats the given time duration (specified in seconds) into seconds, minutes, hours or days as appropriate.
     *
     * @param  seconds the time duration to format
     *
     * @return the number of seconds in string form and in the most appropriate units (seconds, minutes, hours, days)
     */
    private String formatSeconds(long seconds) {
        String ret_val = null;

        if (seconds >= (60 * 60 * 24)) {
            ret_val = String.format("%.1f ", new Object[] { ((float) seconds / (60L * 60 * 24)) })
                + MSG.getMsg(AgentI18NResourceKeys.UNITS_DAYS);
        } else if (seconds >= (60 * 60)) {
            ret_val = String.format("%.1f ", new Object[] { ((float) seconds / (60L * 60)) })
                + MSG.getMsg(AgentI18NResourceKeys.UNITS_HOURS);
        } else if (seconds >= (60)) {
            ret_val = String.format("%.1f ", new Object[] { ((float) seconds / (60L)) })
                + MSG.getMsg(AgentI18NResourceKeys.UNITS_MINUTES);
        }

        if (ret_val != null) {
            ret_val += " (" + seconds + ")";
        } else {
            ret_val = seconds + " " + MSG.getMsg(AgentI18NResourceKeys.UNITS_SECONDS);
        }

        return ret_val;
    }

    /**
     * Formats a count of bytes into KB, MB, GB or TB as appropriate.
     *
     * @param  bytes
     *
     * @return the number of bytes in string form and in the most appropriate units
     */
    private String formatBytes(long bytes) {
        String ret_str = "";

        // we only understand KB, MB, GB and TB. Anything outside that range is returned in bytes
        // KB=1,000 : MB=1,000,000 : GB=1,000,000,000 : TB=1,000,000,000,000
        if ((bytes >= 1000) && (bytes <= 999999999999999L)) {
            if (bytes < 1000000) {
                ret_str += String.format("%.2f", new Object[] { (float) bytes / 1000L }) + " KB";
            } else if (bytes < 1000000000) {
                ret_str += String.format("%.2f", new Object[] { (float) bytes / 1000000L }) + " MB";
            } else if (bytes < 1000000000000L) {
                ret_str += String.format("%.2f", new Object[] { (float) bytes / 1000000L }) + " GB";
            } else {
                ret_str += String.format("%.2f", new Object[] { (float) bytes / 1000000000L }) + " TB";
            }

            ret_str += " (" + bytes + ")";
        } else {
            ret_str += bytes;
        }

        return ret_str;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.METRICS_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.METRICS_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.METRICS_DETAILED_HELP);
    }
}