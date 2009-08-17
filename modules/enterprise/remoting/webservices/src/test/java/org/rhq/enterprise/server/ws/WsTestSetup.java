package org.rhq.enterprise.server.ws;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import org.rhq.enterprise.server.ws.utility.WsUtility;

public class WsTestSetup extends AssertJUnit {
	
	protected static String credentials = "ws-test";
	protected static String host = "127.0.0.1";
	protected static int port = 7080;
	protected static boolean useSSL = false;

	private static final String WS_PACKAGE = "org.rhq.enterprise.server.ws.";

//	static AlertManagerRemote ALERT_MANAGER_REMOTE = null;
//	static AlertDefinitionManagerRemote ALERT_DEFINITION_MANAGER_REMOTE = null;
//	static AvailabilityManagerRemote AVAILABILITY_MANAGER_REMOTE = null;
//	static CallTimeDataManagerRemote CALL_TIME_DATA_MANAGER_REMOTE = null;
//    static ChannelManagerRemote CHANNEL_MANAGER_REMOTE = null;
//	static ConfigurationManagerRemote CONFIGURATION_MANAGER_REMOTE = null;
//	static ContentManagerRemote CONTENT_MANAGER_REMOTE = null;
//	static EventManagerRemote EVENT_MANAGER_REMOTE = null;
//	static MeasurementBaselineManagerRemote MEASUREMENT_BASELINE_MANAGER_REMOTE = null;
//	static MeasurementDataManagerRemote MEASUREMENT_DATA_MANAGER_REMOTE = null;
//	static MeasurementDefinitionManagerRemote MEASUREMENT_DEFINITION_MANAGER_REMOTE = null;
//	static MeasurementProblemManagerRemote MEASUREMENT_PROBLEM_MANAGER_REMOTE = null;
//	static MeasurementScheduleManagerRemote MEASUREMENT_SCHEDULE_MANAGER_REMOTE = null;
//	static OperationManagerRemote OPERATION_MANAGER_REMOTE = null;
//	static ResourceManagerRemote RESOURCE_MANAGER_REMOTE = null;
//	static ResourceGroupManagerRemote RESOURCE_GROUP_MANAGER_REMOTE = null;
//	static RoleManagerRemote ROLE_MANAGER_REMOTE = null;
//	static SubjectManagerRemote SUBJECT_MANAGER_REMOTE = null;
	
	static WebserviceRemotes WEBSERVICE_REMOTE = null;
	
	static ObjectFactory WS_OBJECT_FACTORY = new ObjectFactory();
//	static String[] beans = { "Alert", "AlertDefinition", "Availability",
//			"CallTimeData", "Channel", "Configuration", "Content","Event",
//			"MeasurementBaseline", "MeasurementData", "MeasurementDefinition",
//			"MeasurementProblem", "MeasurementSchedule", "Operation",
//			"Resource", "ResourceGroup", "Role", "Subject" };
	static String[] beans ={"JonWebservice"};
//	private static String host = "";
//	private static int port = -1;

	public WsTestSetup(String host, int port, boolean useSSL)
			throws MalformedURLException, ClassNotFoundException,
			SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		
		Object[] empty = new Object[0];
		for (int i = 0; i < beans.length; i++) {
			// Build URL references to connect to running WS enabled server
			Class<?> beanClass = Class.forName(WS_PACKAGE + beans[i]
					+ "ManagerBeanService");
			URL gUrl = WsUtility.generateRhqRemoteWebserviceURL(beanClass,
					host, port, useSSL);
			QName gQName = WsUtility
					.generateRhqRemoteWebserviceQName(beanClass);
			Class<?>[] params = new Class[2];
			  params[0] = URL.class;
			  params[1] = QName.class;
			Class<?>[] params2 = new Class[0];
			//iterate over beans
			
			Constructor<?> constructor = beanClass.getConstructor(params);
			Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
					+ "ManagerBeanPort", params2);
			Object service = constructor.newInstance(gUrl, gQName);
			WEBSERVICE_REMOTE = (WebserviceRemotes) getRemote.invoke(
					service, empty);

			
//			if (beans[i].equalsIgnoreCase("Alert")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//						+ "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				ALERT_MANAGER_REMOTE = (AlertManagerRemote) getRemote.invoke(
//						service, empty);
//			} 
//			else if (beans[i].equalsIgnoreCase("AlertDefinition")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				ALERT_DEFINITION_MANAGER_REMOTE = (AlertDefinitionManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Availability")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				AVAILABILITY_MANAGER_REMOTE = (AvailabilityManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("CallTimeData")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				CALL_TIME_DATA_MANAGER_REMOTE = (CallTimeDataManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Channel")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				CHANNEL_MANAGER_REMOTE = (ChannelManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Configuration")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				CONFIGURATION_MANAGER_REMOTE = (ConfigurationManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Content")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				CONTENT_MANAGER_REMOTE = (ContentManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Event")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				EVENT_MANAGER_REMOTE = (EventManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("MeasurementBaseline")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				MEASUREMENT_BASELINE_MANAGER_REMOTE = (MeasurementBaselineManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("MeasurementData")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				MEASUREMENT_DATA_MANAGER_REMOTE = (MeasurementDataManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("MeasurementDefinition")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				MEASUREMENT_DEFINITION_MANAGER_REMOTE = (MeasurementDefinitionManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("MeasurementProblem")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				MEASUREMENT_PROBLEM_MANAGER_REMOTE = (MeasurementProblemManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("MeasurementSchedule")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				MEASUREMENT_SCHEDULE_MANAGER_REMOTE = (MeasurementScheduleManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Operation")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				OPERATION_MANAGER_REMOTE = (OperationManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Resource")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				RESOURCE_MANAGER_REMOTE = (ResourceManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("ResourceGroup")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				RESOURCE_GROUP_MANAGER_REMOTE = (ResourceGroupManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Role")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//				                                                             + "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				ROLE_MANAGER_REMOTE = (RoleManagerRemote) getRemote.invoke(
//						service, empty);
//			}
//			else if (beans[i].equalsIgnoreCase("Subject")) {
//				Constructor<?> constructor = beanClass.getConstructor(params);
//				Method getRemote = beanClass.getDeclaredMethod("get" + beans[i]
//						+ "ManagerBeanPort", params2);
//				Object service = constructor.newInstance(gUrl, gQName);
//				SUBJECT_MANAGER_REMOTE = (SubjectManagerRemote) getRemote.invoke(
//						service, empty);
//			}
		}
	}
}
