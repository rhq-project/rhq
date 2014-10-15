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

package org.rhq.enterprise.server.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.core.AgentRegistrationException;
import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.core.clientapi.server.core.AgentVersion;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ServerFactory;

/**
 * This tests the core server service. This includes agent registration.
 *
 * @author John Mazzitelli
 */

// These are the agent registration unit test cases.
// (allowed) means the registration should succeed.
// (REJECT) means the server should reject that agent registration request.
// ---
// A. testNewAgentRegistrationWithOldToken
//   1) register a new agent with a non-null, unknown security token (allowed)
// B. testChangeAddressPort
//   1) register a new agent Z with null security token (allowed)
//   2) re-register agent Z with its token but change its host (allowed)
//   3) re-register agent Z with its token but change its port (allowed)
//   4) re-register agent Z with its token but change its host and port (allowed)
//   5) re-register agent Z with its token but change nothing (allowed)
//   6) re-register agent Z with NO token but change its host (REJECT)
//   7) re-register agent Z with NO token but change its port (REJECT)
//   8) re-register agent Z with NO token but change its host and port (REJECT)
//   9) re-register agent Z with NO token but change nothing (REJECT)
// C. testNormalAgentRegistration
//   1) register a new agent A with a null security token (allowed, same as B.1)
// D. testHijackExistingAgentAddressPort
//   1) register a new agent B with null security token but using A's host/port (REJECT)
// E. testHijackExistingAgentName
//   1) register an agent using an already-existing agent name A, and using A's host but a different port with a null token (REJECT - missing the  token)
//   2) register an agent using an already-existing agent name A, and using A's port but a different host with a null token (REJECT - missing the  token)
//   3) register an agent using an already-existing agent name A, and using a different port and host with a null token (REJECT - missing the  token)
// F. testHijackExistingAgentAddressPortWithBogusToken
//   1) register a new agent B with A's host and port but with a bogus token (REJECT)
// G. testHijackExistingAgentNameWithBogusToken
//   1) re-register agent A with its original host and port but with a bogus token (REJECT)
//   2) re-register agent A with its original host, different port but with bogus token (REJECT)
//   3) re-register agent A with different host, original port but with bogus token (REJECT)
//   4) re-register agent A with different host and port but with bogus token (REJECT)
// H. testHijackExistingAgentNameWithAnotherAgentToken
//   1) re-register agent A with its original host and port but with Z's security token (REJECT - you cannot authenticate using another agent's token)
//   2) re-register agent A with different host and original port but with Z's security token (REJECT - you cannot authenticate using another agent's token)
//   3) re-register agent A with original host and different port but with Z's security token (REJECT - you cannot authenticate using another agent's token)
//   4) re-register agent A with different host and port but with Z's security token (REJECT - you cannot  authenticate using another agent's token)
// I. testAgentHijackingAnotherAgentAddressPort
//   1) re-register agent A using A's correct security token but with Z's host and Z's port  (REJECT - one agent cannot steal another agent's host/port endpoint) NOTE: this is not D.1 because in D.1, the request doesn't have a token. This I.1 test has a token and it really authenticates the agent A making the request. This also isn't F.1 because F.1, while it has a token, it is not a valid token, thus its agent is not authentic.
// J. testAttemptToChangeAgentName
//   1) register agent "newName" but with Z's host/port/token. In effect, this is trying to change the agent's name. (REJECT - you are not allowed to rename agents)

@SuppressWarnings("unchecked")
@Test(groups = { "core.agent.registration" })
public class CoreServerServiceImplTest extends AbstractEJB3Test {
    private static final String TEST_AGENT_NAME_PREFIX = "CoreServerServiceImplTest.Agent";
    private AgentVersion agentVersion;
    private Server server;

