package org.rhq.bundle.ant;

/**
 * @author Ian Springer
 */
public class DeployPropertyNames {
    /** System property that should always be available to ant scripts - it's the location where the deployment should be installed */
    public static final String DEPLOY_DIR = "rhq.deploy.dir";
    
    /** System property that should always be available to ant scripts - it's the ID of the bundle deployment */
    public static final String DEPLOY_ID = "rhq.deploy.id";

    private DeployPropertyNames() {
    }
}
