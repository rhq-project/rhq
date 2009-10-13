package org.rhq.enterprise.server.plugins.rhnhosted;

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.clientapi.server.plugin.content.ContentProvider;


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
        
        if (location == null) {
            throw new IllegalArgumentException("Missing required 'location' property");
        }

        if (certificate == null) {
            throw new IllegalArgumentException("Invalid Cerdentials");
        }

        rhnObject = new RHNConnector(certificate, location);
        rhnObject.Activate();

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
        rhnObject.DeActivate();
        rhnObject.Activate();
    }

}
