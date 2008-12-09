package org.openmim;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkListener;

import java.util.ArrayList;
import java.util.Collection;


public class SimpleMessagingNetworkManager implements MessagingNetworkManager {
  private static final Logger CAT = Logger.getLogger(SimpleMessagingNetworkManager.class.getName());
  private final Collection<MessagingNetwork> networks=new ArrayList<MessagingNetwork>(5);

  public SimpleMessagingNetworkManager() {
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Using messaging network manager: " + this.getClass().getName());
  }

  public void init(Collection<String> classNames) {
    if(networks == null) {
      if(CAT.isDebugEnabled())
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("SimpleMessagingNetworkManager->init()");
      if(classNames == null) {
        String msg = "Invalid classNames in init : null";
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error(msg);
        throw new NullPointerException( msg );
      }

        for (String className : classNames) {
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Creating network plugin for : " + className);
            if (className == null) {
                String msg = "Invalid network class name";
                if (Defines.DEBUG && CAT.isEnabledFor(Level.ERROR)) CAT.error(msg);
                throw new RuntimeException(msg);
            }

            try {
                networks.add((MessagingNetwork) Class.forName(className).newInstance());
            } catch (Exception ex) {
                String msg = "Unable to instantiate messaging network : " + className;
                if (Defines.DEBUG && CAT.isEnabledFor(Level.ERROR)) CAT.error(msg, ex);
                throw new RuntimeException(msg);
            }
        }
    }
      for (MessagingNetwork o : networks) o.init();
  }
  public void deinit() {
      for (MessagingNetwork o : networks) o.deinit();
  }

  public void registerMessagingNetwork( MessagingNetwork network) {
    throw new RuntimeException("Not implemented yet");
  }

  public void unregisterMessagingNetwork( MessagingNetwork network ) {
    throw new RuntimeException("Not implemented yet");
  }
  public void addMessagingNetworkListener( MessagingNetworkListener l) {
      for (MessagingNetwork key : networks) {
          if (key != null) {
              key.addMessagingNetworkListener(l);
          }
      }
  }
  public void removeMessagingNetworkListener( MessagingNetworkListener l) {
      for (MessagingNetwork network : networks) {
          if (network != null) {
              try {
                  network.removeMessagingNetworkListener(l);
              } catch (Exception e) {
                  if (Defines.DEBUG && CAT.isEnabledFor(Level.ERROR))
                      CAT.error("Error removing messaging network listener; error ignored", e);
              }
          }
      }
  }
}