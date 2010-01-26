/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.helpers.alertMigration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Parser for a dump of the 1.3 alert structures via CLI
 * @author Heiko W. Rupp
 */
public class Alert13Parser {

    private final Log log = LogFactory.getLog(Alert13Parser.class);
    File inputFile;
    Reader inputReader;

    public static final String RHQ_USERS = "RHQ Users";

    public Alert13Parser(File input) {
        this.inputFile = input;
        try {
            inputReader = new FileReader(input);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File " + input.getAbsolutePath() + " not found");
        }
    }

    public Alert13Parser(Reader reader) {
        inputReader = reader;
    }

    public Alert13Parser(InputStream stream) {
        inputReader = new InputStreamReader(stream);
    }



    public List<AlertNotification> parse() {

        List<AlertNotification> result = new ArrayList<AlertNotification>();

        List<String[]> lines = null;
        try {
            CSVReader reader = new CSVReader(inputReader);
            lines = reader.readAll();
            reader.close();


            boolean isFirst = true;
        for (String[] line : lines) {
            if (isFirst) {   // Skip over first line with headers
                isFirst=false;
                continue;
            }
            if (line.length<2) // Skip over empty lines
                continue;

            log.debug(Arrays.asList(line));
            String tmp = line[9]; // alert definition id
            int alertDefinitionId = Integer.valueOf(tmp);
            log.debug("  adefid " + alertDefinitionId);

            tmp = line[1]; // Notifications
            CSVReader nr = new CSVReader(new StringReader(tmp));
            String[] notifications = nr.readNext();
            int i = 0;
            int pos = 0;
            while (i < notifications.length) {
                int nid;
                String n = notifications[i];
                log.debug("   Notification: " + n);
                /*
                 * A single notification now runs over multiple lines
                 */
                if (n.startsWith("[") || n.startsWith(" "))
                    n = n.substring(1);

                if (n.equals("]")) {
                    i++;
                    continue;
                }

                nid = Integer.valueOf(getField(n,"id"));


                AlertNotification alNo = new AlertNotification(alertDefinitionId,nid);
                Configuration config = new Configuration();
                alNo.setConfiguration(config);
                alNo.setOrder(pos);

                if (n.startsWith("Subject")) {
                    String subjectId = getField(notifications[i+1],"id");
                    String subject = getField(notifications[i+2],"name");
                    i+=4;
                    log.debug("subject: " + subjectId);
                    config.put(new PropertySimple("subjectId", subjectId));
                    alNo.setSenderName(RHQ_USERS);
                    alNo.setName(subject);

                } else if (n.startsWith("Role")) {
                    String roleId = getField(notifications[i+1],"id");
                    String role = getField(notifications[i+2],"name");
                    i+=4;
                    log.debug("role: " + roleId);
                    config.put(new PropertySimple("roleId", roleId));
                    alNo.setSenderName("Roles");
                    alNo.setName(role);


                } else if (n.startsWith("Snmp")) {
                    String host = getField(notifications[i+1],"host");
                    String port = getField(notifications[i+2],"port");
                    String oid =  getField(notifications[i+3],"oid");
                    i+=5;
                    log.debug("snmp: " + host + ", " + port + ", " + oid);
                    config.put(new PropertySimple("host",host));
                    config.put(new PropertySimple("port",port));
                    config.put(new PropertySimple("OID",oid));
                    alNo.setSenderName("SNMP");
                    alNo.setName(host+oid);

                } else if (n.startsWith("Email")) {
                    String emails = getField(notifications[i+1],"emailAddress");
                    log.debug("email: " + emails);
                    config.put(new PropertySimple("emailAddress",emails));
                    alNo.setSenderName("Email");
                    alNo.setName(emails);
                    i+=3;
                } else {
                    log.error("Unknown notification :>" + n + "<");
                    i++;
                }
                log.debug(" -- for nid : " + nid);

                result.add(alNo);
                pos++;
            }

            tmp = line[12];
            nr = new CSVReader(new StringReader(tmp));
            String[] operations = nr.readNext();
//            for (String op: operations) {
//                log.debug("   Operation: " + op);
//            }

        }
 } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return result;
        }

        return result;
    }

    private String getField(String input, String fieldName) {

        String str = fieldName + "=";
        int pos = input.indexOf(str);
        if (pos==-1)
            return "";
        String tmp = input.substring(pos+str.length());
        if (tmp.endsWith("]"))
            tmp = tmp.substring(0,tmp.length()-1);

        return tmp;
    }
}
