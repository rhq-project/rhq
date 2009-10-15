package org.rhq.enterprise.server.plugins.rhnhosted;

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.clientapi.server.plugin.content.ContentProvider;
 import org.rhq.core.clientapi.server.plugin.content.InitializationException;

 import java.security.KeyException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.FileInputStream;


/**
 * @author pkilambi
 *
 */
public class RHNSource implements ContentProvider {

    private final Log log = LogFactory.getLog(RHNSource.class);
    private RHNConnector rhnObject;

    /**
     * Initializes the adapter with the specified configuration.
     *
     * <p/>Expects <u>one</u> of the following properties:
     *
     * <p/>
     * <table border="1">
     *   <tr>
     *     <td><b>location</b></td>
     *     <td>RHN Hosted server URL</td>
     *   </tr>
     *   <tr>
     *     <td><b>certificate</b></td>
     *     <td>A certificate for authentication and subscription validation.</td>
     *   </tr>
     * </table>
     *
     * <p/>
     *
     * @param  configuration The adapter's configuration propeties.
     *
     * @throws Exception On errors.
     */
    public void initialize(Configuration configuration) throws Exception {
        String locationIn = configuration.getSimpleValue("location", null);
        String certificate = configuration.getSimpleValue("certificate", null);
        String location = locationIn + RHNConstants.DEFAULT_HANDLER;
        // check location field validity
        if (location == null) {
            throw new IllegalArgumentException("Missing required 'location' property");
        }

        // check certificate field validity
        try {
            Certificate cert = CertificateFactory.read(certificate);
            PublicKeyRing keyRing = this.readDefaultKeyRing();
            cert.verifySignature(keyRing);

        } catch(Exception e) {
            log.debug("Invalid Cert");
            throw new InitializationException("Invalid Certificate", e);
        }
        
        // Now we have valid data. Spawn the activation.
        try {
            rhnObject = new RHNConnector(certificate, location);
            rhnObject.processActivation();
            log.debug("Activation successful");
        } catch (Exception e) {
            log.debug("Activation Failed. Please check your configuration");
            throw new InitializationException("Server Activation Failed.", e);
        }
    }

    /**
     * Shutdown the adapter.
     */
    public void shutdown() {
        log.debug("shutdown");
    }



   /**
     * Test's the adapter's connection.
     *
     * @throws Exception When connection is not functional for any reason.
     */
    public void testConnection() throws Exception {
        rhnObject.processDeActivation();
        rhnObject.processActivation();
    }

    protected PublicKeyRing readDefaultKeyRing()
        throws ClassNotFoundException, KeyException, IOException {
        InputStream keyringStream = new FileInputStream(RHNConstants.DEFAULT_WEBAPP_GPG_KEY_RING);
        return new PublicKeyRing(keyringStream);
    }

}
