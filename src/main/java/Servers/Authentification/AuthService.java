package Servers.Authentification;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {
    boolean userExists(String username) throws RemoteException;
    boolean createUser(String username,String email, String password) throws RemoteException;
    boolean deleteUser(String username) throws RemoteException;
    boolean updatePassword(String username, String newPassword) throws RemoteException;
    boolean login(String username, String password)throws RemoteException;
}
