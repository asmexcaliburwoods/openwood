package org.openmim.mn2.model;

public interface Server {
    ServerBean toBean(IMNetworkBean net);

    public String getHostNameRealServer();

    public String getHostNameRedirD();

    public int getRedirdPort();
}
