package org.openmim.messaging_network2.model;

import java.util.List;

public class IRCNetworkBean extends IMNetworkBean{
    private List<ServerBean> servers;

    public List<ServerBean> getServers() {
        return servers;
    }

    public void setServers(List<ServerBean> servers) {
        this.servers = servers;
    }
}
