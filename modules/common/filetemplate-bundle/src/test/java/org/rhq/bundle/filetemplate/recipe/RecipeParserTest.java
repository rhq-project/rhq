/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.bundle.filetemplate.recipe;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.updater.DeploymentProperties;

@Test
public class RecipeParserTest {
    private StringBuilder recipe;

    @BeforeMethod
    public void cleanRecipeBeforeMethod() {
        cleanRecipe();
    }

    public void testBundleCommand() throws Exception {
        addRecipeCommand("bundle --version \"1.0\" --name \"my-name\" --description \"my description here\"");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        DeploymentProperties props = context.getDeploymentProperties();
        assert props.getBundleName().equals("my-name");
        assert props.getBundleVersion().equals("1.0");
        assert props.getDescription().equals("my description here");
        assert props.getDeploymentId() == 0;

        cleanRecipe();
        addRecipeCommand("bundle --name=my-name --version=1.0 \"--description=my description here\"");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        props = context.getDeploymentProperties();
        assert props.getBundleName().equals("my-name");
        assert props.getBundleVersion().equals("1.0");
        assert props.getDescription().equals("my description here");
        assert props.getDeploymentId() == 0;

        // use the "-arg value" notation, as opposed to "--arg=value"
        cleanRecipe();
        addRecipeCommand("bundle -n my-name2 -v 2.0 -d \"my description here 2\"");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        props = context.getDeploymentProperties();
        assert props.getBundleName().equals("my-name2");
        assert props.getBundleVersion().equals("2.0");
        assert props.getDescription().equals("my description here 2");
        assert props.getDeploymentId() == 0;

        // show that you only need name and version but not description
        cleanRecipe();
        addRecipeCommand("bundle -n one -v 1.0");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        props = context.getDeploymentProperties();
        assert props.getBundleName().equals("one");
        assert props.getBundleVersion().equals("1.0");
        assert props.getDescription() == null;
        assert props.getDeploymentId() == 0;

        // show that you need name and version
        cleanRecipe();
        addRecipeCommand("bundle -n one");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        try {
            parser.parseRecipe(context);
            assert false : "should not have parsed, missing version";
        } catch (Exception ok) {
            // expected
        }

        cleanRecipe();
        addRecipeCommand("bundle --name \" \" -v 1");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        try {
            parser.parseRecipe(context);
            assert false : "should not have parsed, blank name not allowed";
        } catch (Exception ok) {
            // expected
        }

        cleanRecipe();
        addRecipeCommand("bundle --version \" \" -n name");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        try {
            parser.parseRecipe(context);
            assert false : "should not have parsed, blank version not allowed";
        } catch (Exception ok) {
            // expected
        }

        cleanRecipe();
        addRecipeCommand("bundle -v 1.0");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        try {
            parser.parseRecipe(context);
            assert false : "should not have parsed, missing name";
        } catch (Exception ok) {
            // expected
        }

        // show that you can't have two bundle commands in the same recipe
        cleanRecipe();
        addRecipeCommand("bundle -n first -v 1.0");
        addRecipeCommand("bundle -n second -v 2.0");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        try {
            parser.parseRecipe(context);
            assert false : "should not have parsed, not allowed multiple bundle commands";
        } catch (Exception ok) {
            // expected
        }
    }

    public void testConfigDefRecipe() throws Exception {
        addRecipeCommand("configdef --name=my.first.property");
        addRecipeCommand("configdef -n custom.prop");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Set<String> vars = context.getReplacementVariables();
        assert vars.size() == 2 : vars;
        assert vars.contains("my.first.property");
        assert vars.contains("custom.prop");
        assert context.getReplacementVariableDefaultValues().isEmpty();

        cleanRecipe();
        addRecipeCommand("configdef -n custom.prop -d 8080");
        addRecipeCommand("configdef --name another.prop --default some.default.value");
        parser = new RecipeParser();
        context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        vars = context.getReplacementVariables();
        assert vars.size() == 2 : vars;
        assert vars.contains("custom.prop");
        assert vars.contains("another.prop");
        assert context.getReplacementVariableDefaultValues().containsKey("another.prop");
        assert context.getReplacementVariableDefaultValues().get("another.prop").equals("some.default.value");
    }

    public void testRealizeRecipe() throws Exception {
        addRecipeCommand("realize --file=@@opt.dir@@/config.ini");
        addRecipeCommand("realize -f @@opt2.dir@@/config2.ini");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Set<String> files = context.getRealizedFiles();
        assert files.size() == 2 : files;
        assert files.contains("@@opt.dir@@/config.ini") : files;
        assert files.contains("@@opt2.dir@@/config2.ini") : files;
    }

