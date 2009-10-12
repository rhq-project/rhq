package org.rhq.plugins.jbosscache3.test;

import java.util.HashMap;
import java.util.Map;

public class CacheStatus {

	private static Map<CacheOperations, CacheStatusName> CACHE_OPERATION_RUNNING = new HashMap<CacheOperations, CacheStatusName>();
	private static Map<CacheOperations, CacheStatusName> CACHE_OPERATION_FINISHED = new HashMap<CacheOperations, CacheStatusName>();
	static {
		CACHE_OPERATION_RUNNING.put(CacheOperations.START,
				CacheStatusName.STARTING);
		CACHE_OPERATION_RUNNING.put(CacheOperations.CREATE,
				CacheStatusName.CREATING);
		CACHE_OPERATION_RUNNING.put(CacheOperations.DESTROY,
				CacheStatusName.DESTROYING);
		CACHE_OPERATION_RUNNING.put(CacheOperations.STOP,
				CacheStatusName.STOPPING);

		CACHE_OPERATION_FINISHED.put(CacheOperations.START,
				CacheStatusName.STARTED);
		CACHE_OPERATION_FINISHED.put(CacheOperations.CREATE,
				CacheStatusName.CREATED);
		CACHE_OPERATION_FINISHED.put(CacheOperations.DESTROY,
				CacheStatusName.DESTROYED);
		CACHE_OPERATION_FINISHED.put(CacheOperations.STOP,
				CacheStatusName.STOPPED);
	}

	public enum CacheOperations {
		START("start"), STOP("stop"), CREATE("create"), DESTROY("destroy");
		private String operationName;

		private CacheOperations(String operationName) {
			this.operationName = operationName;
		}

		public String getOperationName() {
			return operationName;
		}

		public static CacheOperations getStatusName(String opName)
				throws Exception {
			for (CacheOperations name : CacheOperations.values()) {
				if (name.getOperationName().equals(opName))
					return name;
			}

			throw new Exception("Status not found.");
		}

		public static boolean contains(String opName) {
			try {
				getStatusName(opName);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	public enum CacheStatusName {
		STARTED("STARTED"), STARTING("STARTING"), CREATED("CREATED"), CREATING(
				"CREATING"), DESTROYED("DESTROYED"), DESTROYING("DESTROYING"), STOPPED(
				"STOPPED"), STOPPING("STOPPING");

		private String cacheName;

		private CacheStatusName(String name) {
			this.cacheName = name;
		}

		public String getCacheName() {
			return cacheName;
		}

		public static CacheStatusName getStatusName(String statusName)
				throws Exception {
			for (CacheStatusName name : CacheStatusName.values()) {
				if (name.getCacheName().equals(statusName))
					return name;
			}

			throw new Exception("Status not found.");
		}
	}

	public static boolean isOperationRunning(String name) {
		try {
			CacheOperations operation = CacheOperations.getStatusName(name);
			CacheStatusName statusName = CACHE_OPERATION_RUNNING.get(operation);

			if (statusName != null)
				if (statusName.getCacheName().equals(name))
					return true;
				else
					return false;

		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static boolean isOperationFinished(String name) {
		try {
			CacheOperations operation = CacheOperations.getStatusName(name);
			CacheStatusName statusName = CACHE_OPERATION_FINISHED
					.get(operation);

			if (statusName != null)
				if (statusName.getCacheName().equals(name))
					return true;
				else
					return false;

		} catch (Exception e) {
			return false;
		}
		return false;
	}
}
