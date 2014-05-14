package org.openmim.wrapper;

import org.openmim.messaging_network.MessagingNetwork;
import org.openmim.wrapper.MessagingNetworkSession;

//
public interface MessagingNetworkWrappable
extends MessagingNetwork
{
  /** can return null, srcLoginId should never be null. */
  MessagingNetworkSession getSession(String srcLoginId);
}
