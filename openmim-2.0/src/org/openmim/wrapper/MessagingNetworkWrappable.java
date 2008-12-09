package org.openmim.wrapper;

import org.openmim.mn.MessagingNetwork;
import org.openmim.wrapper.MessagingNetworkSession;

//
public interface MessagingNetworkWrappable
extends MessagingNetwork
{
  /** can return null, srcLoginId should never be null. */
  MessagingNetworkSession getSession(String srcLoginId);
}
