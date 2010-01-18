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

import org.rhq.core.domain.alert.notification.AlertNotification;

/**
 * Read in a dump file and print the Alert notifications to stdout.
 * Only useful for testing.
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
        List<AlertNotification> notifications = parser.parse();
        
        System.out.println("Found " + notifications.size() + " notifications in the dump");
        for (AlertNotification n : notifications)
            System.out.println(n);

    }


    private static void usage() {
        System.out.println("Usage:");
        System.out.println("  RestoreAlerts inputFile");
    }

}
