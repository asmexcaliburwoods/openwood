package org.openmim.mn2.controller;

import org.openmim.mn2.model.ServerBean;
import org.openmim.mn2.model.IRCServerBean;
import org.openmim.mn2.model.IMNetworkBean;
import org.openmim.mn2.model.*;

import java.util.List;
import java.util.Set;

public class IRCServerImpl implements Server {
    private String hostNameOfRealServer;
    private String redirdHostName;
    private int redirdPort;
    private String realName;
    private List<String> nickNames;
    private String password;
    private String identdUserName;
    //todo nickserv passwrds

    public IRCServerImpl(String hostNameOfRealServer, String redirdHostName, int redirdPort,
                         String realName, List<String> nickNames, String password,String identdUserName) {
        this.hostNameOfRealServer = hostNameOfRealServer;
        this.redirdHostName = redirdHostName;
        this.redirdPort = redirdPort;
        this.realName = realName;
        this.nickNames = nickNames;
        this.password=password;
        this.identdUserName=identdUserName;
    }

    public String getPassword() {
        return password;
    }

    public String getIdentdUserName() {
        return identdUserName;
    }

    public ServerBean toBean(IMNetworkBean net) {
        IRCServerBean sb=new IRCServerBean();
        sb.setPort(redirdPort);
        sb.setHostName(redirdHostName);
        sb.setImNetwork(net);
        return sb;
    }

    public String getHostNameRealServer() {
        return hostNameOfRealServer;
    }

    public String getHostNameRedirD() {
        return redirdHostName;
    }

    public int getRedirdPort() {
        return redirdPort;
    }

    public String getRealName() {
        return realName;
    }

    public List<String> getNickNames() {
        return nickNames;
    }
}
