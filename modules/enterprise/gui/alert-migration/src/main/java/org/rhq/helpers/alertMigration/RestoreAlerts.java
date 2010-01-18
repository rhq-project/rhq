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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Read in a dump file and apply the (old style) alert notifications
 * to the new way of doing notifications
 *
 * @author Heiko W. Rupp
 */
public class RestoreAlerts {

    private final Log log = LogFactory.getLog(RestoreAlerts.class);


    public static void main(String[] args) {

        if (args.length<1) {
            usage();
            System.exit(1);
        }

        File file = new File(args[0]);
        if (!file.canRead()) {
            System.err.println("Can not read file " + file.getAbsolutePath());
            System.exit(2);
        }

        Alert13Parser parser = new Alert13Parser(file);
        parser.parse();

        System.exit(0);

        boolean isFirst = true;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (br.ready()) {
                String line = br.readLine();
                if (isFirst) { // Skip over line with headers
                    isFirst = false;
                    continue;
                }
                if (line.length()==0)
                        continue;

                // Split line into the items
                String[] items = line.split("\",");
                if (items.length==1)
                    continue;
                System.out.println(items[1]);
                String tmp = items[8].substring(1);
                int alertDefinitionId = Integer.parseInt(tmp);
                System.out.println("  " + alertDefinitionId);
                tmp = items[1].substring(1);
                String[] notifications = splitNotifications(tmp);


            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String[] splitNotifications(String tmp) {

        tmp = tmp.substring(1); // remove ^[
        tmp = tmp.substring(0,tmp.length()-1); // remove ]$

        List<String> notifs = new ArrayList<String>();


        return new String[0];  // TODO: Customise this generated block
    }

    private static void usage() {
        System.out.println("Usage:");
        System.out.println("  RestoreAlerts inputFile");
    }

}