    private static final int A_PORT = 11111;
    private static final String A_HOST = "hostA";
    private static final int B_PORT = 22222;
    private static final String B_HOST = "hostB";
    private static final String VERSION = "1.2.3";
    private static final String BUILD = "12345";
    private static final String AGENT_VERSION = "rhq-agent.latest.version";
    private static final String AGENT_BUILD = "rhq-agent.latest.build-number";
    private static final String AGENT_MD5 = "rhq-agent.latest.md5";
    //lives on the server/gui in agent/downloads folder
    private static final String SERVER_AGENT_PROPERTIES = "rhq-server-agent-versions.properties";
    //lives in the agent jar root
    private static final String AGENT_UPDATE_PROPERTIES = "rhq-agent-update-version.properties";
    private static final String DOWNLOADS_AGENT = "rhq-downloads/rhq-agent";

    // README
    // Arquillian (1.0.2) does not honor Testng's lifecycle, Before/AfterClass are invoked on
    // every test.  We use stand-in tests to simulate BeforeClass and AfterClass, using priorities
    // to make them run first and last.  Testng (I believe) applies priority after dependencies, so it
    // is important that afterClassStandIn() have a dependency such that it runs in the last test-set.

    @Test(priority = -10)
    public void beforeClassStandIn() throws Exception {
        // delete our instance var object files, if they exist
        deleteObjects("a.obj");
        deleteObjects("b.obj");
    }

    @Test(priority = 10, dependsOnMethods = "testNormalAgentRegistration", alwaysRun = true)
    public void afterClassStandIn() throws Exception {
        // delete our instance var object files
        deleteObjects("a.obj");
        deleteObjects("b.obj");

        // clean up any agents we might have created
        Query q = getEntityManager().createQuery(
            "select a from Agent a where name like '" + TEST_AGENT_NAME_PREFIX + "%'");
        List<Agent> doomed = q.getResultList();
        for (Agent deleteMe : doomed) {
            LookupUtil.getAgentManager().deleteAgent(deleteMe);
        }
    }

    @Override
    protected void beforeMethod() throws Exception {
        // mock the name of our server via the sysprop (in production, this is normally set in rhq-server.properties)
        setServerIdentity("CoreServerServiceImplTest.Server");

        // mock up our core server MBean that provides information about where the jboss home dir is
        DummyCoreServer mbean = new DummyCoreServer();
        prepareCustomServerService(mbean, CoreServerMBean.OBJECT_NAME);

        // in order to register, we need to mock out the agent version file used by the server
        // to determine the agent version it supports.
        agentVersion = new AgentVersion(VERSION, BUILD);
        File agentVersionFile = new File(mbean.getJBossServerDataDir(), DOWNLOADS_AGENT + "/" + SERVER_AGENT_PROPERTIES);
        agentVersionFile.getParentFile().mkdirs();
        agentVersionFile.delete();
        Properties agentVersionProps = new Properties();
        agentVersionProps.put(AGENT_VERSION, agentVersion.getVersion());
        agentVersionProps.put(AGENT_BUILD, agentVersion.getBuild());
        FileOutputStream fos = new FileOutputStream(agentVersionFile);
        try {
            agentVersionProps.store(fos, "This file was created by " + CoreServerServiceImplTest.class.getName());
        } finally {
            fos.close();
        }

        //#now build a fake agent.jar file as well for the tests.
        File testLocation = new File(getTempDir(), this.getClass().getSimpleName());
        File serverVersionFile = new File(testLocation, DOWNLOADS_AGENT + "/" + SERVER_AGENT_PROPERTIES);
        File agentPropertiesFile = new File(testLocation, DOWNLOADS_AGENT + "/" + AGENT_UPDATE_PROPERTIES);
        FileInputStream fin = new FileInputStream(serverVersionFile);
        FileOutputStream fout = new FileOutputStream(agentPropertiesFile);
        //build out the version for the agent jar
        StreamUtil.copy(fin, fout, true);

        //generate fake agent file.
        File agentBinaryFile = new File(testLocation, DOWNLOADS_AGENT + "/agent.jar");
        buildFakeAgentJar(agentVersionFile, agentBinaryFile);
        // make sure this is older than the version file
        agentBinaryFile.setLastModified(System.currentTimeMillis() - 600000);

        // this mocks out the endpoint ping - the server will think the agent that is registering is up and pingable
        prepareForTestAgents();

        // mock our server
        server = ServerFactory.newInstance();
        server.setName(getServerIdentity());
        server.setAddress("CoreServerServiceImplTest.localhost");
        server.setPort(12345);

        server.setSecurePort(12346);
        server.setOperationMode(OperationMode.NORMAL);
        int serverId = LookupUtil.getServerManager().create(server);
        server.setId(serverId);
    }

