package org.openmim.messaging_network2.model;

import org.openmim.messaging_network2.controller.IMNetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationBean {
    private ContactListGroup contactList;
    private List<IMNetworkBean> networksConfigured;
    private List<RoleToDisplayBean> rolesToDisplay;
    private GlobalParameters globalParameters;
    private GlobalIRCParameters globalIRCParameters;

    public List<IMNetworkBean> getNetworksConfigured() {
        return networksConfigured;
    }

    public void setNetworksConfigured(List<IMNetworkBean> networksConfigured) {
        this.networksConfigured = networksConfigured;
        Map<String, List<ServerBean>> networkKeyCanonical2ListOfServersToKeepConnectionWith=
        	new HashMap<String, List<ServerBean>>();
        for (IMNetworkBean bean : networksConfigured) {
            if(bean.getType()== IMNetwork.Type.irc)
                networkKeyCanonical2ListOfServersToKeepConnectionWith.put(
                		bean.getKey().toLowerCase(),((IRCNetworkBean)bean).getServers());
        }
        setNetworkKeyCanonical2ListOfServersToKeepConnectionWith(
        		networkKeyCanonical2ListOfServersToKeepConnectionWith);
    }

    public List<RoleToDisplayBean> getRolesToDisplay() {
        return rolesToDisplay;
    }

    public void setRolesToDisplay(List<RoleToDisplayBean> rolesToDisplay) {
        this.rolesToDisplay = rolesToDisplay;
    }

    private Map<String, List<ServerBean>> networkKeyCanonical2ListOfServersToKeepConnectionWith =
            new HashMap<String, List<ServerBean>>();

    public Map<String, List<ServerBean>> getNetworkKeyCanonical2ListOfServersToKeepConnectionWith() {
        return networkKeyCanonical2ListOfServersToKeepConnectionWith;
    }

    public void setNetworkKeyCanonical2ListOfServersToKeepConnectionWith(Map<String, List<ServerBean>> 
    	networkKeyCanonical2ListOfServersToKeepConnectionWith) {
        this.networkKeyCanonical2ListOfServersToKeepConnectionWith = 
        	networkKeyCanonical2ListOfServersToKeepConnectionWith;
    }

    public GlobalParameters getGlobalParameters() {
        return globalParameters;
    }

    public GlobalIRCParameters getGlobalIRCParameters() {
        return globalIRCParameters;
    }

    public void setGlobalParameters(GlobalParameters globalParameters) {
        this.globalParameters = globalParameters;
    }

    public void setGlobalIRCParameters(GlobalIRCParameters globalIRCParameters) {
        this.globalIRCParameters = globalIRCParameters;
    }

    public ContactListGroup getContactList() {
        return contactList;
    }

    public void setContactList(ContactListGroup contactList) {
        this.contactList = contactList;
    }
}