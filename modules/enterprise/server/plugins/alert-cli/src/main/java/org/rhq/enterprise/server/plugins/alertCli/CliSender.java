/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.alertCli;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Uses CLI to perform the alert notification.
 *
 * @author Lukas Krejci
 */
public class CliSender extends AlertSender<ServerPluginComponent> {

    public static final String PROP_PACKAGE_ID = "packageId";
    public static final String PROP_REPO_ID = "repoId";
    public static final String PROP_USER_ID = "userId";

    private static final Log LOG = LogFactory.getLog(CliSender.class);

    private static final String SUMMARY_TEMPLATE = "Ran script $packageName in version $packageVersion from repo $repoName as user $userName.";
    private static final String PREVIEW_TEMPLATE = "Run script $packageName from repo $repoName as user $userName.";

    /**
     * Simple strongly typed representation of the alert configuration
     */
    private static class Config {
        Subject subject;
        int packageId;
        int repoId;
    }
    
    public SenderResult send(Alert alert) {
        SenderResult result = new SenderResult();
        BufferedReader reader = null;
        try {
            Config config = getConfig();
            result.setSummary(createSummary(config, SUMMARY_TEMPLATE));

            ByteArrayOutputStream scriptOutputStream = new ByteArrayOutputStream();

            ScriptEngine engine = getScriptEngine(alert, scriptOutputStream, config);

            InputStream packageBits = getPackageBits(config.packageId, config.repoId);

            reader = new BufferedReader(new InputStreamReader(packageBits));

            engine.eval(reader);

            String scriptOutput = scriptOutputStream.toString(Charset.defaultCharset().name());

            if (scriptOutput.length() == 0) {
                scriptOutput = "Script generated no output.";
            }
            
            result.addSuccessMessage(scriptOutput);

            return result;
        } catch (IllegalArgumentException e) {
            return SenderResult.getSimpleFailure(e.getMessage());
        } catch (Exception e) {
            result.addFailureMessage(ThrowableUtil.getAllMessages(e));
            return result;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error("Failed to close the script reader.", e);
                }
            }
        }
    }

    @Override
    public String previewConfiguration() {
        try {
            Config c = getConfig();
            return createSummary(c, PREVIEW_TEMPLATE);
        } catch (Exception e) {
            LOG.warn("Failed to get the configuration preview.", e);
            return "Failed to get configuration preview: " + e.getMessage();
        }
    }

    private static ScriptEngine getScriptEngine(Alert alert, OutputStream scriptOutput, Config config) throws ScriptException,
        IOException {
        Subject user = config.subject;

        LocalClient client = new LocalClient(user);

        PrintWriter output = new PrintWriter(scriptOutput);
        StandardBindings bindings = new StandardBindings(output, client);
        bindings.put("alert", alert);

        return ScriptEngineFactory.getScriptEngine("JavaScript", new PackageFinder(Collections.<File> emptyList()),
            bindings);
    }

    private static InputStream getPackageBits(int packageId, int repoId) throws IOException {
        final ContentSourceManagerLocal csm = LookupUtil.getContentSourceManager();
        RepoManagerLocal rm = LookupUtil.getRepoManagerLocal();
        final PackageVersion versionToUse = rm.getLatestPackageVersion(LookupUtil.getSubjectManager().getOverlord(), packageId, repoId, null);

        PipedInputStream ret = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(ret);

        Thread reader = new Thread(new Runnable() {
            public void run() {
                try {
                    csm.outputPackageVersionBits(versionToUse, out);
                } catch (RuntimeException e) {
                    LOG.warn("The thread for reading the bits of package version [" + versionToUse
                        + "] failed with exception.", e);
                    throw e;
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        //doesn't happen in piped output stream
                        LOG.error(
                            "Failed to close the piped output stream receiving the package bits of package version "
                                + versionToUse + ". This should never happen.", e);
                    }
                }
            }
        });
        reader.setName("CLI Alert download thread for package version " + versionToUse);
        reader.setDaemon(true);
        reader.start();

        return ret;
    }

    /**
     * Possible replacements are:
     * <ul>
     * <li><code>$userName</code>
     * <li><code>$packageName</code>
     * <li><code>$packageVersion</code>
     * <li><code>$repoName</code>
     * </ul>
     * @param config
     * @param template
     * @return
     */
    private static String createSummary(Config config, String template) {
        try {
            String ret = template;

            ret = ret.replace("$userName", config.subject.getName());

            //now get the package and repo info
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            RepoManagerLocal rm = LookupUtil.getRepoManagerLocal();
            PackageVersion versionToUse = rm.getLatestPackageVersion(overlord, config.packageId, config.repoId);

            ret = ret.replace("$packageName", versionToUse.getDisplayName());
            ret = ret.replace("$pacakgeVersion", versionToUse.getDisplayVersion() == null ? versionToUse.getVersion()
                : versionToUse.getDisplayVersion());

            RepoCriteria criteria = new RepoCriteria();
            criteria.addFilterId(config.repoId);
            
            List<Repo> repos = rm.findReposByCriteria(overlord, criteria);
            
            String repoName;
            
            if (repos.size() > 0) {
                repoName = repos.get(0).getName();
            } else {
                repoName = "unknown repo with id " + config.repoId;
            }
            
            ret = ret.replace("$repoName", repoName);
            
            return ret;
        } catch (Exception e) {
            LOG.info("Failed to create alert sender summary.", e);
            return "Failed to create summary: " + e.getMessage();
        }
    }

    private Config getConfig() throws IllegalArgumentException {
        Config ret = new Config();

        int subjectId = getIntFromConfiguration(PROP_USER_ID, "User id not specified.", "Failed to read subject id property: ");
        int packageId = getIntFromConfiguration(PROP_PACKAGE_ID, "Package id of the script not specified.", "Failed to read the package id property: ");
        int repoId = getIntFromConfiguration(PROP_REPO_ID, "Repo to download the script package from not specified.", "Failed to read the repo id property: ");
        
        Subject subject = LookupUtil.getSubjectManager().getSubjectById(subjectId);

        if (subject == null) {
            throw new IllegalArgumentException("User with id " + subjectId + " doesn't exist anymore.");
        }

        ret.subject = subject;
        ret.packageId = packageId;
        ret.repoId = repoId;
        
        return ret;
    }
    
    private int getIntFromConfiguration(String propName, String errorMessage, String convertErrorMessage) throws IllegalArgumentException {
        PropertySimple prop = alertParameters.getSimple(propName);
        
        if (prop == null) {
            throw new IllegalArgumentException(errorMessage);                       
        }
        
        try {
            return prop.getIntegerValue();
        } catch (Exception e) {
            throw new IllegalArgumentException(convertErrorMessage + e.getMessage(), e);
        }
    }
}
