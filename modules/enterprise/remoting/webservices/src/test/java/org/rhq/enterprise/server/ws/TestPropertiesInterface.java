package org.rhq.enterprise.server.ws;

/**Only point of this file is to set testing properties for all instances.
 * 
 * @author spinder
 *
 */
public interface TestPropertiesInterface {

	static final boolean TESTS_ENABLED = true;
	static String credentials = "ws-test";
	static String host = "127.0.0.1";
	static int port = 7080;
	static boolean useSSL = false;

}
