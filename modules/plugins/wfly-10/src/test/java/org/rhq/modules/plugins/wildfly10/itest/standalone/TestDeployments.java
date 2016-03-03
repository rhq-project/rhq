package org.rhq.modules.plugins.wildfly10.itest.standalone;

import java.io.IOException;
import java.io.InputStream;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;

/**
* @author Thomas Segismont
*/
enum TestDeployments {
    DEPLOYMENT_1("test-simple-1.war"), //
    DEPLOYMENT_2("test-simple-2.war"), //
    JAVAEE6_TEST_APP("javaee6-test-app.war");

    private String deploymentName;
    private String resourceKey; // different if version is stripped
    private String resourcePath;
    private byte[] hash;

    TestDeployments(String warName) {
        this(warName, warName);
    }

    TestDeployments(String warName, String resourceKey) {
        this.deploymentName = warName;
        this.resourceKey = resourceKey;
        this.resourcePath = "itest/" + warName;
        InputStream resourceAsStream = DeploymentTest.class.getClassLoader().getResourceAsStream(resourcePath);
        try {
            hash = MessageDigestGenerator.getDigest(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtil.safeClose(resourceAsStream);
        }
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public byte[] getHash() {
        return hash;
    }
}
