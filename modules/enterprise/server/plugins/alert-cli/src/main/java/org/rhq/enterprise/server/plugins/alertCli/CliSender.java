/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.plugins.alertCli;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.StandardScriptPermissions;
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
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderValidationResults;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.scripting.ScriptEngineInitializer;
import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.ScriptSourceProviderFactory;

/**
 * Uses CLI to perform the alert notification.
 *
 * @author Lukas Krejci
 */
public class CliSender extends AlertSender<CliComponent> {
    private static final Log LOG = LogFactory.getLog(CliSender.class);

    private static final int MAX_RESULT_SIZE = 4000;

    public static final String PROP_PACKAGE_ID = "packageId";
    public static final String PROP_REPO_ID = "repoId";
    public static final String PROP_USER_ID = "userId";
    public static final String PROP_USER_NAME = "userName";
    public static final String PROP_USER_PASSWORD = "userPassword";

    private static final String SUMMARY_TEMPLATE = "Ran script $packageName in version $packageVersion from repo $repoName as user $userName.";
    private static final String PREVIEW_TEMPLATE = "Run script $packageName from repo $repoName as user $userName.";

    private static final String VALIDATION_ERROR_MESSAGE = "The provided user failed to authenticate.";

    //no more than 10 concurrently running CLI notifications..
    //is that enough?
    private static final int MAX_SCRIPT_ENGINES = 10;
    private static final Map<String, Queue<ScriptEngine>> SCRIPT_ENGINES = new HashMap<String, Queue<ScriptEngine>>();
    private static int ENGINES_IN_USE = 0;

    /**
     * Simple strongly typed representation of the alert configuration
     */
    private static class Config {
        Subject subject;
        int packageId;
        int repoId;
    }

    private static class ExceptionHolder {
        public ScriptException scriptException;
        public Throwable throwable;
    }

    private static class BitsAndFileExtension {
        InputStream packageBits;
        String scriptFileExtension;
    }

