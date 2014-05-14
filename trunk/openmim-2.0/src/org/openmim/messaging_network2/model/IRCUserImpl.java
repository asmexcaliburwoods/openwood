package org.openmim.messaging_network2.model;

import java.beans.PropertyChangeListener;
import java.net.InetAddress;

import com.egplab.utils.Lang;


public class IRCUserImpl implements IRCUser {
    private String activeNick;
    private String userName;
    private String hostName;
    private InetAddress inetAddress;
    private boolean triedResolve = false;

    public IRCUserImpl(String activeNick) {
        setActiveNick(activeNick);
    }

    private String convertNullToStar(String s) {
        return (s == null ? "*" : s);
    }

    public boolean equals(Object o) {
        return o != null && o instanceof User && activeNick.equalsIgnoreCase(((IRCUser) o).getActiveNick());
    }


    /**
     * Returns null if the host name is unknown, or if it cannot be resolved using DNS.
     * Otherwise returns <code>getInetAddress().getHostAddress()</code>.
     */
    public String getHostAddress() {
        InetAddress ia = getInetAddress();
        if (ia == null)
            return null;
        else
            return ia.getHostAddress();
    }

    public String getHostName() {
        return hostName;
    }

    /**
     * Returns InetAddress; can return null if not set
     * or cannot be resolved using hostname given.
     */
    public synchronized InetAddress getInetAddress() {
        if (!triedResolve)
            tryResolve();
        return inetAddress;
    }



    /**
     * Never returns null.
     */
    public String getActiveNick() {
        return activeNick;
    }

    /**
     * Returns null until the declared user name is known.
     */
    public String getUserName() {
        return userName;
    }

    public synchronized void setHostName(String hostName) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(hostName, "hostName");
        this.hostName = hostName;
        triedResolve = false;
    }

    /**
     * Can be set to null.
     */
    private void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public void setActiveNick(String activeNick) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(activeNick, "activeNick");
        this.activeNick = activeNick;
    }

    public String getDisplayName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean canRename() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDisplayName(String displayName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setUserName(java.lang.String userName) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(userName, "userName");
        this.userName = userName;
    }

    public String toString() {
        return getActiveNick() + "!" + //
                convertNullToStar(getUserName()) + "@" + //
                convertNullToStar(getHostName());
    }

    private void tryResolve() {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(hostName, "hostName");
        Lang.ASSERT(!triedResolve, "this should never be called twice for each setHostname()");
        triedResolve = true;
        InetAddress ia = null;
        try {
            org.openmim.irc.driver.IRCProtocol.dbg("resolving \"" + hostName + "\"...");
//            netscape.security.PrivilegeManager.enablePrivilege(NetscapeTarget.UniversalConnect);
//	com.ms.security.PolicyEngine.assertPermission(com.ms.security.PermissionID.NETIO);
            ia = InetAddress.getByName(hostName);
            org.openmim.irc.driver.IRCProtocol.dbg("resolved \"" + hostName + "\": " + ia);
        }
        catch (java.net.UnknownHostException ex) {
            org.openmim.irc.driver.IRCProtocol.dbg("\"" + hostName + "\" cannot be resolved: " + ex.getMessage());
        }
        catch (Exception exception) {
            org.openmim.irc.driver.IRCProtocol.dbg("\"" + hostName + "\" cannot be resolved because permission not granted by user: " + exception.getMessage());
        }
        setInetAddress(ia);
    }
}
