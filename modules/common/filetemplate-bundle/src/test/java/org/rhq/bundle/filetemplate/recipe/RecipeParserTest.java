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

@Test
public class RecipeParserTest {
    private StringBuilder recipe;

    @BeforeMethod
    public void cleanRecipeBeforeMethod() {
        cleanRecipe();
    }

    public void testConfigDefRecipe() throws Exception {
        addRecipeCommand("configdef -n custom.prop");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Set<String> vars = context.getReplacementVariables();
        assert vars.size() == 1 : vars;
        assert vars.contains("custom.prop");
    }

    public void testRealizeRecipe() throws Exception {
        addRecipeCommand("realize --file=<%opt.dir%>/config.ini");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Set<String> files = context.getRealizedFiles();
        assert files.size() == 1 : files;
        assert files.contains("<%opt.dir%>/config.ini") : files;
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
        addRecipeCommand("# <% ignored.inside.comment %>");
        addRecipeCommand("deploy -f jboss.tar -d \"<% opt.dir %>/jboss\"");
        addRecipeCommand("deploy -f tomcat.tar -d <%opt.dir%>/tomcat");
        addRecipeCommand("deploy -f jboss.zip -d <%rhq.system.hostname%>/opt/tomcat"); // this is ignored, its an agent fact variable
        RecipeParser parser = new RecipeParser();
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("<% opt.dir %>/jboss") : files;
        assert files.containsKey("tomcat.tar") : files;
        assert files.get("tomcat.tar").equals("<%opt.dir%>/tomcat") : files;
        assert files.containsKey("jboss.zip") : files;
        assert files.get("jboss.zip").equals("<%rhq.system.hostname%>/opt/tomcat") : files;
        assert context.getReplacementVariables().contains("opt.dir") : context.getReplacementVariables();
        assert context.getReplacementVariables().size() == 1 : context.getReplacementVariables();
    }

    public void testSimpleRecipeReplaceReplacementVariables() throws Exception {
        addRecipeCommand("deploy -f jboss.tar -d <% opt.dir %>/jboss");
        addRecipeCommand("deploy -f jboss2.tar -d <%unknown%>/jboss");
        addRecipeCommand("deploy -f jboss3.tar -d <% rhq.system.hostname %>/jboss");
        RecipeParser parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        RecipeContext context = new RecipeContext(getRecipe());
        context.addReplacementVariableValue("opt.dir", "/foo/bar");
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss.tar") : files;
        assert files.get("jboss.tar").equals("/foo/bar/jboss") : files;
        assert files.containsKey("jboss2.tar") : files;
        assert files.get("jboss2.tar").equals("<%unknown%>/jboss") : files;
        assert files.containsKey("jboss3.tar") : files;
        assert files.get("jboss3.tar").equals(SystemInfoFactory.createSystemInfo().getHostname() + "/jboss") : files;
    }

    public void testSimpleRecipeReplaceJavaSystemPropertyReplacementVariables() throws Exception {
        addRecipeCommand("deploy -f jboss1.tar -d <%java.io.tmpdir%>");
        addRecipeCommand("deploy -f jboss2.tar -d <%custom.sysprop%>");
        RecipeParser parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        RecipeContext context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        Map<String, String> files = context.getDeployFiles();
        assert files.containsKey("jboss1.tar") : files;
        assert files.get("jboss1.tar").equals(System.getProperty("java.io.tmpdir")) : files;
        assert files.containsKey("jboss2.tar") : files;
        assert files.get("jboss2.tar").equals("<%custom.sysprop%>") : files;

        // now set our custom system property and see that it gets replaced properly
        System.setProperty("custom.sysprop", "MY/CUSTOM/PROPERTY/HERE");
        parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        context = new RecipeContext(getRecipe());
        parser.parseRecipe(context);
        files = context.getDeployFiles();
        assert files.containsKey("jboss1.tar") : files;
        assert files.get("jboss1.tar").equals(System.getProperty("java.io.tmpdir")) : files;
        assert files.containsKey("jboss2.tar") : files;
        assert files.get("jboss2.tar").equals("MY/CUSTOM/PROPERTY/HERE") : files;
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
