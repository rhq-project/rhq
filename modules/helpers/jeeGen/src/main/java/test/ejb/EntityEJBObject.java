package test.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * Entity bean - remote interface.
 */
public interface EntityEJBObject extends EJBObject {

    Integer getKey() throws RemoteException;

    void setKey(Integer key) throws RemoteException;

    String getName() throws RemoteException;

    void setName(String empName) throws RemoteException;

}
