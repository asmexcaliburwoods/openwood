package org.openmim.icq2k;

import org.openmim.icq.util.*;
import org.openmim.icq.util.joe.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkException;

/**
  Utility class for conversion of integer status values between 
  MessagingNetwork ones and OSCAR protocol ones.
*/
public final class StatusUtil
{
  public final static int OSCAR_STATUS_OFFLINE = -1;
  public final static int OSCAR_STATUS_ONLINE = 0x0000;
  public final static int OSCAR_STATUS_FREE_FOR_CHAT = 0x0020;
  public final static int OSCAR_STATUS_AWAY = 0x0001;
  public final static int OSCAR_STATUS_NA2 = 0x0004;
  public final static int OSCAR_STATUS_NA = 0x0005;
  public final static int OSCAR_STATUS_OCCUPIED2 = 0x0010;
  public final static int OSCAR_STATUS_OCCUPIED = 0x0011;
  public final static int OSCAR_STATUS_DND = 0x0013;
  public final static int OSCAR_STATUS_INVISIBLE = 0x0100;

  private StatusUtil()
  {
  }

  public static void EXPECT_IS_OSCAR_STATUS(int status) throws MessagingNetworkException
  {
    switch (status)
    {
      case OSCAR_STATUS_AWAY :
      case OSCAR_STATUS_DND :
      case OSCAR_STATUS_FREE_FOR_CHAT :
      case OSCAR_STATUS_NA :
      case OSCAR_STATUS_NA2 :
      case OSCAR_STATUS_OCCUPIED :
      case OSCAR_STATUS_OCCUPIED2 :
      case OSCAR_STATUS_OFFLINE :
      case OSCAR_STATUS_ONLINE :
      case OSCAR_STATUS_INVISIBLE :
        return;
    }
    MLang.EXPECT_FALSE(
      HexUtil.toHexString0x(status)+" is not a valid OSCAR status.",
      MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
      MessagingNetworkException.ENDUSER_MIM_BUG);
  }

  public static int translateStatusMimToOscar(int status) throws MessagingNetworkException
  {
    MLang.EXPECT_IS_MIM_STATUS(status, "status");
    int translatedStatus = -1;
    switch (status)
    {
      case MessagingNetwork.STATUS_ONLINE :
        translatedStatus = OSCAR_STATUS_ONLINE;
        break;
      case MessagingNetwork.STATUS_BUSY :
        translatedStatus = OSCAR_STATUS_OCCUPIED;
        break;
      case MessagingNetwork.STATUS_OFFLINE :
        translatedStatus = OSCAR_STATUS_OFFLINE;
        break;
      default :
        Lang.ASSERT_FALSE("invalid mim status: " + HexUtil.toHexString0x(status));
        break;
    }
    return translatedStatus;
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
  public static int translateStatusOscarToMim_self(int status)
  {
    int translatedStatus;
    switch (status)
    {
      case OSCAR_STATUS_DND :
      case OSCAR_STATUS_OCCUPIED :
      case OSCAR_STATUS_OCCUPIED2 :
        translatedStatus = MessagingNetwork.STATUS_BUSY;
        break;
      case OSCAR_STATUS_OFFLINE :
        translatedStatus = MessagingNetwork.STATUS_OFFLINE;
        break;
      /*
      case OSCAR_STATUS_AWAY :
      case OSCAR_STATUS_FREE_FOR_CHAT :
      case OSCAR_STATUS_NA :
      case OSCAR_STATUS_NA2 :
      case OSCAR_STATUS_ONLINE :
      case OSCAR_STATUS_INVISIBLE :
      */
      default :
        translatedStatus = MessagingNetwork.STATUS_ONLINE;
        break;
    }
    return translatedStatus;
  }
  public static int translateStatusOscarToMim_cl_entry(int status)
  {
    int translatedStatus;
    switch (status)
    {
      case OSCAR_STATUS_AWAY :
      case OSCAR_STATUS_NA :
      case OSCAR_STATUS_NA2 :
        translatedStatus = MessagingNetwork.STATUS_BUSY;
        break;
      case OSCAR_STATUS_OFFLINE :
        translatedStatus = MessagingNetwork.STATUS_OFFLINE;
        break;
      /*
      case OSCAR_STATUS_DND :
      case OSCAR_STATUS_FREE_FOR_CHAT :
      case OSCAR_STATUS_OCCUPIED :
      case OSCAR_STATUS_OCCUPIED2 :
      case OSCAR_STATUS_OFFLINE :
      case OSCAR_STATUS_ONLINE :
      case OSCAR_STATUS_INVISIBLE :
      */
      default :
        translatedStatus = MessagingNetwork.STATUS_ONLINE;
        break;
    }
    return translatedStatus;
  }
  public static String translateStatusOscarToString(int status)
  {
    String translatedStatus;
    switch (status)
    {
      case OSCAR_STATUS_AWAY :
        translatedStatus = "OSCAR_STATUS_AWAY";
        break;
      case OSCAR_STATUS_DND :
        translatedStatus = "OSCAR_STATUS_DND";
        break;
      case OSCAR_STATUS_FREE_FOR_CHAT :
        translatedStatus = "OSCAR_STATUS_FREE_FOR_CHAT";
        break;
      case OSCAR_STATUS_NA :
        translatedStatus = "OSCAR_STATUS_NA";
        break;
      case OSCAR_STATUS_NA2 :
        translatedStatus = "OSCAR_STATUS_NA2";
        break;
      case OSCAR_STATUS_OCCUPIED :
        translatedStatus = "OSCAR_STATUS_OCCUPIED";
        break;
      case OSCAR_STATUS_OCCUPIED2 :
        translatedStatus = "OSCAR_STATUS_OCCUPIED2";
        break;
      case OSCAR_STATUS_OFFLINE :
        translatedStatus = "OSCAR_STATUS_OFFLINE";
        break;
      case OSCAR_STATUS_INVISIBLE :
        translatedStatus = "OSCAR_STATUS_INVISIBLE";
        break;
      case OSCAR_STATUS_ONLINE :
        translatedStatus = "OSCAR_STATUS_ONLINE";
        break;
      default :
        translatedStatus = "OSCAR_STATUS_UNKNOWN(value==" + HexUtil.toHexString0x(status) + ")";
        break;
    }
    return translatedStatus;
  }
}
