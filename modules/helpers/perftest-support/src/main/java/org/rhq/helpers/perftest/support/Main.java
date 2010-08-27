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

package org.rhq.helpers.perftest.support;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 *
 * @author Lukas Krejci
 */
public class Main {

    private Main() {
        
    }
    
    public static void main(String[] args) throws Exception {
        LongOpt[] longOptions = new LongOpt[10];
        
        longOptions[0] = new LongOpt("url", LongOpt.REQUIRED_ARGUMENT, null, 'r');
        longOptions[1] = new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u');
        longOptions[2] = new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p');
        longOptions[3] = new LongOpt("driver-class", LongOpt.REQUIRED_ARGUMENT, null, 'd');
        longOptions[4] = new LongOpt("config-file", LongOpt.REQUIRED_ARGUMENT, null, 'c');
        longOptions[5] = new LongOpt("export", LongOpt.NO_ARGUMENT, null, 'e');
        longOptions[6] = new LongOpt("import", LongOpt.NO_ARGUMENT, null, 'i');
        longOptions[7] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longOptions[8] = new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f');
        longOptions[9] = new LongOpt("format", LongOpt.REQUIRED_ARGUMENT, null, 'o');
        
        Getopt options = new Getopt("datagen", args, "eihr:u:p:d:c:f:o:", longOptions);
        
        String url = null;
        String user = null;
        String password = null;
        String driverClass = null;
        String configFile = null;
        String ioFileName = null;
        String format = "xml";
        boolean doExport = false;
        boolean doImport = false;
        List<String> tables = new ArrayList<String>();
        
        int option;
        while ((option = options.getopt()) != -1) {
            switch (option) {
            case 'r':
                url = options.getOptarg();
                break;
            case 'u':
                user = options.getOptarg();
                break;
            case 'p':
                password = options.getOptarg();
                break;
            case 'd':
                driverClass = options.getOptarg();
                break;
            case 'c':
                configFile = options.getOptarg();
                break;
            case 'e':
                doExport = true;
                break;
            case 'i':
                doImport = true;
                break;
            case 'h':
                usage();
                break;
            case 'f':
                ioFileName = options.getOptarg();
                break;
            case 'o':
                format = options.getOptarg();
                break;
                
            }
        }
        
        for (int i = options.getOptind(); i < args.length; i++) {
            tables.add(args[i]);
        }
        
        Properties settings = new Properties();
        
        if (configFile != null) {
            FileInputStream file = new FileInputStream(configFile); 
            try {
                settings.load(file);
            } finally {
                file.close();
            }
        }
        
        putNotNull(settings, "url", url);
        putNotNull(settings, "user", user);
        putNotNull(settings, "password", password);
        putNotNull(settings, "driverClass", driverClass);
        for(String table : tables) {
            putNotNull(settings, "table." + table, "");
        }
        
        validate(settings);
        
        if (doExport) {
            Output output = Settings.getOutputObject(format, ioFileName);
            try {
                Exporter.run(settings, output.getConsumer());
            } finally {
                output.close();
            }
        } else if (doImport) {
            Input input = Settings.getInputObject(format, ioFileName);
            try {
                Importer.run(settings, input.getProducer());
            } finally {
                input.close();
            }
        } else {
            System.err.println("You must specify whether to export or import.");
            System.exit(1);
        }
        
        System.exit(0);
    }
    
    private static void putNotNull(Properties settings, String key, String value) {
        if (value != null) {
            settings.put(key, value);
        }
    }
    
    private static void validate(Properties settings) {
        //TODO implement
    }
    
    private static void usage() {
        System.out.println("Usage:");
        
        //TODO implement
        
        System.exit(0);
    }
}