    public void testFileRecipe() throws Exception {
        addRecipeCommand("file --source=run-me.sh --destination=/opt/run.sh");
        addRecipeCommand("file -s META-INF/another.xml -d /etc/another.xml");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getFiles();
        assert files.size() == 2 : files;
        assert files.get("run-me.sh") != null : files;
        assert files.get("run-me.sh").equals("/opt/run.sh") : files;
        assert files.get("META-INF/another.xml") != null : files;
        assert files.get("META-INF/another.xml").equals("/etc/another.xml") : files;
    }

    public void testScriptRecipe() throws Exception {
        addRecipeCommand("script run-me.sh");
        addRecipeCommand("script lots-o-params.sh -d /opt/jboss -- foo.txt \"hello world\"");
        addRecipeCommand("script run-me.sh with params");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Set<String> scripts = context.getScriptFiles();
        assert scripts.size() == 2 : scripts;
        assert scripts.contains("run-me.sh") : scripts;
        assert scripts.contains("lots-o-params.sh") : scripts;
    }

    public void testSimpleRecipeReplacementVariables() throws Exception {
        addRecipeCommand("# @@ ignored.inside.comment @@");
        addRecipeCommand("deploy -f jboss.tar -d \"@@ opt.dir @@/jboss\"");
        addRecipeCommand("deploy -f tomcat.tar -d @@opt.dir@@/tomcat");
        addRecipeCommand("deploy -f jboss.zip -d @@rhq.system.hostname@@/opt/tomcat"); // this is ignored, its an agent fact variable
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("@@ opt.dir @@/jboss") : files;
        assert files.containsKey("tomcat.tar") : files;
        assert files.get("tomcat.tar").equals("@@opt.dir@@/tomcat") : files;
        assert files.containsKey("jboss.zip") : files;
        assert files.get("jboss.zip").equals("@@rhq.system.hostname@@/opt/tomcat") : files;
        assert context.getReplacementVariables().contains("opt.dir") : context.getReplacementVariables();
        assert context.getReplacementVariables().size() == 1 : context.getReplacementVariables();
    }

    public void testSimpleRecipeReplaceReplacementVariables() throws Exception {
        addRecipeCommand("deploy -f jboss.tar -d @@ opt.dir @@/jboss");
        addRecipeCommand("deploy -f jboss2.tar -d @@unknown@@/jboss");
        addRecipeCommand("deploy -f jboss3.tar -d @@ rhq.system.hostname @@/jboss");
        RecipeParser parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        RecipeContext context = new RecipeContext(getRecipe());
        context.addReplacementVariableValue("opt.dir", "/foo/bar");
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("/foo/bar/jboss") : files;
        assert files.containsKey("jboss2.tar") : files;
        assert files.get("jboss2.tar").equals("@@unknown@@/jboss") : files;
        assert files.containsKey("jboss3.tar") : files;
        assert files.get("jboss3.tar").equals(SystemInfoFactory.createSystemInfo().getHostname() + "/jboss") : files;
    }

    public void testSimpleRecipeReplaceJavaSystemPropertyReplacementVariables() throws Exception {
        /*
        java.util.Properties sysprops = System.getProperties();
        for (Map.Entry<Object, Object> sysprop : sysprops.entrySet()) {
            if (sysprop.getValue().toString().length() < 60) {
                System.out.println("==>" + sysprop.getKey() + "=" + sysprop.getValue());
            }
        }
        */

        addRecipeCommand("deploy -f jboss1.tar -d @@rhq.system.sysprop.java.io.tmpdir@@");
        addRecipeCommand("deploy -f jboss2.tar -d @@rhq.system.sysprop.file.separator@@");
        //addRecipeCommand("deploy -f jboss3.tar -d @@rhq.system.sysprop.line.separator@@"); // can't test this here
        addRecipeCommand("deploy -f jboss4.tar -d @@rhq.system.sysprop.path.separator@@");
        addRecipeCommand("deploy -f jboss5.tar -d \"@@rhq.system.sysprop.java.home@@\"");
        addRecipeCommand("deploy -f jboss6.tar -d @@rhq.system.sysprop.java.version@@");
        //addRecipeCommand("deploy -f jboss7.tar -d @@rhq.system.sysprop.user.timezone@@"); // sometimes this is empty
        //addRecipeCommand("deploy -f jboss8.tar -d @@rhq.system.sysprop.user.region@@"); // sometimes this doesn't exist
        addRecipeCommand("deploy -f jboss9.tar -d @@rhq.system.sysprop.user.country@@");
        addRecipeCommand("deploy -f jboss10.tar -d @@rhq.system.sysprop.user.language@@");
        addRecipeCommand("deploy -f jboss11.tar -d @@rhq.system.sysprop.custom.sysprop@@"); // non-standard sysprop
        RecipeParser parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.get("jboss1.tar").equals(System.getProperty("java.io.tmpdir").replace('\\', '/')) : files;
        assert files.get("jboss2.tar").equals(System.getProperty("file.separator").replace('\\', '/')) : files;
        //assert files.get("jboss3.tar").equals(System.getProperty("line.separator")) : files;
        assert files.get("jboss4.tar").equals(System.getProperty("path.separator")) : files;
        assert files.get("jboss5.tar").equals(System.getProperty("java.home").replace('\\', '/')) : files;
        assert files.get("jboss6.tar").equals(System.getProperty("java.version")) : files;
        //assert files.get("jboss7.tar").equals(System.getProperty("user.timezone")) : files;
        //assert files.get("jboss8.tar").equals(System.getProperty("user.region")) : files;
        assert files.get("jboss9.tar").equals(System.getProperty("user.country")) : files;
        assert files.get("jboss10.tar").equals(System.getProperty("user.language")) : files;
        assert files.get("jboss11.tar").equals("@@rhq.system.sysprop.custom.sysprop@@") : files;

        // now set a custom system property and see that it does NOT get replaced - we ignore custom sysprops!
        System.setProperty("custom.sysprop", "MY/CUSTOM/PROPERTY/HERE");
        parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        files = context.getDeployFiles();
        assert files.get("jboss11.tar").equals("@@rhq.system.sysprop.custom.sysprop@@") : files;
    }

