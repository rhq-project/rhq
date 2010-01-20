/**
 * 
 */
package org.rhq.enterprise.server.plugin.pc.content;

/**
 * @author mmccune
 * 
 */
public class ThreadUtil {
	
	public static void checkInterrupted() throws InterruptedException {
		Thread cthread = Thread.currentThread();
		System.out.println("ThreadInterrupt : [" + cthread.getName() + "]");
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

	}

}
