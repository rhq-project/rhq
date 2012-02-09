package test.ejb;

import java.rmi.RemoteException;
import javax.ejb.EJBObject;

/**
 * Stateless session bean - remote interface.
 */
public interface StatelessSessionEJBObject extends EJBObject {

    String echo(String string) throws RemoteException;

}
