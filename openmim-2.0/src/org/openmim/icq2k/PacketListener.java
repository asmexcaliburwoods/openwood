package org.openmim.icq2k;

import org.openmim.messaging_network.MessagingNetworkException;

public interface PacketListener
{
  void packetSent();
  void packetCanceled(MessagingNetworkException ex);
}