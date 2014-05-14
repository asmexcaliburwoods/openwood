package org.openmim.messaging_network2.model;

public interface Server {
    ServerBean toBean(IMNetworkBean net);

    public String getHostNameRealServer();

    public String getHostNameRedirD();

    public int getRedirdPort();
}
