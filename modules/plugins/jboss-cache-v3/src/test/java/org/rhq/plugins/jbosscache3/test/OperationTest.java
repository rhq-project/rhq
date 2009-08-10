package org.rhq.plugins.jbosscache3.test;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

public class OperationTest {
	private Log log = LogFactory.getLog(OperationTest.class);
	private EmsConnection connection;
	private String[] INTERCEPTOR_STATISTIC_FIELDS = { "HitMissRatio",
			"ReadWriteRatio", "AverageWriteTime", "AverageReadTime" };

	private static String CACHE_STATUS_ATRIBUTE = "CacheStatus";

	public OperationTest(EmsConnection connection) {
		this.connection = connection;
	}

	protected void testOperations() throws Exception {

		for (String resourceTypeName : CacheComponentTest.RESOURCE_TYPES) {

			ResourceType resourceType = TestHelper.getResourceType(
					resourceTypeName, CacheComponentTest.CACHE_PLUGIN_NAME);

			Set<OperationDefinition> operationDefinitions = resourceType
					.getOperationDefinitions();

			Set<Resource> resources = TestHelper.getResources(resourceType);

			for (Resource resource : resources) {
				log.info("Validating operations for " + resource + "...");
				OperationFacet operationFacet = ComponentUtil.getComponent(
						resource.getId(), OperationFacet.class,
						FacetLockType.WRITE,
						CacheComponentTest.OPERATION_FACET_METHOD_TIMEOUT,
						true, true);

				for (OperationDefinition operationDefinition : operationDefinitions) {
					String name = operationDefinition.getName();

					if (name.equals("printDetails")) {
						System.out.println("cnec");
					}
					OperationResult result = operationFacet.invokeOperation(
							name, new Configuration());

					log.info("Validating operation '" + name + "' result ("
							+ result + ")...");

					ConfigurationDefinition def = operationDefinition
							.getResultsConfigurationDefinition();

					if (CacheStatus.CacheOperations.contains(name)) {
						Object obj = getEmsAttribute(resource.getResourceKey(),
								CACHE_STATUS_ATRIBUTE);
						String statusName = obj.toString();
						if (CacheStatus.isOperationRunning(statusName)) {
							// TODO waiting for result
						}
						assert (CacheStatus.isOperationFinished(statusName));
					}

					if (def != null) {
						PropertyDefinitionSimple property = def
								.getPropertyDefinitionSimple("output");

						if (property != null) {
							Object emsOperationRes = getEmsOperationResult(
									resource.getResourceKey(), name);
							String value = result.getComplexResults()
									.getSimpleValue("output", "");
							String type = getOperationResultType(resource
									.getResourceKey(), name);
							assert (TestHelper.compareValues(value, type,
									emsOperationRes));
						}
					}

				}
			}
		}
		return;
	}

	protected boolean testResetFields(String[] fields, Resource resource) {
		try {
			for (String metricName : fields) {
				String value = TestHelper.getMetricValue(resource, metricName);
				Double dValue = Double.valueOf(value);
				if (!dValue.equals(0))
					return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected Object getEmsOperationResult(String beanName, String operationName) {
		EmsBean bean = connection.getBean(beanName);
		EmsOperation operation = bean.getOperation(operationName);

		Object obj = operation.invoke(new Object[] {});
		return obj;
	}

	protected String getOperationResultType(String beanName,
			String operationName) {
		EmsBean bean = connection.getBean(beanName);
		EmsOperation operation = bean.getOperation(operationName);
		return operation.getReturnType();
	}

	protected Object getEmsAttribute(String beanName, String attName) {
		EmsBean bean = connection.getBean(beanName);
		EmsAttribute att = bean.getAttribute(attName);

		Object obj = att.getValue();
		return obj;
	}
}
