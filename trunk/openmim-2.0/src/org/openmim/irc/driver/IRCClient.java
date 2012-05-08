package org.openmim.irc.driver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.openmim.mn2.controller.IMNetwork;
import org.openmim.mn2.controller.IRCController;
import org.openmim.mn2.controller.MN2Factory;

import squirrel_util.Lang;
import squirrel_util.Logger;

//
public class IRCClient extends IRCProtocol {
    protected Socket socket;
    private String ircServerAddress;
    private IRCController queryClient;

    public void close() {
        try {
            super.close();
        }
        finally {
            try {
                if (socket != null)
                    socket.close();
            }
            catch (IOException ioexception) {
                Logger.printException(ioexception);
            }
        }
    }

    public String getIrcServerAddress() {
        Lang.ASSERT_NOT_NULL(ircServerAddress, "ircServerAddress");
        return ircServerAddress;
    }

    public IRCController getLocalClient() {
        return queryClient;
    }

    public InetAddress getLocalInetAddress() {
        Socket socket1 = socket;
        if (socket1 == null)
            return null;
        else
            return socket1.getLocalAddress();
    }

    public void init(IMNetwork net,String redirdHost, String realIrcServerHostName, int redirdPort, String nickName, String userName, String loginPassword, String realName, IRCListener irclistener, MN2Factory MN2Factory) //
            throws IOException {
        Lang.ASSERT_NOT_NULL(redirdHost, "redirdHost");
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(loginPassword, "loginPassword");
        System.err.println("ircclient.init: redirdHost=" + redirdHost);
        ircServerAddress = redirdHost;
        irclistener.connecting();
        try {
            socket = new Socket(redirdHost, redirdPort);
        }
        catch (Throwable throwable) {
            System.err.println("connect ex");
            if (throwable.getClass().getName().indexOf("Security") != -1) {
                throw new RuntimeException(throwable);//PrivilegeManager.enablePrivilege("UniversalConnect");
//	  PolicyEngine.assertPermission(PermissionID.NETIO);
            } else {
                if (throwable instanceof IOException)
                    throw (IOException) throwable;
                else {
                    Logger.printException(throwable);
                    throw new RuntimeException("" + throwable);
                }
            }
            //this is only executed if SecurityException is catched
//            socket = new Socket(redirdHost, redirdPort);
        }
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 4096);
        PrintWriter printwriter = new PrintWriter(new BufferedOutputStream(socket.getOutputStream(), 1024), true);
        init(bufferedreader, printwriter, irclistener);
        irclistener.registering();
        register(loginPassword, nickName, userName, realIrcServerHostName, realIrcServerHostName, realName);
        irclistener.connected();
        queryClient = new IRCController(net, nickName, realName, userName, loginPassword, MN2Factory);
    }
}
