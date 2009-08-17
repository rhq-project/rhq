package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.List;

import org.rhq.enterprise.server.ws.ResourceCriteria;

import org.testng.annotations.Test;

@Test(groups = "ws")
public class WsResourceManagerTest extends WsTestSetup{

	private static final boolean TESTS_ENABLED = true;

	public WsResourceManagerTest()
			throws MalformedURLException, ClassNotFoundException,
			SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		super(host, port, useSSL);
	}

	@Test(enabled = TESTS_ENABLED)
	public void testResourceManager() throws java.lang.Exception {
		//define search term
		String searchTerm = "RHQ AGENT";
		
		//build criteria
		Subject subject = WEBSERVICE_REMOTE.login(credentials, credentials);
		ResourceCriteria resourceCriteria = WS_OBJECT_FACTORY.createResourceCriteria();
		List<Resource> results = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
		assertNotNull("Results not located correctly",results);
		
		//without filter term, should be get *
		int totoalResourcesLocated = results.size();
		
		//add criterion .. and resubmit
		resourceCriteria.setFilterName(searchTerm);
		results = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
		assertNotNull("Results not located correctly",results);
		assertTrue("Criteria did not filter properly.",(totoalResourcesLocated > results.size()));
	}

}
