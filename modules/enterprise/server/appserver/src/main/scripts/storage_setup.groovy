/**
 * This script is intended for use with dev builds. It prepares the dev-container for
 * storage node installations. The maven build is configured such that this script will be
 * executed automatically with default values during the package phase when the dev profile
 * is active.
 *
 * Note that this script does not install storage nodes. It only sets up the necessary
 * directory structure for installing storage nodes. For the default storage node, it
 * only generates properties file.
 *
 * The setup is done only for the default storage node installation by default. To run the
 * setup for additional storage node installations at the same time you build your
 * dev-container run,
 *
 *   $ mvn clean package -Pdev -Drhq.storage.num-nodes=3
 *
 * Running this command line should produce dev-container/rhq-server,
 * dev-container/rhq-server-2, and dev-container/rhq-server-3. The rhq-server-2 and
 * rhq-server-3 directories do not contain the full server distribution. They contain the
 * necessary scripts combined with a number of symlinks to provide a fast, lightweight
 * solution for deploying multiple storage nodes with the dev-container.
 *
 * You can also run the setup for additional storage nodes after you have already built
 * your dev-container by executing
 *
 *   $ mvn -o groovy:execute -Pdev -Dsource=src/main/scripts/storage_setup.groovy -Drhq.storage.num-nodes=4
 *
 * The script will detect that you already have rhq-server-2 and rhq-server-3 setup; so, it
 * will only set up rhq-server-4.
 *
 * Finally, you can configure the storage installer heap options using the following system
 * properties,
 *
 *   rhq.storage.heap-size
 *   rhq.storage.heap-new-size
 *
 *   $ mvn clean package -Pdev -Drhq.storage.num-nodes=2 \
 *                       -Drhq.storage.heap-size="512M" \
 *                       -Drhq.storage.heap-new-size="128M"
 */

numNodes = (properties['rhq.storage.num-nodes'] ?: 1) as Integer

heapSize = properties['rhq.storage.heap-size'] ?: '256M'
heapNewSize = properties['rhq.storage.heap-new-size'] ?: '64M'
dataRootDir = properties['rhq.dev.data.dir'] ?: "${properties['rhq.rootDir']}/rhq-data"
defaultJmxPort = 7299

defaultServerBasedir = properties["rhq.containerServerDir"]
log.info "DEFAULT SERVER BASEDIR = $defaultServerBasedir"

defaultServerBasedir = "dev-container/rhq-server/"
log.info "DEFAULT SERVER BASEDIR = $defaultServerBasedir"


seeds = calculateSeeds()

def calculateSeeds() {
  def addresses = []
  for (i in 1..numNodes) {
    addresses << "127.0.0.$i"
  }
  return addresses.join(',')
}

def createStoragePropertiesFile(basedir, nodeId) {
  def binDir = new File(basedir, "bin")
  def propsFile = new File(binDir, "rhq-storage.properties")

  log.info "Creating $propsFile"

  propsFile.withWriter { writer ->
    writer.write("""# storage installer options for dev deployment
rhq.storage.commitlog=${dataRootDir}/storage-$nodeId/commit_log
rhq.storage.data=${dataRootDir}/storage-$nodeId/data
rhq.storage.saved-caches=${dataRootDir}/storage-$nodeId/saved_caches
rhq.storage.heap-size=${heapSize}
rhq.storage.heap-new-size=${heapNewSize}
rhq.storage.hostname=127.0.0.$nodeId
rhq.storage.jmx-port=${defaultJmxPort + (nodeId - 1)}
rhq.storage.seeds=${seeds}
rhq.storage.verify-data-dirs-empty=false
"""
    )
  }
}

def prepareDefaultStorageInstallation() {
  log.info "Preparing default storage installation in $defaultServerBasedir"

  def storageProps = new File("$defaultServerBasedir/bin/rhq-storage.properties")
  def rhqctlProps = new File("$defaultServerBasedir/bin/rhqctl.properties")

  if (storageProps.exists() && rhqctlProps.exists()) {
    log.info "It looks like setup for default storage has already run"
    log.info "Skipping storage setup for $defaultServerBasedir"
  } else {
    createStoragePropertiesFile(defaultServerBasedir, 1)
  }
}

def prepareAdditionalStorageInstallations() {
  log.info "Preparing for additional storage installations"

  for (i in 2..numNodes) {
    def nodeId = i
    def basedir = "${properties["rhq.containerDir"]}/rhq-server-$nodeId"

    log.info "Preparing for storage node installation at $basedir"

    if (new File(basedir).exists()) {
      log.info "$basedir already exists"
      log.info "Skipping strorage installation setup."
    } else {
      ant.mkdir(dir: basedir)
      ant.copy(todir: "${basedir}/bin") {
        fileset(dir:"${defaultServerBasedir}/bin")
      }
      log.info "Updating permissions in $basedir/bin"
      ant.chmod(dir: "$basedir/bin", perm: "ug+x", includes: "*.sh,rhqctl", verbose: true)
      ant.chmod(dir: "$basedir/bin/internal", perm: "ug+x", includes: "*.sh,rhqctl", verbose: true)

      createStoragePropertiesFile(basedir, nodeId)

      log.info "Creating symlink for $basedir/jbossas"
      ant.symlink(link: "${basedir}/jbossas", resource: "${defaultServerBasedir}/jbossas")

      prepareModulesDir(basedir)

      log.info "Setup is complete for $basedir"
    }
  }
}

def prepareModulesDir(basedir) {
  def modulesDir = "$basedir/modules"
  def defaultModulesDir = "$defaultServerBasedir/modules"

  log.info "Preparing $modulesDir"
  log.info "Creating symlinks for RHQ modules"

  ant.mkdir(dir: "$basedir/modules/org")
  ant.symlink(link: "$modulesDir/org/apache", resource: "$defaultModulesDir/org/apache")

  ant.mkdir(dir: "$modulesDir/org/rhq")
  ant.symlink(
    link: "$modulesDir/org/rhq/rhq-cassandra-installer",
    resource: "$defaultModulesDir/org/rhq/rhq-cassandra-installer"
  )
  ant.symlink(link: "$modulesDir/org/rhq/rhq-server-control", resource: "$defaultModulesDir/org/rhq/rhq-server-control")

  def downloadsDir = "$modulesDir/org/rhq/server-startup/main/deployments/rhq.ear/rhq-downloads"
  ant.mkdir(dir: downloadsDir)
  ant.symlink(
    link: "$downloadsDir/rhq-agent",
    resource: "$defaultModulesDir/org/rhq/server-startup/main/deployments/rhq.ear/rhq-downloads/rhq-agent"
  )
}

prepareDefaultStorageInstallation()
if (numNodes > 1) {
  prepareAdditionalStorageInstallations()
}





