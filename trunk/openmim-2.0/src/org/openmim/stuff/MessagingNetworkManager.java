package org.openmim.stuff;

import org.openmim.messaging_network.MessagingNetwork;
import org.openmim.messaging_network.MessagingNetworkListener;

import java.util.Collection;

public interface MessagingNetworkManager {
  void init(Collection<String> classNames);
  void deinit();

  /** not implemented */
  void registerMessagingNetwork( MessagingNetwork network);
  /** not implemented */
  void unregisterMessagingNetwork( MessagingNetwork network );
  /** Adds listener to all networks */
  void addMessagingNetworkListener( MessagingNetworkListener l);
  /** Adds listener to all networks */
  void removeMessagingNetworkListener( MessagingNetworkListener l);
}