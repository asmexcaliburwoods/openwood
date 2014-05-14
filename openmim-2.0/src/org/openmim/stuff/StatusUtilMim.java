package org.openmim.stuff;

import org.openmim.icq.utils.*;
import org.openmim.messaging_network.MessagingNetwork;

/** Utility class for conversion of Mim integer status values to String. */                                 
public final class StatusUtilMim
{
  private StatusUtilMim()
  {
  }
  
  public static String translateStatusMimToString(int status)
  {
    String translatedStatus;
    switch (status)
    {
      case MessagingNetwork.STATUS_ONLINE :
        translatedStatus = "MIM_STATUS_ONLINE";
        break;
      case MessagingNetwork.STATUS_BUSY :
        translatedStatus = "MIM_STATUS_BUSY";
        break;
      case MessagingNetwork.STATUS_OFFLINE :
        translatedStatus = "MIM_STATUS_OFFLINE";
        break;
      default :
        translatedStatus = "MIM_STATUS_INVALID(value=="+HexUtil.toHexString0x(status)+")";
        break;
    }
    return translatedStatus;
  }
}
