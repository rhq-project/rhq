/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import mazz.i18n.Msg;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Manages the configuration preferences.
 *
 * @author John Mazzitelli
 */
public class ConfigPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.CONFIG);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        try {
            AgentConfiguration agent_config = agent.getConfiguration();
            Preferences preferences = agent_config.getPreferences();

            if ((args.length != 2) && (args.length != 3)) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            } else if (args.length == 2) {
                if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.CONFIG_LIST))) {
                    out.println(getPreferencesXml(preferences));
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                }
            } else if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.CONFIG_IMPORT))) {
                String importFile = args[2];
                agent.loadConfigurationFile(importFile);

                out.println(MSG.getMsg(AgentI18NResourceKeys.CONFIG_IMPORT_CONFIG_IMPORTED) + importFile);
            } else if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.CONFIG_EXPORT))) {
                String exportFileString = args[2];
                File exportFile = new File(exportFileString);
                PrintWriter pw = new PrintWriter(new FileOutputStream(exportFile));

                pw.write(getPreferencesXml(preferences));
                pw.close();

                out.println(MSG.getMsg(AgentI18NResourceKeys.CONFIG_EXPORT_CONFIG_EXPORTED) + exportFile);
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            }
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.CONFIG_FAILURE));
            e.printStackTrace(out);
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.CONFIG_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.CONFIG_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.CONFIG_DETAILED_HELP);
    }

    /**
     * Returns formatted XML containing the preferences.
     *
     * @param  preferences the preferences whose formatted XML string is to be returned
     *
     * @return formated XML
     *
     * @throws Exception
     */
    private String getPreferencesXml(Preferences preferences) throws Exception {
        // get the raw, unformatted XML for the preferences
        ByteArrayOutputStream unformatted = new ByteArrayOutputStream();
        preferences.exportSubtree(unformatted);

        // now format the XML
        ByteArrayInputStream bais = new ByteArrayInputStream(unformatted.toByteArray());
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        // BZ 536138 & 1038728, don't resolve the preferences DTD.
        docBuilder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });
        Document doc = docBuilder.parse(bais);
        Source source = new DOMSource(doc);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // We'll want the DTD to output document
        String xmlDec = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
        String dtdDec = "<!DOCTYPE preferences SYSTEM \"http://java.sun.com/dtd/preferences.dtd\">";

        output.write(xmlDec.getBytes("UTF-8"));
        output.write('\n');
        output.write(dtdDec.getBytes("UTF-8"));
        output.write('\n');
        Result result = new StreamResult(output);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);

        return output.toString();
    }
}