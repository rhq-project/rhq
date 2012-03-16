
//
// THIS IS NOT A STANDALONE SCRIPT, it is a sample and needs customization!
//
// This script performs some mock file remediation.  It is designed as a script to be executed via
// an Alert CLI Notification for an alert firing on Drift detection.  The following blog
// shows a demo of its intended use:
//
//   http://jayshaughnessy.blogspot.com/2012_01_01_archive.html
// 
// In short, the idea is that RHQ Drift detection is being used to ensure file drift is reported. And
// RHQ Alerting is in place to respond to reported drift by executin a script like this to remediate the
// problem by provisioning a clean file-set, thus eliminating the drift.
//
// NOTES:
// - The 'alert' variable is seeded by the alert sender
// - It is assumed that the needed bundle version and bundle destination already exist.

// SET THESE VARIABLES
var bundleDestinationName = 'Alert CLI Demo'
var bundleVersion = 1
var logFile = '/tmp/alert-cli-demo/logs/alert-' + alert.id + '.log'

// Log what we're doing to a file tied to the fired alert id
//
var e = exporter
e.setTarget( 'raw', logFile )

// Dump the alert
//
e.write( alert )

// get a proxy for the alerted-on Resource
//
var alertResource = ProxyFactory.getResource(alert.alertDefinition.resource.id)

// Dump the resource
//
e.write( " " )
e.write( alertResource )


// Remediate file

// Find the Bundle Destination
//
var destCrit = new BundleDestinationCriteria()
destCrit.addFilterName( bundleDestinationName )
var result = BundleManager.findBundleDestinationsByCriteria( destCrit )
var dest = result.get( 0 )

// Find the Bundle Version
//
var versionCrit = new BundleVersionCriteria()
versionCrit.addFilterVersion( bundleVersion )
result = BundleManager.findBundleVersionsByCriteria( versionCrit )
var ver = result.get( 0 )

// Create a new Deployment for the bundle version and the destination
//
var deployment = BundleManager.createBundleDeployment(ver.getId(), dest.getId(), 'remediate drift', new Configuration())

// Schedule a clean deploy of the deployment. This will wipe out the edited file and lay down a clean copy
//
BundleManager.scheduleBundleDeployment(deployment.getId(), true)

e.write( " " )
e.write( "REMEDIATION COMPLETE!" )
