package org.rhq.enterprise.server.ws;

public class JaxbUtilities {
	
	/**A utility method to handle Property instances as bundled in Map instances and HashMap with JAXB.
	 * Initial syntax is unintuitive due to JAXB specification change that is needed. See following link. 
	 * https://jaxb.dev.java.net/guide/Mapping_your_favorite_class.html
	 * @param configuration
	 * @param property
	 * @return boolean flag indicating success.
	 */
	public static boolean addProperty(WsConfiguration configuration,
			Property property) {
		boolean success = false;
		if ((configuration != null) && (property != null)) {//null checking
			if (property instanceof PropertySimple) {
				configuration.getPropertySimpleContainer().add((PropertySimple) property);
				success=true;
			}
			else if (property instanceof PropertyList) {
				configuration.getPropertyListContainer().add((PropertyList) property);
				success=true;
			}
			else if (property instanceof PropertyMap) {
				configuration.getPropertyMapContainer().add((PropertyMap) property);
				success=true;
			}
		} else {
			throw new IllegalArgumentException(
					"Configuration or Property objects passed in cannot be null.");
		}
		return success;
	}

}
