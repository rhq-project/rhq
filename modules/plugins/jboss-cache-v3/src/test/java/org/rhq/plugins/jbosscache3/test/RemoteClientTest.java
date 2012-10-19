package org.rhq.plugins.jbosscache3.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;

public class RemoteClientTest {

	private static final String REMOTE_NAME = "TestCacheBean/remote";
	private static final String JBOSS_HOME = "homeDir";
	private static final String JBOSS_NAME = "serverName";
	private Log log = LogFactory.getLog(RemoteClientTest.class);
	private List<String> fileNames;
	private Object remoteObject;

	public RemoteClientTest() {
		fileNames = new ArrayList<String>();
	}

	public void runTest() {

		try {
			remoteObject = AppServerUtils.getRemoteObject(REMOTE_NAME,
					Object.class);

			AppServerUtils.invokeMethod("create", remoteObject,
					(MethodArgDef[]) null);
			AppServerUtils.invokeMethod("test", remoteObject,
					(MethodArgDef[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void runClientClean() {
		try {

			AppServerUtils.invokeMethod("remove", remoteObject,
					(MethodArgDef[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void deployCacheExample(String jarPath) throws Exception {
		log.info("Deploying cache example " + jarPath + ".");
		fileNames.add(jarPath);
		File jarFile = new File(jarPath);

		AppServerUtils.deployFileToAS(jarFile.getName(), jarFile, false);
	}

	public void deployXmlExample(String xmlFilePath) throws Exception {
		log.info("Deploying xml cache example " + xmlFilePath + ".");
		File sourceFile = new File(xmlFilePath);

		Resource res = AppServerUtils.getASResource();
		Configuration config = res.getPluginConfiguration();
		PropertySimple propHome = config.getSimple(JBOSS_HOME);
		PropertySimple propName = config.getSimple(JBOSS_NAME);
		assert (propHome != null);
		assert (propName != null);

		String name = propHome.getStringValue() + File.separatorChar + "server"
				+ File.separatorChar + propName.getStringValue()
				+ File.separatorChar + "deploy";
		File destDir = new File(name);
		assert (destDir.exists());
		assert (destDir.isDirectory());

		File destFile = File.createTempFile("tmp", "-service.xml", destDir);
		destFile.deleteOnExit();

		TestHelper.copyFile(destFile, sourceFile);
	}

	public void undeployCacheExample() throws Exception {
		for (String name : fileNames) {
			log.info("Undeploying cache example " + name + ".");
			int sepIndex = name.lastIndexOf(File.separatorChar);
			if (sepIndex != -1)
				name = name.substring(sepIndex + 1, name.length());

			AppServerUtils.undeployFromAS(name);
		}
	}
}