    @Override
    public SenderResult send(Alert alert) {
        SenderResult result = new SenderResult();
        BufferedReader reader = null;
        ScriptEngine engine = null;
        Subject subjectWithSession = null;
        final SessionManager sessionManager = SessionManager.getInstance();
        String language = null;

        try {
            final Config config = getConfig();

            // simulate the login by getting a session ID
            config.subject = sessionManager.put(config.subject, pluginComponent.getScriptTimeout() * 1000);
            subjectWithSession = config.subject;

            result.setSummary(createSummary(config, SUMMARY_TEMPLATE));

            ByteArrayOutputStream scriptOutputStream = new ByteArrayOutputStream();
            PrintWriter scriptOut = new PrintWriter(scriptOutputStream);

            BitsAndFileExtension packageData = getPackageBits(config.packageId, config.repoId);

            InputStream packageBits = packageData.packageBits;
            String scriptFileExtension = packageData.scriptFileExtension;
            language = ScriptEngineFactory.getLanguageByScriptFileExtension(scriptFileExtension);

            if (language == null) {
                ArrayList<String> supportedExtensions = new ArrayList<String>();
                for (String lang : ScriptEngineFactory.getSupportedLanguages()) {
                    supportedExtensions.add(ScriptEngineFactory.getFileExtensionForLanguage(lang));
                }

                throw new IllegalArgumentException(
                    "Could not determine the script engine to use based on the script file extension '"
                        + scriptFileExtension + "'. Only the following extensions are currently supported: "
                        + supportedExtensions);
            }

            engine = getScriptEngine(alert, scriptOut, config, language, pluginComponent.getDomainPackagesNames());

            reader = new BufferedReader(new InputStreamReader(packageBits));

            final BufferedReader rdr = reader;

            final ExceptionHolder exceptionHolder = new ExceptionHolder();

            final ScriptEngine e = engine;
            Thread scriptRunner = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        e.eval(rdr);
                    } catch (ScriptException e) {
                        exceptionHolder.scriptException = e;
                    } catch (Throwable e) {
                        exceptionHolder.throwable = e;
                    }
                }
            }, "Script Runner for alert " + alert);
            scriptRunner.setDaemon(true);
            scriptRunner.start();

            if (pluginComponent.getScriptTimeout() <= 0) {
                scriptRunner.join();
            } else {
                scriptRunner.join(pluginComponent.getScriptTimeout() * 1000);
            }

            scriptRunner.interrupt();

            if (exceptionHolder.scriptException != null) {
                LOG.info("The script execution for CLI notification of alert [" + alert + "] failed.",
                    exceptionHolder.scriptException);

                //make things pretty for the UI
                ScriptEngineInitializer initializer = ScriptEngineFactory.getInitializer(language);
                String message = initializer.extractUserFriendlyErrorMessage(exceptionHolder.scriptException);
                int col = exceptionHolder.scriptException.getColumnNumber();
                int line = exceptionHolder.scriptException.getLineNumber();
                String scriptName = createSummary(config, "script $packageName ($packageVersion) in repo $repoName");
                throw new ScriptException(message, scriptName, line, col);
            } else if (exceptionHolder.throwable != null) {
                LOG.info("The script execution for CLI notification of alert [" + alert + "] failed.",
                    exceptionHolder.throwable);

                throw exceptionHolder.throwable;
            }

            scriptOut.flush();
            String scriptOutput = scriptOutputStream.toString(Charset.defaultCharset().name());

            if (scriptOutput.length() == 0) {
                scriptOutput = "Script generated no output.";
            }

            if (scriptOutput.length() > remainingResultSize(result)) {
                scriptOutput = scriptOutput.substring(0, remainingResultSize(result));
            }

            result.addSuccessMessage(scriptOutput);

            return result;
        } catch (IllegalArgumentException e) {
            return SenderResult.getSimpleFailure(e.getMessage()); //well, let's just hope the message doesn't exceed 4k.
        } catch (Throwable e) {
            result.addFailureMessage(ThrowableUtil.getAllMessages(e, true, remainingResultSize(result)));
            return result;
        } finally {
            if (subjectWithSession != null) {
                sessionManager.invalidate(subjectWithSession.getSessionId());
            }
            if (engine != null) {
                returnEngine(engine, language);
            }

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

    @Override
    public AlertSenderValidationResults validateAndFinalizeConfiguration(Subject subject) {
        AlertSenderValidationResults results = new AlertSenderValidationResults(alertParameters, extraParameters);

        String userIdString = alertParameters.getSimpleValue(PROP_USER_ID, null);
        String userName = alertParameters.getSimpleValue(PROP_USER_NAME, null);
        String userPassword = alertParameters.getSimpleValue(PROP_USER_PASSWORD, null);

        Integer userId = userIdString == null ? null : Integer.valueOf(userIdString);

        if (userId == null || userId != subject.getId()) {
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

            Subject authSubject = subjectManager.checkAuthentication(userName, userPassword);

            if (authSubject == null) {
                PropertySimple userNameProp = new PropertySimple(PROP_USER_NAME, userName);
                userNameProp.setErrorMessage(VALIDATION_ERROR_MESSAGE);
                alertParameters.put(userNameProp);
                alertParameters.put(new PropertySimple(PROP_USER_ID, null));
            } else {
                //make sure we store the id of the user that actually authenticated to prevent
                //security breaches.
                alertParameters.put(new PropertySimple(PROP_USER_ID, authSubject.getId()));
            }
        } else {
            //make sure to store the username of the user... not that it is functionally
            //required but prevent confusions in case of debugging some errorneous situation
            alertParameters.put(new PropertySimple(PROP_USER_NAME, subject.getName()));
        }

        //do not store the password in the database ever
        alertParameters.put(new PropertySimple(PROP_USER_PASSWORD, null));

        return results;
    }

    private static ScriptEngine getScriptEngine(Alert alert, PrintWriter output, Config config, String language,
        Set<String> domainPackagesNames) throws ScriptException, IOException, InterruptedException {
        Subject user = config.subject;

        LocalClient client = new LocalClient(user);

        StandardBindings bindings = new StandardBindings(output, client);
        bindings.put("alert", alert);

        ScriptEngine engine = takeEngine(bindings, language, domainPackagesNames);
        engine.getContext().setWriter(output);
        engine.getContext().setErrorWriter(output);

        return engine;
    }

    private static BitsAndFileExtension getPackageBits(int packageId, int repoId) throws IOException {
        final ContentSourceManagerLocal csm = LookupUtil.getContentSourceManager();
        RepoManagerLocal rm = LookupUtil.getRepoManagerLocal();
        final PackageVersion versionToUse = rm.getLatestPackageVersion(LookupUtil.getSubjectManager().getOverlord(),
            packageId, repoId);

        if (versionToUse == null) {
            throw new IllegalArgumentException(
                "The package with id "
                    + packageId
                    + " either doesn't exist at all or doesn't have any version. Can't execute a CLI script without a script to run.");
        }

        PipedInputStream bits = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(bits);

        Thread reader = new Thread(new Runnable() {
            @Override
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

        BitsAndFileExtension ret = new BitsAndFileExtension();
        ret.packageBits = bits;
        String fileName = versionToUse.getFileName();
        String extension = "";

        if (fileName != null) {
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx >= 0) {
                extension = fileName.substring(dotIdx + 1);
            }
        }

        ret.scriptFileExtension = extension;

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

            if (versionToUse != null) {
                ret = ret.replace("$packageName", versionToUse.getDisplayName());
                ret = ret.replace(
                    "$packageVersion",
                    versionToUse.getDisplayVersion() == null ? versionToUse.getVersion() : versionToUse
                        .getDisplayVersion());
            } else {
                ret = ret.replace("$packageName", "unknown script with package id " + config.packageId);
                ret = ret.replace("$packageVersion", "no version");
            }

            RepoCriteria criteria = new RepoCriteria();
            criteria.addFilterId(config.repoId);
            criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

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

        int subjectId = getIntFromConfiguration(PROP_USER_ID, "User id not specified.",
            "Failed to read subject id property: ");
        int packageId = getIntFromConfiguration(PROP_PACKAGE_ID, "Package id of the script not specified.",
            "Failed to read the package id property: ");
        int repoId = getIntFromConfiguration(PROP_REPO_ID, "Repo to download the script package from not specified.",
            "Failed to read the repo id property: ");

        Subject subject = LookupUtil.getSubjectManager().getSubjectById(subjectId);

        if (subject == null) {
            throw new IllegalArgumentException("User with id " + subjectId + " doesn't exist anymore.");
        }

        ret.subject = subject;
        ret.packageId = packageId;
        ret.repoId = repoId;

        return ret;
    }

    private int getIntFromConfiguration(String propName, String errorMessage, String convertErrorMessage)
        throws IllegalArgumentException {

        PropertySimple prop = alertParameters.getSimple(propName);
        if (prop == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        Integer integerValue;
        try {
            integerValue = prop.getIntegerValue();
        } catch (Exception e) {
            throw new IllegalArgumentException(convertErrorMessage + e.getMessage(), e);
        }

        if (integerValue == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return integerValue;
    }

    private static ScriptEngine takeEngine(StandardBindings bindings, String language, Set<String> domainPackagesNames)
        throws InterruptedException, ScriptException, IOException {
        synchronized (SCRIPT_ENGINES) {
            if (ENGINES_IN_USE >= MAX_SCRIPT_ENGINES) {
                SCRIPT_ENGINES.wait();
            }

            Queue<ScriptEngine> q = SCRIPT_ENGINES.get(language);
            if (q == null) {
                q = new ArrayDeque<ScriptEngine>();
                SCRIPT_ENGINES.put(language, q);
            }

            ScriptEngine engine = q.poll();

            if (engine == null) {
                engine = ScriptEngineFactory.getSecuredScriptEngine(language, new CliSenderPackageFinder(
                    domainPackagesNames), bindings, new StandardScriptPermissions());
            }
            //TODO is this OK, or should we use a different classloader than the context classloader?
            ScriptSourceProvider[] providers = ScriptSourceProviderFactory.get(null);
            ScriptEngineFactory.injectStandardBindings(engine, bindings, true, providers);

            ++ENGINES_IN_USE;

            return engine;
        }
    }

    private static void returnEngine(ScriptEngine engine, String language) {
        synchronized (SCRIPT_ENGINES) {
            Queue<ScriptEngine> q = SCRIPT_ENGINES.get(language);
            if (q == null) {
                //hmm... this is very strange and should not happen, because we should have initied the queue in the 
                //takeEngine() method...
                q = new ArrayDeque<ScriptEngine>();
                SCRIPT_ENGINES.put(language, q);
            }
            q.offer(engine);
            --ENGINES_IN_USE;
            SCRIPT_ENGINES.notify();
        }
    }

    private static int remainingResultSize(SenderResult r) {
        //the "10" is a ballpark to allow for some formatting
        //done by the receivers of the SenderResult.
        int ret = MAX_RESULT_SIZE - r.getSummary().length() - 10;

        for (String m : r.getSuccessMessages()) {
            ret -= m.length() + 10;
        }

        for (String m : r.getFailureMessages()) {
            ret -= m.length() + 10;
        }

        return ret;
    }

    private static class CliSenderPackageFinder extends PackageFinder {
        final Set<String> domainPackagesNames;

        CliSenderPackageFinder(Set<String> domainPackagesNames) {
            super(Collections.<File> emptyList());
            this.domainPackagesNames = domainPackagesNames;
        }

        @Override
        public Set<String> findPackages(String packageRoot) throws IOException {
            return domainPackagesNames;
        }
    }
}
