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
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