    @Override
    protected void afterMethod() throws Exception {

        // cleanup our test server
        LookupUtil.getTopologyManager().updateServerMode(LookupUtil.getSubjectManager().getOverlord(),
            new Integer[] { server.getId() }, OperationMode.DOWN);
        LookupUtil.getTopologyManager().deleteServer(LookupUtil.getSubjectManager().getOverlord(), server.getId());

        // shutdown our mock mbean server
        unprepareCustomServerService(CoreServerMBean.OBJECT_NAME);

        unprepareForTestAgents();
    }

    @Test
    public void testNewAgentRegistrationWithOldToken() throws Exception {
        // this tests the case where someone purged an agent from the DB, but then
        // changed their mind and want to re-run that agent and re-register it again.
        // In this case, the agent (if not using --cleanallconfig) would still have the old token.
        // The agent should still be allowed to register again.
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request = createRequest(prefixName("old"), "hostOld", 12345, "oldtoken");
        AgentRegistrationResults results = service.registerAgent(request);
        assert results != null : "cannot re-register an old agent";
        Agent agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(request.getName());
        assert agent.getAddress().equals(request.getAddress());
        assert agent.getPort() == request.getPort();
        LookupUtil.getAgentManager().deleteAgent(agent);
    }

    @Test
    public void testChangeAddressPort() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        AgentRegistrationResults results;

        String tuid = "" + new Random().nextInt();
        //let's make this unique per run to support repeated individual runs.
        String zName = prefixName("Z" + "-" + tuid);

        // create a new agent Z with host/port of hostZ/55550
        request = createRequest(zName, "hostZ", 55550, null);
        results = service.registerAgent(request);
        assert results != null : "got null results";

