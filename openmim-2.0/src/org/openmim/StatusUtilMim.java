package org.openmim;

import org.openmim.icq.util.joe.*;
import org.openmim.mn.MessagingNetwork;

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
