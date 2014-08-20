package org.rhq.enterprise.server.core;

import java.util.Properties;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.core.AgentVersion;

/**
 * Test that doesn't require any EE infrastructure - just checking to make sure
 * the agent version check works.
 *
 * @author John Mazzitelli, Simeon Pinder
 */
@Test
public class AgentSupportedBuildTest {

    private String agentLatestBuild;
    private String agentLatestVersion;
    private String supportedBuildsRegex;

    // our agent manager to use to run the check - its the real AgentManagerBean that doesn't require any EE infrastructure for this test
    private AgentManagerBean agentManager = new AgentManagerStub();

    public void testLatestAgentBuildCheck() {
        AgentVersion agentVersionInfo = setLatestAgentVersionAndBuildToCheck("1.0.GA", "cafebabe0", "1.0.GA",
            "cafebabe0");
        assert true == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
        agentVersionInfo = setLatestAgentVersionAndBuildToCheck("1.0.GA", "cafebabe0", "1.0.RC1", "cafebabe1");
        assert false == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
        agentVersionInfo = setLatestAgentVersionAndBuildToCheck("1.0.GA", "nocafe", "2.0.GA", "cafebabe2");
        assert false == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
    }

    /** Test Regex options. When build strings are no longer used then 3.2.0.GA-redhat-N is most elegant test via regex.
     *  Also need to exercise regex with build ids to support GA and Update one where Version string is identical, 
     *  differing only by build(git commit hash).
     */
    public void testSupportedBuildsCheck() {
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

        // regex test #4 - tests for lists of accepted builds
        // Ex. 3.2.0.GA   | 3.2 Update 01 | 3.2 Update 02 | 
        regex = "cafebabe0|cafebabe1|cafebabe2|abcdef8";
        checkOK(regex, "cafebabe0");
        checkOK(regex, "cafebabe1");
        checkOK(regex, "cafebabe2");
        checkOK(regex, "abcdef8");
        checkFail(regex, "cafebabe9");

        // regex test #5 - tests for lists of accepted builds
        // Ex. 3.2.0.GA   | 3.2 Update 01 | 3.2 Update 02 | 
        regex = ".*(cafebabe0|cafebabe1|cafebabe2|abcdef8).*";
        checkOK(regex, "test:cafebabe0");
        checkOK(regex, "abeabe\\:cafebabe1");
        checkOK(regex, "cafebabe2");
        checkOK(regex, "abcdef8");
        checkFail(regex, "cafebabe9");
    }

    public void testLatestAgentBuildCheckRegex() {
        String regexOptions = "cafebabe0|3degf01|cafebabe9";
        AgentVersion agentVersionInfo = setSupportedBuildsToCheck(regexOptions, "1.0.GA", "cafebabe0");
        assert true == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
        agentVersionInfo = setSupportedBuildsToCheck(regexOptions, "1.0.RC1", "cafebabe1");
        assert false == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
        agentVersionInfo = setSupportedBuildsToCheck(regexOptions, "2.0.GA", "3deg");
        assert false == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported();
    }

    private void checkOK(String supportedBuilds, String agentBuildToCheck) {
        check(supportedBuilds, agentBuildToCheck, true);
    }

    private void checkFail(String supportedBuilds, String agentBuildToCheck) {
        check(supportedBuilds, agentBuildToCheck, false);
    }

    private void check(String supportedBuilds, String agentBuildToCheck, boolean expectedResult) {
        //Version string completely ignored in this case and only specific build identifier matters.
        AgentVersion agentVersionInfo = setSupportedBuildsToCheck(supportedBuilds, "", agentBuildToCheck);
        assert expectedResult == agentManager.isAgentVersionSupported(agentVersionInfo).isSupported() : "supportedBuilds="
            + supportedBuilds + "; agentBuildToCheck=" + agentBuildToCheck;
    }

    private AgentVersion setSupportedBuildsToCheck(String supportedBuildsRegex, String agentVersionToCheck,
        String agentBuildToCheck) {
        this.supportedBuildsRegex = supportedBuildsRegex;

        return new AgentVersion(agentVersionToCheck, agentBuildToCheck);
    }

    private AgentVersion setLatestAgentVersionAndBuildToCheck(String agentLatestVersion, String agentLatestBuild,
        String agentVersionToCheck,
        String agentBuildToCheck) {
        this.supportedBuildsRegex = null;
        this.agentLatestBuild = agentLatestBuild;
        this.agentLatestVersion = agentLatestVersion;

        return new AgentVersion(agentVersionToCheck, agentBuildToCheck);
    }

    private class AgentManagerStub extends AgentManagerBean {
        @Override
        public Properties getAgentUpdateVersionFileContent() {
            // these are private constants in the subclass, so we don't have access to them - just redefine them here
            final String RHQ_AGENT_LATEST_BUILD = "rhq-agent.latest.build-number";
            final String RHQ_AGENT_LATEST_VERSION = "rhq-agent.latest.version";
            final String RHQ_AGENT_SUPPORTED_BUILDS = "rhq-agent.supported.builds";

            Properties p = new Properties();
            p.put(RHQ_AGENT_LATEST_VERSION, AgentSupportedBuildTest.this.agentLatestVersion);
            p.put(RHQ_AGENT_LATEST_BUILD, AgentSupportedBuildTest.this.agentLatestBuild);

            // this is optional, a system does not have to have this set
            if (AgentSupportedBuildTest.this.supportedBuildsRegex != null) {
                p.put(RHQ_AGENT_SUPPORTED_BUILDS, AgentSupportedBuildTest.this.supportedBuildsRegex);
            }
            return p;
        }
    }
}
