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
import java.util.List;
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

        Props props = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {

            props = askQuestions(br, new Props());
            if (props == null) {
                // abort by user
                return;
            }

            boolean done = false;
            do {

                System.out.println();
                System.out.print("Do you want to add a child to " + props.getName() + "? (y/N) ");
                String answer = br.readLine();
                if (answer==null) {
                    break;
                }
                answer = answer.toLowerCase(Locale.getDefault());
                if (answer.startsWith("n") || answer.length() == 0)
                    done = true;
                else {
                    Props child = askQuestions(br, props);
                    if (child == null) {
                        // abort by user
                        return;
                    }
                    props.getChildren().add(child);
                }

            } while (!done);
        } catch (IOException ioe) {
            System.err.println("Internal error happended: " + ioe.getMessage());
        } finally {
                br.close();
        }

        if (props!=null) {
            log.info("\nYou have chosen:\n" + props.toString());
            postprocess(props);
            generate(props);
        }

        System.out.println("Don't forget to ");
        System.out.println("  - add your plugin to the parent pom.xml if needed");
        System.out.println("  - edit pom.xml of your plugin");
        System.out.println("  - edit rhq-plugin.xml of your plugin");
    }

    /**
     * Do some post processing over the input received.
     * @param props The properties just recorded from the user input
     */
    private void postprocess(Props props) {

        // Set the package
        String pkg = props.getPackagePrefix() + "." + props.getName();
        props.setPkg(pkg);

        for (Props cProp : props.getChildren()) {
            cProp.setPkg(pkg);
        }

    }

    /**
     * Ask the questions by introspecting the {@link Props} class
     * @param br  BufferedReader to read the users answers from
     * @param parentProps Props of the parent - some of them will be copied to the children
     * @return an initialized Props object
     * @throws Exception if anything goes wrong
     * @see org.rhq.helpers.pluginGen.Props
     */
    private Props askQuestions(BufferedReader br, Props parentProps) throws Exception {

        Method[] meths = Props.class.getDeclaredMethods();
        Props props = new Props();

        System.out.print("Please specify the plugin root category ");
        List<ResourceCategory> possibleChildren = ResourceCategory.getPossibleChildren(parentProps.getCategory());
        for (ResourceCategory cat : possibleChildren) {
            System.out.print(cat + "(" + cat.getAbbrev() + "), ");
        }

        String answer = br.readLine();
        answer = answer.toUpperCase(Locale.getDefault());
        ResourceCategory cat = ResourceCategory.getByAbbrv(answer.charAt(0));
        if (cat != null)
            props.setCategory(cat);
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

            if (name.equals("PackagePrefix") && parentProps.getPackagePrefix() != null) {
                props.setPackagePrefix(parentProps.getPackagePrefix());
            } else if (name.equals("FileSystemRoot") && parentProps.getFileSystemRoot() != null) {
                props.setFileSystemRoot(parentProps.getFileSystemRoot());
            } else if (name.equals("ParentType") && parentProps.getName() != null) {
                // Set parent type always when we are in the child
                props.setParentType(caps(parentProps.getComponentClass()));
            } else if (name.equals("UsesExternalJarsInPlugin") && parentProps.getName() != null) {
                // Skip this one on children
            } else if (name.equals("UsePluginLifecycleListenerApi") && parentProps.getName() != null) {
                // Skip this one on children
            } else if (name.equals("DependsOnJmxPlugin") && parentProps.getName() != null) {
                // Skip this one on children
            } else if (name.equals("RhqVersion") && parentProps.getName() != null) {
                // Skip this one on children
            } else if (name.equals("Pkg")) {
                // Always skip this - we postprocess it
            } else {

                System.out.print("Please specify");
                boolean isBool = false;
                if (retType.equals(Boolean.TYPE)) {
                    System.out.print(" if it should support " + name + " (y/N): ");
                    isBool = true;
                } else {
                    System.out.print(" its " + name + ": ");
                }

                answer = br.readLine();
                if (answer == null) {
                    System.out.println("EOL .. aborting");
                    return null;
                }
                String setterName = "set" + caps(name);

                Method setter;
                if (isBool)
                    setter = Props.class.getMethod(setterName, Boolean.TYPE);
                else
                    setter = Props.class.getMethod(setterName, String.class);

                if (isBool) {
                    if (answer.toLowerCase(Locale.getDefault()).startsWith("y")
                        || answer.toLowerCase(Locale.getDefault()).startsWith("j")) {
                        setter.invoke(props, true);
                    }
                } else {
                    if (!answer.startsWith("\n") && !answer.startsWith("\r") && !(answer.length() == 0))
                        setter.invoke(props, answer);
                }
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
            return;
        }

        File baseDir = new File(props.getFileSystemRoot());
        if (!baseDir.isDirectory()) {
            log.error("This is no directory: '" + baseDir.getAbsolutePath() +"'");
            return;
        }

        boolean success;
        File activeDirectory = new File(props.getFileSystemRoot(), props.getName());

        if (!activeDirectory.exists()) {
            success = activeDirectory.mkdir();
            if (!success) {
                log.error("Creation of plugin basedir failed");
                return;
            }
        }

        // write pom.xml
        createFile(props, "pom", "pom.xml", activeDirectory.getAbsolutePath());

        // Create java directory hierarchie
        String path = activeDirectory.getAbsolutePath() + File.separator + "src" + File.separator + "main"
            + File.separator;

        activeDirectory = new File(path);
        if (!activeDirectory.exists()) {
            success = activeDirectory.mkdirs();
            if (!success) {
                log.error("Creation of main directory failed");
                return;
            }
        }
        File resourceDirs = new File(path + File.separator + "resources" + File.separator + "META-INF");
        if (!resourceDirs.exists()) {
            success = resourceDirs.mkdirs();
            if (!success) {
                log.error("Creation of resources/META-INF failed");
                return;
            }
        }
        // create rhq-plugin.xml below resourceDirs
        createFile(props, "descriptor", "rhq-plugin.xml", resourceDirs.getAbsolutePath());

        File javaDirs = new File(path + File.separator + "java" + File.separator
            + toDirPath(props.getPackagePrefix(), File.separator) + props.getName());
        if (!javaDirs.exists()) {
            success = javaDirs.mkdirs();
            if (!success) {
                log.error("Creation of java package failed");
                return;
            }
        }
        // create Discovery and component classes
        createFile(props, "discovery", props.getDiscoveryClass() + ".java", javaDirs.getAbsolutePath());
        createFile(props, "component", props.getComponentClass() + ".java", javaDirs.getAbsolutePath());

        if (props.isEvents()) {
            createFile(props, "eventPoller", caps(props.getName()) + "EventPoller.java", javaDirs.getAbsolutePath());
        }

        // See if there are children and create for them too
        if (!props.getChildren().isEmpty())
            log.info("Creating child services");

        for (Props cProps : props.getChildren()) {
            createFile(cProps, "discovery", cProps.getDiscoveryClass() + ".java", javaDirs.getAbsolutePath());
            createFile(cProps, "component", cProps.getComponentClass() + ".java", javaDirs.getAbsolutePath());

            // create EventPoller
            if (cProps.isEvents()) {
                createFile(cProps, "eventPoller", caps(cProps.getName()) + "EventPoller.java", javaDirs
                    .getAbsolutePath());
            }
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
     * @param props The properties used to create the respective file
     * @param template The name of the template without .ftl suffix
     * @param fileName The name of the file to create
     * @param directory The name of the directory to create in
     */
    public void createFile(Props props, String template, String fileName, String directory) {

        try {
            log.info("Trying to generate " + directory + "/" + fileName);
            Configuration config = new Configuration();

            // XXX fall-over to ClassTL after failure in FTL seems not to work
            // FileTemplateLoader ftl = new FileTemplateLoader(new File("src/main/resources"));
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/");
            TemplateLoader[] loaders = new TemplateLoader[] { ctl };
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);

            config.setTemplateLoader(mtl);

            Template templ = config.getTemplate(template + ".ftl");

            Writer out = new BufferedWriter(new FileWriter(new File(directory, fileName)));
            try {
                Map<String, Props> root = new HashMap<String, Props>();
                root.put("props", props);
                templ.process(root, out);
            }
            finally {
                out.flush();
                out.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (TemplateException te) {
            te.printStackTrace();
        }

    }

    static String caps(String in) {
        if (in == null)
            return null;

        return in.substring(0, 1).toUpperCase(Locale.getDefault()) + in.substring(1);
    }
}