        // now change Z's host to hostZprime
        request = createRequest(zName, "hostZprime", 55550, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        Agent agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZprime");
        assert agent.getPort() == 55550;

        // now change Z's port to 55551
        request = createRequest(zName, "hostZprime", 55551, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZprime");
        assert agent.getPort() == 55551;

        // now change Z's host/port to hostZdoubleprime/55552
        request = createRequest(zName, "hostZdoubleprime" + tuid, 55552, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZdoubleprime" + tuid);
        assert agent.getPort() == 55552;

        // now don't change Z's host/port but re-register everything the same with its token
        request = createRequest(zName, "hostZdoubleprime" + tuid, 55552, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZdoubleprime" + tuid);
        assert agent.getPort() == 55552;

        // remember this agent so our later tests can use it
        AgentRegistrationRequest zReq = request;
        AgentRegistrationResults zResults = results;

        writeObjects("b.obj", zReq, zResults);

        // Try to re-register changes to host and/or port but do not send any token.
        // Because there is no token, these should fail.
        request = createRequest(zName, B_HOST, zReq.getPort(), null);
        try {
            service.registerAgent(request);
            assert false : "(1) Should not have been able to register without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(zName, zReq.getAddress(), B_PORT, null);
        try {
            service.registerAgent(request);
            assert false : "(2) Should not have been able to register without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(zName, B_HOST, B_PORT, null);
        try {
            service.registerAgent(request);
            assert false : "(3) Should not have been able to register without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(zName, zReq.getAddress(), zReq.getPort(), null);
        try {
            service.registerAgent(request);
            assert false : "(4) Should not have been able to register without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testChangeAddressPort")
    public void testNormalAgentRegistration() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest aReq = createRequest(prefixName("A"), A_HOST, A_PORT, null);
        AgentRegistrationResults aResults = service.registerAgent(aReq);
        assert aResults != null : "got null results";

        writeObjects("a.obj", aReq, aResults);
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentAddressPort() throws Exception {
        List<Object> objs = readObjects("a.obj", 1);
        AgentRegistrationRequest aReq = (AgentRegistrationRequest) objs.get(0);
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(prefixName("B"), aReq.getAddress(), aReq.getPort(), null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used host/port with new agent name";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentName() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;

        List<Object> objs = readObjects("a.obj", 1);
        AgentRegistrationRequest aReq = (AgentRegistrationRequest) objs.get(0);

        request = createRequest(aReq.getName(), aReq.getAddress(), B_PORT, null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), B_HOST, aReq.getPort(), null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), B_HOST, B_PORT, null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name without a token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentAddressPortWithBogusToken() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;

        List<Object> objs = readObjects("a.obj", 1);
        AgentRegistrationRequest aReq = (AgentRegistrationRequest) objs.get(0);

        request = createRequest(prefixName("B"), aReq.getAddress(), aReq.getPort(), "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used host/port with new agent name and invalid token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentNameWithBogusToken() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;

        List<Object> objs = readObjects("a.obj", 1);
        AgentRegistrationRequest aReq = (AgentRegistrationRequest) objs.get(0);

        request = createRequest(aReq.getName(), aReq.getAddress(), aReq.getPort(), "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }

        request = createRequest(aReq.getName(), aReq.getAddress(), B_PORT, "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), B_HOST, aReq.getPort(), "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), B_HOST, B_PORT, "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentNameWithAnotherAgentToken() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;

        List<Object> objs = readObjects("a.obj", 1);
        AgentRegistrationRequest aReq = (AgentRegistrationRequest) objs.get(0);

        objs = readObjects("b.obj", 2);
        @SuppressWarnings("unused")
        AgentRegistrationRequest zReq = (AgentRegistrationRequest) objs.get(0);
        AgentRegistrationResults zResults = (AgentRegistrationResults) objs.get(1);

        request = createRequest(aReq.getName(), aReq.getAddress(), aReq.getPort(), zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), B_HOST, aReq.getPort(), zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), aReq.getAddress(), B_PORT, zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
        request = createRequest(aReq.getName(), B_HOST, B_PORT, zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testAgentHijackingAnotherAgentAddressPort() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        List<Object> objs = readObjects("a.obj", 2);
        AgentRegistrationRequest aReq = (AgentRegistrationRequest) objs.get(0);
        AgentRegistrationResults aResults = (AgentRegistrationResults) objs.get(1);

        objs = readObjects("b.obj", 1);
        AgentRegistrationRequest zReq = (AgentRegistrationRequest) objs.get(0);

        request = createRequest(aReq.getName(), zReq.getAddress(), zReq.getPort(), aResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "An agent should not have been able to hijack another agent's host/port";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testAttemptToChangeAgentName() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;

        List<Object> objs = readObjects("b.obj", 2);
        AgentRegistrationRequest zReq = (AgentRegistrationRequest) objs.get(0);
        AgentRegistrationResults zResults = (AgentRegistrationResults) objs.get(1);

        request = createRequest(prefixName("newName"), zReq.getAddress(), zReq.getPort(), zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "An agent should not be able to change its name";
        } catch (AgentRegistrationException ok) {
            debugPrintThrowable(ok);
        }
    }

    /** Exercises the agentUpdateVersionFile mechanism.
     *  verify that one is created if none exists before.
     */
    @Test
    public void testAgentUpateVersionFile() {
        AgentManagerLocal agentManager = LookupUtil.getAgentManager();
        String AGENT_VERSION = CoreServerServiceImplTest.AGENT_VERSION;
        String AGENT_BUILD = CoreServerServiceImplTest.AGENT_BUILD;
        String RHQ_AGENT_LATEST_MD5 = CoreServerServiceImplTest.AGENT_MD5;
        String version = VERSION;
        String build = BUILD;

        try {
            File updateFile = agentManager.getAgentUpdateVersionFile();
            assert updateFile != null : "GetAgentUpdateVersionFile returned null.";
            Properties props = new Properties();
            FileInputStream inStream = new FileInputStream(updateFile);
            try {
                props.load(inStream);
            } finally {
                inStream.close();
            }
            //check that properties present
            boolean locatedAgentVersion = false;
            boolean locatedAgentBuild = false;
            for (Object property : props.keySet()) {
                if (property.toString().equals(AGENT_VERSION)) {
                    locatedAgentVersion = true;
                } else if (property.toString().equals(AGENT_BUILD)) {
                    locatedAgentBuild = true;
                }
            }
            assert locatedAgentVersion : AGENT_VERSION + " was not found.";
            assert locatedAgentBuild : AGENT_BUILD + " was not found.";

            //Now delete the file and test that it's recreated properly
            File testLocation = new File(getTempDir(), "CoreServerServiceImplTest");
            File serverVersionFile = new File(testLocation,
 DOWNLOADS_AGENT + "/" + SERVER_AGENT_PROPERTIES);
            File agentVersionFile = new File(testLocation,
 DOWNLOADS_AGENT + "/" + AGENT_UPDATE_PROPERTIES);
            FileInputStream fin = new FileInputStream(serverVersionFile);
            FileOutputStream fout = new FileOutputStream(agentVersionFile);
            StreamUtil.copy(fin, fout, true);
            serverVersionFile.delete();
            assert !serverVersionFile.exists() : "The default test file location still exists. Unable to proceed.";
            assert agentVersionFile.exists() : "The agent properties file was not created. Unable to proceed.";

            //update the mocked components necessary for regeneration
            DummyCoreServerTweaked mbean = new DummyCoreServerTweaked(version, build);
            prepareCustomServerService(mbean, CoreServerMBean.OBJECT_NAME);

            //generate fake agent file.
            File agentBinaryFile = new File(testLocation, DOWNLOADS_AGENT + "/agent.jar");
            assert agentBinaryFile.exists() : "A fake agent binary file should already exist.";
            assert agentBinaryFile.delete() : "Unable to delete default binary file.";
            buildFakeAgentJar(agentVersionFile, agentBinaryFile);
            assert agentBinaryFile.exists() : "Failed to build fake agent file:" + agentBinaryFile.getCanonicalPath();

            //trigger the file regeneration
            updateFile = agentManager.getAgentUpdateVersionFile();

            //check values
            props = new Properties();
            inStream = new FileInputStream(updateFile);
            try {
                props.load(inStream);
            } finally {
                inStream.close();
            }

            locatedAgentVersion = false;
            locatedAgentBuild = false;
            for (Object property : props.keySet()) {
                if (property.toString().equals(AGENT_VERSION)) {
                    locatedAgentVersion = true;
                } else if (property.toString().equals(AGENT_BUILD)) {
                    locatedAgentBuild = true;
                }
            }
            //Verify regenerated bits. It's more than we deliver with a release
            assert locatedAgentVersion : AGENT_VERSION + " was not found.";
            assert props.getProperty(AGENT_VERSION).equals(version) : "Version field did not match. Expected '"
                + version + "' but got '" + props.getProperty(AGENT_VERSION) + "'.";
            assert locatedAgentBuild : AGENT_BUILD + " was not found.";
            assert props.getProperty(AGENT_BUILD).equals(build) : "Version field did not match. Expected '" + version
                + "' but got '" + props.getProperty(AGENT_BUILD) + "'.";
            assert props.getProperty(RHQ_AGENT_LATEST_MD5) != null : "MD5 value not located.";
            assert props.getProperty(RHQ_AGENT_LATEST_MD5).trim().length() > 0 : "No checksum value was located.";

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLinkAgentWithAgentInstall() throws Exception {
        AgentManagerLocal agentManager = LookupUtil.getAgentManager();
        SubjectManagerLocal sm = LookupUtil.getSubjectManager();

        AgentInstall persistedAgentInstall = agentManager.getAgentInstallByAgentName(sm.getOverlord(),
            "should-not-exist");
        assert persistedAgentInstall == null;

        AgentInstall agentInstall = new AgentInstall();
        agentInstall.setSshHost("CoreServerServiceImpl-SshHost");
        agentInstall.setSshPort(44);
        agentInstall.setSshUsername("CoreServerServiceImpl-SshUsername");
        agentInstall.setSshPassword("CoreServerServiceImpl-SshPassword");
        agentInstall = agentManager.updateAgentInstall(sm.getOverlord(), agentInstall);
        assert agentInstall.getId() > 0 : "didn't persist properly - ID should be non-zero";
        assert agentInstall.getAgentName() == null : "there should be no agent name yet";
        assert agentInstall.getInstallLocation() == null : "there should be no install location yet";

        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest aReq = createRequest(prefixName(".AgentInstall"), A_HOST, A_PORT, null,
            String.valueOf(agentInstall.getId()), "/tmp/CoreServerServiceImplTest/rhq-agent");
        AgentRegistrationResults aResults = service.registerAgent(aReq);
        assert aResults != null : "got null results";

        persistedAgentInstall = agentManager.getAgentInstallByAgentName(sm.getOverlord(), aReq.getName());
        assert persistedAgentInstall != null : "the new agent info is missing";
        assert persistedAgentInstall.getAgentName().equals(aReq.getName());
        assert persistedAgentInstall.getInstallLocation().equals("/tmp/CoreServerServiceImplTest/rhq-agent");
        assert persistedAgentInstall.getSshHost().equals("CoreServerServiceImpl-SshHost");
        assert persistedAgentInstall.getSshPort().equals(44);
        assert persistedAgentInstall.getSshUsername().equals("CoreServerServiceImpl-SshUsername");
        assert persistedAgentInstall.getSshPassword().equals("CoreServerServiceImpl-SshPassword");

        Agent doomed = agentManager.getAgentByName(aReq.getName());
        agentManager.deleteAgent(doomed);
        persistedAgentInstall = agentManager.getAgentInstallByAgentName(sm.getOverlord(), aReq.getName());
        assert persistedAgentInstall == null : "the agent info should have been deleted";
    }

    private void buildFakeAgentJar(File binaryContents, File agentBinaryFile) throws FileNotFoundException, IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "54321");
        JarOutputStream target = new JarOutputStream(new FileOutputStream(agentBinaryFile), manifest);

        //include the file passed in as contents of the jar.
        BufferedInputStream in = null;
        try {
            JarEntry entry = new JarEntry(binaryContents.getName());

            entry.setTime(binaryContents.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(binaryContents));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (in != null)
                in.close();
        }
        target.close();
    }

    private class DummyCoreServerTweaked extends DummyCoreServer {
        private String version;
        private String build;

        @Override
        public String getVersion() {
            return this.version;
        }

        @Override
        public String getBuildNumber() {
            return this.build;
        }

        public DummyCoreServerTweaked(String version, String build) {
            this.version = version;
            this.build = build;
        }
    }

    private AgentRegistrationRequest createRequest(String name, String address, int port, String token) {
        return createRequest(name, address, port, token, "12345", "/tmp/CoreServerServiceImplTest/rhq-agent");
    }

    private AgentRegistrationRequest createRequest(String name, String address, int port, String token,
        String installId, String installLocation) {
        return new AgentRegistrationRequest(name, address, port, "socket://" + address + ":" + port
            + "/?rhq.communications.connector.rhqtype=agent", true, token, agentVersion, installId, installLocation);
    }

    private String prefixName(String name) {
        return TEST_AGENT_NAME_PREFIX + name;
    }

    private void debugPrintThrowable(Throwable t) {
        if (true) {
            System.out.println(ThrowableUtil.getAllMessages(t));
        }
    }

    interface DummyCoreServerMBean extends CoreServerMBean {
    };

    class DummyCoreServer implements DummyCoreServerMBean {

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public String getBuildNumber() {
            return null;
        }

        @Override
        public Date getBootTime() {
            return null;
        }

        @Override
        public File getInstallDir() {
            return null;
        }

        @Override
        public File getJBossServerHomeDir() {
            return new File(getTempDir(), "CoreServerServiceImplTest");
        }

        @Override
        public File getJBossServerDataDir() {
            return new File(getTempDir(), "CoreServerServiceImplTest");
        }

        @Override
        public File getJBossServerTempDir() {
            return new File(getTempDir(), "CoreServerServiceImplTest");
        }

        @Override
        public File getEarDeploymentDir() {
            return new File(getTempDir(), "CoreServerServiceImplTest");
        }

        @Override
        public ProductInfo getProductInfo() {
            return null;
        }
    }
}
