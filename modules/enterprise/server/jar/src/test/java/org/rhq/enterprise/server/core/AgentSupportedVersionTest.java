package org.rhq.enterprise.server.core;

import java.util.Properties;
import java.util.Random;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.core.AgentVersion;

/**
 * Test that doesn't require any EE infrastructure - just checking to make sure
 * the agent version check works.
 *
 * @author John Mazzitelli
 */
@Test
public class AgentSupportedVersionTest {

    private String agentLatestVersion;
    private String supportedVersionsRegex;

    // our agent manager to use to run the check - its the real AgentManagerBean that doesn't require any EE infrastructure for this test
    private AgentManagerBean agentManager = new AgentManagerStub();

    public void testLatestAgentVersionCheck() {
        AgentVersion agentVersionInfo = setLatestAgentVersionToCheck("1.0.GA", "1.0.GA");
        assert true == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
        agentVersionInfo = setLatestAgentVersionToCheck("1.0.GA", "1.0.RC1");
        assert false == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
        agentVersionInfo = setLatestAgentVersionToCheck("1.0.GA", "2.0.GA");
        assert false == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
    }

    public void testSupportedVersionsCheck() {
        String regex = "1.0.GA"; // no regex, just a simple string equality test
        checkOK(regex, "1.0.GA");
        checkFail(regex, "1.0.RC1");
        checkFail(regex, "2.0.GA");

        // regex test #1
        regex = "3.2.0.(GA|CP[12345])";
        checkOK(regex, "3.2.0.GA");
        checkOK(regex, "3.2.0.CP1");
        checkOK(regex, "3.2.0.CP5");
        checkFail(regex, "3.2.1.GA");
        checkFail(regex, "3.3.0.CP1");

        // regex test #2
        regex = "1.[01234].(RC1|RC2|GA)";
        checkOK(regex, "1.0.RC1");
        checkOK(regex, "1.1.RC2");
        checkOK(regex, "1.4.GA");
        checkFail(regex, "1.5.GA");

        // regex test #3 - combines #1 and #2
        regex = "(3.2.0.(GA|CP[12345]))|(1.[01234].(RC1|RC2|GA))";
        checkOK(regex, "3.2.0.GA");
        checkOK(regex, "3.2.0.CP1");
        checkOK(regex, "3.2.0.CP5");
        checkFail(regex, "3.2.1.GA");
        checkFail(regex, "3.3.0.CP1");
        checkOK(regex, "1.0.RC1");
        checkOK(regex, "1.1.RC2");
        checkOK(regex, "1.4.GA");
        checkFail(regex, "1.5.GA");
    }

    private void checkOK(String supportedVersions, String agentVersionToCheck) {
        check(supportedVersions, agentVersionToCheck, true);
    }

    private void checkFail(String supportedVersions, String agentVersionToCheck) {
        check(supportedVersions, agentVersionToCheck, false);
    }

    private void check(String supportedVersions, String agentVersionToCheck, boolean expectedResult) {
        AgentVersion agentVersionInfo = setSupportedVersionsToCheck(supportedVersions, agentVersionToCheck);
        assert expectedResult == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported() : "supportedVersions="
            + supportedVersions + "; agentVersionToCheck=" + agentVersionToCheck;
    }

    private AgentVersion setSupportedVersionsToCheck(String supportedVersionsRegex, String agentVersionToCheck) {
        this.supportedVersionsRegex = supportedVersionsRegex;
        this.agentLatestVersion = "0.0.1.irrelevant-version-when-we-have-supported-versions-regex";

        // We don't check build numbers during version check anymore - but just to make sure, always give a different value.
        // This will show that we don't fail no matter what the build number is.
        String buildNumber = String.valueOf(new Random().nextInt());
        return new AgentVersion(agentVersionToCheck, buildNumber);
    }

    private AgentVersion setLatestAgentVersionToCheck(String agentLatestVersion, String agentVersionToCheck) {
        this.supportedVersionsRegex = null;
        this.agentLatestVersion = agentLatestVersion;

        // We don't check build numbers during version check anymore - but just to make sure, always give a different value.
        // This will show that we don't fail no matter what the build number is.
        String buildNumber = String.valueOf(new Random().nextInt());
        return new AgentVersion(agentVersionToCheck, buildNumber);
    }

    private class AgentManagerStub extends AgentManagerBean {
        @Override
        public Properties getAgentUpdateVersionFileContent() {
            // these are private constants in the subclass, so we don't have access to them - just redefine them here
            final String RHQ_AGENT_LATEST_VERSION = "rhq-agent.latest.version";
            final String RHQ_AGENT_SUPPORTED_VERSIONS = "rhq-agent.supported.versions";

            Properties p = new Properties();
            p.put(RHQ_AGENT_LATEST_VERSION, AgentSupportedVersionTest.this.agentLatestVersion);

            // this is optional, a system does not have to have this set
            if (AgentSupportedVersionTest.this.supportedVersionsRegex != null) {
                p.put(RHQ_AGENT_SUPPORTED_VERSIONS, AgentSupportedVersionTest.this.supportedVersionsRegex);
            }
            return p;
        }
    }
}