    public void testSimpleRecipe() throws Exception {
        addRecipeCommand("deploy -f jboss.tar -d /opt/jboss");
        addRecipeCommand("deploy -f tomcat.tar -d /opt/tomcat");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("/opt/jboss") : files;
        assert files.containsKey("tomcat.tar") : files;
        assert files.get("tomcat.tar").equals("/opt/tomcat") : files;
    }

    public void testSimpleRecipeWithQuotes() throws Exception {
        addRecipeCommand("deploy -f jboss1.zip -d \"/opt/jboss1\"");
        addRecipeCommand("deploy -f jboss.tar --directory='/opt/jboss'");
        addRecipeCommand("deploy -f tomcat.tar \"--directory=/opt/tomcat\"");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss1.zip") : files;
        assert files.get("jboss1.zip").equals("/opt/jboss1") : files;
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("'/opt/jboss'") : files;
        assert files.containsKey("tomcat.tar") : files;
        assert files.get("tomcat.tar").equals("/opt/tomcat") : files;
    }

    public void testSimpleRecipeWithBackslashes() throws Exception {
        addRecipeCommand("deploy -f jboss1.zip -d \"C:\\opt\\jboss1\"");
        addRecipeCommand("deploy -f tomcat.tar \"--directory=C:\\Documents and Settings\\user\\\"");
        addRecipeCommand("deploy -f jboss2.zip -d C:\\opt\\jboss1");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss1.zip") : files;
        assert files.get("jboss1.zip").equals("C:/opt/jboss1") : files;
        assert files.containsKey("tomcat.tar") : files;
        assert files.get("tomcat.tar").equals("C:/Documents and Settings/user/") : files;
        assert files.containsKey("jboss2.zip") : files;
        assert files.get("jboss2.zip").equals("C:/opt/jboss1") : files;
    }

    public void testSimpleRecipeError() throws Exception {
        addRecipeCommand("deploy -f jboss.tar");
        RecipeParser parser = new RecipeParser();

        try {
            RecipeContext context = new RecipeContext(getRecipe());
            parser.parseRecipe(context);
            assert false : "This should have failed - need to provide a -d to the deploy command";
        } catch (Exception ok) {
            // to be expected
        }

        cleanRecipe();
        addRecipeCommand("deploy -d /opt/jboss");

        try {
            RecipeContext context = new RecipeContext(getRecipe());
            parser.parseRecipe(context);
            assert false : "This should have failed - need to provide a -f to the deploy command";
        } catch (Exception ok) {
            // to be expected
        }
    }

    public void testSimpleRecipeComments() throws Exception {
        addRecipeCommand("");
        addRecipeCommand("#");
        addRecipeCommand("### comment here");
        addRecipeCommand("deploy -f jboss.tar -d /opt/jboss");
        addRecipeCommand("");
        addRecipeCommand("#");
        addRecipeCommand("### comment here");
        addRecipeCommand("deploy -f tomcat.tar -d /opt/tomcat");
        addRecipeCommand("");
        addRecipeCommand("#");
        addRecipeCommand("### comment here");
        addRecipeCommand("");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("/opt/jboss") : files;
        assert files.containsKey("tomcat.tar") : files;
        assert files.get("tomcat.tar").equals("/opt/tomcat") : files;
    }

    private void cleanRecipe() {
        this.recipe = new StringBuilder();
    }

    private void addRecipeCommand(String commandLine) {
        this.recipe.append(commandLine).append("\n");
    }

    private String getRecipe() {
        return this.recipe.toString();
    }
}
