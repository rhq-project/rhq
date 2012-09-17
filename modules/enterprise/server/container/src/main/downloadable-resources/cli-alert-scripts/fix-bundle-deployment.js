/**
 * Using this script one can automatically reset a bundle deployment to a specified version.
 * 
 * The configuration of the bundle and its version to reset to is located at the top of this file. 
 * Please provide the values to the variables as instructed to make this script work in 
 * a concrete situation.
 */

//----------------------------- script configuration

/**
 * Replace the "null" with the ID of the bundle destination you want to reset.
 * 
 * The ID of the destination can be determined from the URL. If you navigate to 
 * some destination of a bundle, the destination ID is the number at the very
 * end of the URL in the address bar of your browser. 
 * 
 * It can also be determined programmatically using the remote API of the RHQ 
 * server but that is beyond the scope of this comment. 
 */
var destinationId = null; 

/**
 * Replace the "null" with the ID of the bundle version the deployment should be reset to.
 * 
 * As with the destinations, the ID of a bundle version can be determined in the GUI by
 * examining the URL. If you navigate to some version of a bundle, the bundle version ID is
 * the very last number in the URL in the address bar of the browser. 
 */
var bundleVersionId = null;

/**
 * A deployment of the bundle version may require configuration. 
 * Normally, this can be provided as a simple javascript object (using the curly braces notation). 
 * Each key in the object corresponds to a name of one "rhq:input-property" in the
 * recipe of the bundle.
 * 
 * For example, if your deploy.xml recipe contains the following input properties:
 * 
 * <rhq:input-property type="string" name="my-string-property" />
 * <rhq:input-property type="boolean" name="my-bool-property" />
 * <rhq:input-property type="integer" name="my-int-property" />
 * 
 * You would provide values for those properties as:
 * 
 * var deploymentConfig = {
 *     "my-string-property" : "value",
 *     "my-bool-property" : false,
 *     "my-int-property" : 42
 * };
 * 
 * 
 * Note that the bundle recipes support only a limited set of types of these properties.
 * Please consult the provisioning subsystem documentation for a complete coverage of the bundle recipe
 * capabilities.
 */
var deploymentConfig = null;

/**
 * The description of the deployment that will be performed.
 */
var description = "redeploy due to drift";

// DO NOT TOUCH THIS UNLESS YOU FULLY UNDERSTAND WHAT YOU ARE DOING.
// NOTE: It's essential that isCleanDeployment=true, otherwise files that have drifted will not be replaced with their
//       original versions from the bundle.
var isCleanDeployment = true;

//-------------------------- script body

var bundles = require('rhq://downloads/bundles');

bundles.deployBundle(destinationId, bundleVersionId, deploymentConfig, description, isCleanDeployment);
