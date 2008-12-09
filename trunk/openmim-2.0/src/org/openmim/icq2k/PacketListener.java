package org.openmim.icq2k;

import org.openmim.mn.MessagingNetworkException;

public interface PacketListener
{
  void packetSent();
  void packetCanceled(MessagingNetworkException ex);
}