package org.rhq.enterprise.communications;

import java.util.prefs.Preferences;

import org.testng.annotations.Test;

@Test
public class ServiceContainerConfigurationTest {
    public void testTransportParams() {
        Preferences prefs = Preferences.userRoot().node("rhqtest").node("ServiceContainerConfigurationTest");

        // BZ 1166383 - we need to make sure generalizeSocketException=true gets in the params even if we don't specify it

        // first make sure its in the default
        assert (new ServiceContainerConfiguration(prefs)).getConnectorTransportParams().contains(
            "generalizeSocketException=true") : "missing expected param";

        // now try all different formats of transport param and make sure the one we want always gets in there
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS, "/some/path");
        assertTransportParams(prefs, "/some/path/?generalizeSocketException=true");

        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS, "/some/path/");
        assertTransportParams(prefs, "/some/path/?generalizeSocketException=true");

        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS, "/some/path/?foo");
        assertTransportParams(prefs, "/some/path/?foo&generalizeSocketException=true");

        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS, "/some/path/?foo=false");
        assertTransportParams(prefs, "/some/path/?foo=false&generalizeSocketException=true");

        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS, "foo=false");
        assertTransportParams(prefs, "foo=false&generalizeSocketException=true");

        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS, "foo=false&bar=1");
        assertTransportParams(prefs, "foo=false&bar=1&generalizeSocketException=true");

    }

    private void assertTransportParams(Preferences p, String expected) {
        ServiceContainerConfiguration config = new ServiceContainerConfiguration(p);
        String actual = config.getConnectorTransportParams();
        assert actual.equals(expected) : "actual [" + actual + "] != expected [" + expected + "]";
    }
}
