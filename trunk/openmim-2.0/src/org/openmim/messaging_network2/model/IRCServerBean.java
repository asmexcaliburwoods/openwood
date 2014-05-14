package org.openmim.messaging_network2.model;

public class IRCServerBean extends ServerBean{
    private String hostName="";
    private int port=6667;
    private String password="p";

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }
}
