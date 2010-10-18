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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.rhq.helpers.perftest.support.config.Entity;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;
import org.rhq.helpers.perftest.support.jpa.mapping.MappingTranslator;

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
        
        Getopt options = new Getopt("data", args, "eihr:u:p:d:c:f:o:", longOptions);
        
        String url = null;
        String user = null;
        String password = null;
        String driverClass = null;
        String configFile = null;
        String ioFileName = null;
        String format = "xml";
        boolean doExport = false;
        boolean doImport = false;
        List<String> entities = new ArrayList<String>();
        
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
            entities.add(args[i]);
        }
        
        Properties settings = new Properties();
        putNotNull(settings, "url", url);
        putNotNull(settings, "user", user);
        putNotNull(settings, "password", password);
        putNotNull(settings, "driverClass", driverClass);
        
        if (!validate(settings)) {
            System.exit(1);
        }

        ExportConfiguration config = null;
        
        if (configFile != null) {
            JAXBContext c = ExportConfiguration.getJAXBContext();
            Unmarshaller um = c.createUnmarshaller();
            config = (ExportConfiguration) um.unmarshal(new FileReader(configFile));
        }
        
        if (config == null) {
            config = new ExportConfiguration();
            
            //only use the entities from the command line if no config file
            //was specified.
            for(String entity : entities) {
                Entity e = new Entity();
                e.setName(entity);
                e.setIncludeAllFields(true);
                e.setFilter("SELECT * FROM " + MappingTranslator.getTableName(config.getClassForEntity(e)));
                config.getEntities().add(e);
            }            
        }
        config.setSettings(settings);
        
        if (doExport) {
            Output output = Settings.getOutputObject(format, ioFileName);
            
            try {
                Exporter.run(config, output.getConsumer());
            } finally {
                output.close();
            }
        } else if (doImport) {
            Input input = Settings.getInputObject(format, ioFileName);
            try {
                Importer.run(settings, input);
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
    
    private static boolean validate(Properties settings) {
        boolean ok = true;
        
        if (!settings.containsKey("url")) {
            System.err.println("The url of the database to connect to is missing.");
            ok = false;
        }
        
        return ok;
    }
    
    private static void usage() {
        System.out.println("Usage:");
        
        System.out.println("data(.sh|.bat) (--export|--import) [<other-options>] [entity-names...]");
        System.out.println();
        System.out.println("--url : the JDBC URL to the database");
        System.out.println("--username : the database username");
        System.out.println("--password : the database password");
        System.out.println("--driver-class : the full name of the JDBC driver class");
        System.out.println("--config-file : The configuration file specifying what entities to export");
        System.out.println("--file : the file to export the data to or to import the data from (defaults to standard output or input respectively)");
        System.out.println("--format : one of " + Arrays.asList(FileFormat.values()));
        System.out.println("--help : this info");
        
        System.exit(0);
    }
}
