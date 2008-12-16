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
package org.rhq.helpers.pluginGen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Main class for the plugin generator
 *
 * @author Heiko W. Rupp
 */
public class PluginGen {

    private final Log log = LogFactory.getLog(PluginGen.class);

    public static void main(String[] arg) throws Exception {
        PluginGen pg = new PluginGen();
        pg.run();

    }

    public PluginGen() {
    }

    public void run() throws Exception {

        Props props = askQuestions();
        log.info("\nYou have choosen:\n" + props.toString());
        generate(props);
    }

    private Props askQuestions() throws Exception {

        Method[] meths = Props.class.getDeclaredMethods();
        Props props = new Props();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Please speficy the plugin root category (Platform, Server, servIce): ");
        String answer = br.readLine();
        answer = answer.toLowerCase(Locale.getDefault());
        if (answer.startsWith("p"))
            props.setCategory(Props.ResourceCategory.PLATFORM);
        else if (answer.startsWith("s"))
            props.setCategory(Props.ResourceCategory.SERVER);
        else if (answer.startsWith("i"))
            props.setCategory(Props.ResourceCategory.SERVICE);
        else {
            System.err.println("Bad answer, only use P/S/I");
            System.exit(1);
        }

        for (Method m : meths) {
            String name = m.getName();
            if (!name.startsWith("get") && !name.startsWith("is"))
                continue;

            Class retType = m.getReturnType();
            if (!retType.equals(String.class) && !retType.equals(Boolean.TYPE)) {
                continue;
            }

            if (name.startsWith("get"))
                name = name.substring(3);
            else
                name = name.substring(2);

            System.out.print("Please specify");
            boolean isBool = false;
            if (retType.equals(Boolean.TYPE)) {
                System.out.print(" if it should support " + name + " (y/N): ");
                isBool = true;
            } else {
                System.out.print(" its " + name + ": ");
            }

            answer = br.readLine();
            String setterName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);

            Method setter;
            if (isBool)
                setter = Props.class.getMethod(setterName, Boolean.TYPE);
            else
                setter = Props.class.getMethod(setterName, String.class);

            if (isBool) {
                if (answer.toLowerCase().startsWith("y") || answer.toLowerCase().startsWith("j")) {
                    setter.invoke(props, true);
                }
            } else {
                if (!answer.startsWith("\n") && !answer.startsWith("\r") && !(answer.length()==0))
                    setter.invoke(props, answer);
            }

        }

        return props;
    }

    /**
     * Trigger the generation of the directory hierarchy.
     * @param props Parameters to take into account
     */
    private void generate(Props props) {

        log.info("Generating...");

        if (props.getFileSystemRoot() == null || props.getFileSystemRoot().equals("")) {
            log.error("No root directory given, can not continue");
            System.exit(1);
        }

        File baseDir = new File(props.getFileSystemRoot());
        if (!baseDir.isDirectory()) {
            log.error("This is no directory: " + baseDir.getAbsolutePath());
            return;
        }

        boolean success = false;
        File activeDirectory = new File(props.getFileSystemRoot(), props.getName());

        if (!activeDirectory.exists()) {
            success = activeDirectory.mkdir();
            if (!success) {
                log.error("Creation of plugin basedir failed");
                return;
            }
        }

        // write pom.xml
        createFile(props,"pom","pom.xml",activeDirectory.getAbsolutePath());

        // Create java directory hierarchie
        String path = activeDirectory.getAbsolutePath() + File.separator
                + "src" + File.separator
                + "main" + File.separator;

        activeDirectory = new File(path);
        if (!activeDirectory.exists()) {
            success = activeDirectory.mkdirs();
            if (!success) {
                log.error("Creation of main directory failed");
                return;
            }
        }
        File resourceDirs = new File(path + File.separator
                + "resources" + File.separator
                + "META-INF");
        if (!resourceDirs.exists()) {
            success = resourceDirs.mkdirs();
            if (!success) {
                log.error("Creation of resources/META-INF failed");
                return;
            }
        }
        // create rhq-plugin.xml below resourceDirs
        createFile(props,"descriptor","rhq-plugin.xml",resourceDirs.getAbsolutePath());

        File javaDirs = new File(path + File.separator
                + "java" + File.separator
                + toDirPath(props.getPackagePrefix(), File.separator)
                + props.getName()
        );
        if (!javaDirs.exists()) {
            success = javaDirs.mkdirs();
            if (!success) {
                log.error("Creation of java package failed");
                return;
            }
        }
        // create Discovery and component classes
        createFile(props,"discovery",props.getDiscoveryClass()+".java",javaDirs.getAbsolutePath());
        createFile(props,"component",props.getComponentClass()+".java",javaDirs.getAbsolutePath());

        if (props.isEvents()) {
            createFile(props,"eventPoller", "DummyEventPoller.java", javaDirs.getAbsolutePath());
        }
        log.info("Done ..");

    }

    /**
     * Translate a packgage into a filesystem path
     * @param pkg Package in standard notation like com.acme.plugins
     * @param separator File separator
     * @return a path suitable to pass to File
     */
    private String toDirPath(String pkg, String separator) {

        String res = pkg.replaceAll("\\.", separator);
        if (!pkg.endsWith("."))
            res += separator;
        return res;
    }

    /**
     * Apply a template to generate a file
     * @param template The name of the template without .ftl suffix
     * @param fileName The name of the file to create
     * @param directory The name of the directory to create in
     */
    public void createFile(Props props, String template, String fileName, String directory) {

        try {
            log.info("Trying to generate " + directory + "/" + fileName );
            Configuration config = new Configuration();

            // XXX fall-over to ClassTL after failure in FTL seems not to work
            // FileTemplateLoader ftl = new FileTemplateLoader(new File("src/main/resources"));
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/");
            TemplateLoader[] loaders = new TemplateLoader[] { ctl};
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);

            config.setTemplateLoader(mtl);

            Template templ = config.getTemplate(template + ".ftl");

            Writer out = new BufferedWriter(new FileWriter(new File(directory,fileName)));
            Map root = new HashMap();
            root.put("props",props);
            templ.process(root, out);
            out.flush();
            out.close();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        catch (TemplateException te) {
            te.printStackTrace();
        }

    }

}
