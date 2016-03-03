package org.rhq.modules.plugins.wildfly10.helper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.modules.plugins.wildfly10.helper.ServerPluginConfiguration.Property;

/**
 * 
 * @author lzoubek
 *
 */
@Test(groups = "unit")
public class JBossCliConfigurationTest {

    JBossCliConfiguration cliConfig;
    ServerPluginConfiguration serverConfig;
    Configuration pluginConfig;

    @BeforeMethod
    public void setup() throws Exception {
        File jbossCliXml = new File(getClass().getClassLoader().getResource("jboss-cli-1.3.xml").toURI());
        pluginConfig = Configuration.builder()
            .addSimple(Property.SECURE, "true")
            .addSimple(Property.TRUSTSTORE, "/tmp/truststore")
            .addSimple(Property.TRUSTSTORE_PASSWORD, "truststorepass")
            .addSimple(Property.CLIENTCERT_AUTHENTICATION, true)
            .addSimple(Property.KEYSTORE, "/tmp/keystore")
            .addSimple(Property.KEYSTORE_PASSWORD, "keystorepass")
            .addSimple(Property.KEY_PASSWORD, "keypass")
            .addSimple(Property.NATIVE_HOST, "1.1.1.1")
            .addSimple(Property.NATIVE_PORT, 123456)
            .build();
        serverConfig = new ServerPluginConfiguration(pluginConfig);
        cliConfig = new JBossCliConfiguration(jbossCliXml, serverConfig);
    }

    @Test(expectedExceptions = IOException.class)
    public void fileDoesNotExist() throws Exception {
        cliConfig = new JBossCliConfiguration(new File(UUID.randomUUID().toString()), serverConfig);
    }

    public void detectVersion() {
        Assert.assertEquals(cliConfig.getCliConstants().version(), "1.3");
    }

    public void defaultController() {
        assert cliConfig.configureDefaultController() == null;
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/default-controller/host"), "1.1.1.1");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/default-controller/port"), "123456");
    }

    public void securityNoChangesNotSecure() {
        pluginConfig.remove(Property.SECURE);
        assert cliConfig.configureSecurity() != null;
    }

    public void securityNoChangesNoTruststore() {
        pluginConfig.remove(Property.TRUSTSTORE);
        assert cliConfig.configureSecurity() != null;
    }

    public void security() {
        assert cliConfig.configureSecurity() == null;
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store"), "/tmp/truststore");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store-password"),
            "truststorepass");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-store"), "/tmp/keystore");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-store-password"), "keystorepass");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-password"), "keypass");
    }

    public void securityUsingVault() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone711.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));

        Assert.assertNull(cliConfig.configureSecurityUsingVault(hostConfig));

        // VAULT was written and KEYSTORE_URL option was taken from standalone711.xml
        Assert.assertEquals(
            cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/vault/vault-option[@name='KEYSTORE_URL']/@value"),
            "EAP_HOME/vault/vault.keystore");

        // assert SSL related values were taken from standalone711.xml
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store"), "keystore.jks");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store-password"), "secure");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-store"), "truststore.jks");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-store-password"), "secured");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-password"), "");
    }

    public void securitySChemaVersion10() throws Exception {
        File jbossCliXml = new File(getClass().getClassLoader().getResource("jboss-cli-1.0.xml").toURI());
        cliConfig = new JBossCliConfiguration(jbossCliXml, serverConfig);

        Assert.assertEquals(cliConfig.getCliConstants().version(), "1.0");

        // this hostConfiguration has vault defined
        URL url = getClass().getClassLoader().getResource("standalone711.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));

        // jboss-cli.xml schema version 1.0 does not support vault passwords - expect failure
        Assert.assertEquals(cliConfig.configureSecurityUsingVault(hostConfig),
            "Cannot store truststore passwords using vault, because it is not supported by this version of EAP");

        // jboss-cli.xml schema version 1.0 uses camelCase element names
        Assert.assertNull(cliConfig.configureSecurity());
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store"), "");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trustStore"), "/tmp/truststore");
    }


    public void securityNoClientCertAuth() {
        pluginConfig.setSimpleValue(Property.CLIENTCERT_AUTHENTICATION, String.valueOf(false));
        assert cliConfig.configureSecurity() == null;
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store"), "/tmp/truststore");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/trust-store-password"),
            "truststorepass");
        // key-store related properties must not be set
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-store"), "");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-store-password"), "");
        Assert.assertEquals(cliConfig.obtainXmlPropertyViaXPath("/jboss-cli/ssl/key-password"), "");
    }
}
