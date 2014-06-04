package org.rhq.enterprise.agent;

import java.util.prefs.Preferences;

import mazz.i18n.Logger;

import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.testng.annotations.Test;

@Test
public class VMHealthCheckTest {

    private final int interval = 1000;

    private final Logger LOG = AgentI18NFactory.getLogger(getClass());

    public void testIt() throws Exception {
        AgentMain agent = new AgentMain();
        Preferences p = agent.getConfiguration().getPreferences();
        p.put(AgentConfigurationConstants.VM_HEALTH_CHECK_INTERVAL_MSECS, "" + interval);
        p.put(AgentConfigurationConstants.WAIT_FOR_SERVER_AT_STARTUP_MSECS, "100");
        p.put(AgentConfigurationConstants.DO_NOT_START_PLUGIN_CONTAINER_AT_STARTUP, "true");
        agent.getConfiguration().setAgentSecurityToken("foo");
        VMHealthCheckThread t = new VMHealthCheckThread(agent);
        t.start();
        assert t.isOutOfMemory() == false: "memory should be good";
        t.stopChecking();
        t.join(1000);
        assert t.isAlive() == false;

        VMHealthCheckThread t2 = new VMHealthCheckThread(agent) {

            @Override
            boolean isOutOfMemory() {
                LOG.info("isOutOfMemory", new Throwable());
                return true;
            }

        };

        LOG.info("start check thread");
        t2.start();
        LOG.info("start agent");
        agent.start(); // this will block until the check thread kills it
        Thread.sleep(interval + 500);
        LOG.info("wait for agent to be shutdown");
        assert !agent.isStarted() : "agent shutdown by checker";
    }
}
