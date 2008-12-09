package org.openmim.mn;

import org.openmim.icq.util.joe.*;
import org.openmim.Defines;

public class MessagingNetworkException extends Exception
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(MessagingNetworkException.class.getName());

  //reason categories
  public final static int CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_LOGOUT_CALLER = 0;
  public final static int CATEGORY_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER = 1;
  public final static int CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR = 2;
  public final static int CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN = 3;
  public final static int CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR = 4;
  public final static int CATEGORY_STILL_CONNECTED = 5;
  public final static int CATEGORY_NOT_CATEGORIZED = -1;

  //end user error codes
  public final static int ENDUSER_STATUS_CHANGED_UNDEFINED_REASON = 0;
  public final static int ENDUSER_LOGGED_OFF_ON_BEHALF_OF_END_USER = 1;
  public final static int ENDUSER_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER = 2;
  public final static int ENDUSER_SECOND_LOGIN_REQUEST_IGNORED = 3;
  public final static int ENDUSER_LOGGED_OFF_YOU_WERE_ABSENT_FOR_LONG_TIME = 4;
  public final static int ENDUSER_LOGGED_OFF_DUE_TO_ACCOUNT_REMOVAL = 5;
  public final static int ENDUSER_LOGGED_OFF_DUE_TO_ACCOUNT_ADDING_FAILURE = 6;
  public final static int ENDUSER_LOGGED_OFF_DUE_TO_REINITIALIZATION = 7;
  public final static int ENDUSER_LOGGED_OFF_DUE_TO_RECONNECT_EXCEPTION_DURING_SERVER_START = 8;
  public final static int ENDUSER_UNDEFINED = 9;

  public final static int ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR = 20;
  public final static int ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT = 21;
  public final static int ENDUSER_MESSAGING_OPERATION_TIMED_OUT_NOT_LOGGED_OFF = 22;
  public final static int ENDUSER_OPERATION_NOT_SUPPORTED = 23;
  public final static int ENDUSER_LOGGED_OFF_MESSAGING_SERVER_PROBLEMS = 24;
  public final static int ENDUSER_MESSAGING_SERVER_PROBLEMS_NOT_LOGGED_OFF = 25;
  public final static int ENDUSER_LOGGED_OFF_USER_IS_TOO_ACTIVE = 26;
  public final static int ENDUSER_CANNOT_COMPLETE_REQUEST_RECIPIENT_IS_OFFLINE = 27;
  public final static int ENDUSER_TOO_MANY_PARTICIPANTS_IN_THE_ROOM_NOT_LOGGED_OFF = 28;
  public final static int ENDUSER_QUEUE_FULL = 29;
  public final static int ENDUSER_OPERATION_REJECTED_USER_TOO_ACTIVE_LOGGED_OFF = 30;
  public final static int ENDUSER_OPERATION_REJECTED_USER_TOO_ACTIVE_NOT_LOGGED_OFF = 31;
  public final static int ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_LOGGED_OFF = 32;
  public final static int ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_NOT_LOGGED_OFF = 33;

  // , MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR
  // , MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF
  public final static int ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR = 50;
  public final static int ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF = 51;
  public final static int ENDUSER_LOGGED_OFF_ICQ_SERVER_REPORTED_YOU_AS_OFFLINE = 52;
  public final static int ENDUSER_LOGGED_OFF_MESSAGING_SERVER_REPORTED_YOU_AS_OFFLINE = 53;
  public final static int ENDUSER_CANNOT_SEND_MESSAGE_RECIPIENT_IS_OFFLINE = 54;

  public final static int ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN = 60;

  public final static int ENDUSER_INVALID_UIN_SPECIFIED = 100;
  public final static int ENDUSER_INVALID_UIN_SPECIFIED_UIN_CANNOT_START_WITH_ZERO = 101;
  public final static int ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_BE_EMPTY_STRING = 102;
  public final static int ENDUSER_INVALID_UIN_SPECIFIED_MUST_BE_INTEGER_IN_THE_RANGE = 103; //range is 10000...2147483646
  public final static int ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_CONTAIN_WHITESPACE = 104;
  public final static int ENDUSER_INVALID_LOGIN_ID_SPECIFIED_LOGGED_OFF = 105;
  public final static int ENDUSER_INVALID_LOGIN_ID_SPECIFIED_STILL_CONNECTED = 106;

  public final static int ENDUSER_CANNOT_ADD_YOURSELF_TO_CONTACT_LIST = 120;
  public final static int ENDUSER_CANNOT_LOGIN_WITH_YOURSELF_ON_CONTACT_LIST = 121;

  public final static int ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USERINFO = 130;
  public final static int ENDUSER_MESSAGING_SERVER_REPORTS_CANNOT_SEND_MESSAGE = 131;
  public final static int ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USER_SEARCH_RESULTS = 132;

  public final static int ENDUSER_PASSWORD_IS_TOO_LONG = 150;
  public final static int ENDUSER_TEXT_MESSAGE_IS_TOO_LONG = 151;

  public final static int ENDUSER_NETWORK_ERROR_RECONNECTING = 200;

  //end user reasons signaling bugs located in mim
  public final static int ENDUSER_MIM_BUG = 300;
  public final static int ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY = 301;
  public final static int ENDUSER_MIM_BUG_MESSAGE_TEXT_CANNOT_BE_EMPTY = 302;
  public final static int ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE = 303;

  //MessagingNetworkException.
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE = 400;
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE = 401;
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE = 402;
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE = 403;
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE = 404;
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE = 405;
  public final static int ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE = 406;

  public final static int ENDUSER_CANNOT_LOGIN_MESSAGING_SERVER_REPORTS_UNKNOWN_ERROR = 500;
  public final static int ENDUSER_CANNOT_LOGIN_INVALID_OR_NOT_REGISTERED_UIN = 501;
  public final static int ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_UIN = 502;
  public final static int ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_10_OR_20_MINUTES_LATER = 503;
  public final static int ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_1_OR_2_MINUTES_LATER = 504;
  public final static int ENDUSER_CANNOT_LOGIN_WRONG_PASSWORD = 505;
  public final static int ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_LOGIN_ID = 506;
  //public final static int ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_10_OR_20_MINUTES_LATER = 507;
  //public final static int ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_1_OR_2_MINUTES_LATER = 508;

  /**
    ENDUSER_MIM_BUG_UNKNOWN_ERROR marks all unhandled exceptions
    (that have not been handled by the messaging plugin:
    any such an exception represents a messaging plugin BUG.)
  */
  public final static int ENDUSER_MIM_BUG_UNKNOWN_ERROR = -1;

  /**
    ENDUSER_NO_ERROR marks all places where the end user reason codes are not applicable
    (and not reported to an user)
  */
  public final static int ENDUSER_NO_ERROR = -2;




  private final int category;

  private final int endUserReasonCode;

  public MessagingNetworkException(int category, int endUserReasonCode)
  {
    this(null, category, endUserReasonCode);
  }

  public MessagingNetworkException(String s, int category, int endUserReasonCode)
  {
    super(s);
    switch (category)
    {
      case CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_LOGOUT_CALLER :
      case CATEGORY_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER :
      case CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR :
      case CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN :
      case CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR :
      case CATEGORY_STILL_CONNECTED :
      case CATEGORY_NOT_CATEGORIZED :
        this.category = category;
        break;
      default:
        throw new AssertException("invalid category: "+category);
    }
    this.endUserReasonCode = endUserReasonCode;
  }

  public int getLogger()
  {
    return category;
  }

  public int getEndUserReasonCode()
  {
    return endUserReasonCode;
  }

  public static String getEndUserReasonMessage(int endUserReasonCode)
  {
    String m;

    switch (endUserReasonCode)
    {
      //new enduser codes (used in msn plugin)
      case ENDUSER_OPERATION_NOT_SUPPORTED :
        m = "operation not supported";
        break;
      case ENDUSER_UNDEFINED :
        m = "server error 3";
        break;
      case ENDUSER_TOO_MANY_PARTICIPANTS_IN_THE_ROOM_NOT_LOGGED_OFF :
        m = "too many participants in the room: not logged off";
        break;
      case ENDUSER_LOGGED_OFF_MESSAGING_SERVER_REPORTED_YOU_AS_OFFLINE :
        m = "logged off: messaging server reported you as offline";
        break;
      case ENDUSER_CANNOT_SEND_MESSAGE_RECIPIENT_IS_OFFLINE :
        m = "cannot send message: recipient is offline";
        break;
      case ENDUSER_INVALID_LOGIN_ID_SPECIFIED_LOGGED_OFF :
        m = "logged off: invalid login id specified";
        break;
      case ENDUSER_INVALID_LOGIN_ID_SPECIFIED_STILL_CONNECTED :
        m = "invalid login id specified (not logged off)";
        break;
      case ENDUSER_CANNOT_LOGIN_WRONG_PASSWORD :
        m = "cannot login: wrong password";
        break;
      case ENDUSER_LOGGED_OFF_MESSAGING_SERVER_PROBLEMS :
        m = "logged off: messaging server problems";
        break;
      case ENDUSER_MESSAGING_SERVER_PROBLEMS_NOT_LOGGED_OFF :
        m = "messaging server problems (not logged off)";
        break;
      case ENDUSER_LOGGED_OFF_USER_IS_TOO_ACTIVE :
        m = "logged off: user is too active";
        break;
      case ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_LOGIN_ID :
        m = "cannot login: invalid password or login id";
        break;
      case ENDUSER_CANNOT_COMPLETE_REQUEST_RECIPIENT_IS_OFFLINE :
        m = "cannot complete request: your party is offline";
        break;
      //new enduser codes end

      //changed end user codes
      case ENDUSER_MESSAGING_SERVER_REPORTS_CANNOT_SEND_MESSAGE :
        //old line: m = "messaging server reports: generic error sending message";
        m = "messaging server reports: message delivery failed";
        break;
      //end of changed

      //old enduser codes
      case ENDUSER_LOGGED_OFF_ON_BEHALF_OF_END_USER :
        m = "logged off by user request";
        break;
      case ENDUSER_STATUS_CHANGED_UNDEFINED_REASON :
        m = "status changed";
        break;
      case ENDUSER_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER :
        m = "logged off: you logged on from another computer";
        break;
      case ENDUSER_SECOND_LOGIN_REQUEST_IGNORED :
        m = "second login request ignored (not logged off)";
        break;
      case ENDUSER_LOGGED_OFF_YOU_WERE_ABSENT_FOR_LONG_TIME:
        m = "logged off: you were absent for a long time";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_ACCOUNT_REMOVAL:
        m = "logged off: account was removed";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_ACCOUNT_ADDING_FAILURE:
        m = "logged off: unable to add account";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_REINITIALIZATION:
//        m = "logged off: reinitialization";
        m = "";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR :
        m = "logged off: network error";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT :
        m = "logged off: messaging server timeout";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_RECONNECT_EXCEPTION_DURING_SERVER_START:
        m = "logged off: reconnect error during server start";
        break;
      case ENDUSER_OPERATION_REJECTED_USER_TOO_ACTIVE_LOGGED_OFF :
        m = "logged off: user is too active (last operation rejected)";
        break;
      case ENDUSER_OPERATION_REJECTED_USER_TOO_ACTIVE_NOT_LOGGED_OFF :
        m = "operation rejected: user is too active (not logged off)";
        break;
      case ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_LOGGED_OFF :
        m = "logged off: mim server too busy (last operation rejected)";
        break;
      case ENDUSER_OPERATION_REJECTED_SERVER_TOO_BUSY_NOT_LOGGED_OFF :
        m = "operation rejected: mim server too busy (not logged off)";
        break;
      case ENDUSER_MESSAGING_OPERATION_TIMED_OUT_NOT_LOGGED_OFF :
        m = "messaging operation timed out (not logged off)";
        break;
      case ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR :
        m = "logged off: messaging protocol error";
        break;
      case ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF :
        m = "messaging protocol error (not logged off)";
        break;
      case ENDUSER_LOGGED_OFF_ICQ_SERVER_REPORTED_YOU_AS_OFFLINE :
        m = "logged off: messaging server reported you as offline";
        break;
      case ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN :
        m = "logged off: mim server maintenance";
        break;
      case ENDUSER_INVALID_UIN_SPECIFIED :
        m = "bad uin";
        break;
      case ENDUSER_INVALID_UIN_SPECIFIED_UIN_CANNOT_START_WITH_ZERO :
        m = "bad uin: uin cannot start with 0";
        break;
      case ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_BE_EMPTY_STRING :
        m = "bad uin: cannot be empty string";
        break;
      case ENDUSER_INVALID_UIN_SPECIFIED_MUST_BE_INTEGER_IN_THE_RANGE :
        m = "bad uin: must be an integer 10000...2147483646";
        break;
      case ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_CONTAIN_WHITESPACE :
        m = "bad uin: cannot contain whitespace";
        break;
      case ENDUSER_CANNOT_ADD_YOURSELF_TO_CONTACT_LIST :
        m = "cannot add yourself to a contact list";
        break;
      case ENDUSER_CANNOT_LOGIN_WITH_YOURSELF_ON_CONTACT_LIST :
        m = "cannot login with yourself on a contact list";
        break;
      case ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USERINFO :
        m = "messaging server refuses to return the user details";
        break;
      case ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USER_SEARCH_RESULTS :
        m = "messaging server refuses to return the user search results";
        break;
      case ENDUSER_PASSWORD_IS_TOO_LONG :
        m = "password is too long";
        break;
      case ENDUSER_TEXT_MESSAGE_IS_TOO_LONG :
        m = "text message is too long";
        break;
      case ENDUSER_NETWORK_ERROR_RECONNECTING :
        m = "network error: reconnecting, please wait";
        break;
      case ENDUSER_MIM_BUG :
        m = "server error 1";
        break;
      case ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY :
        m = "password cannot be null nor empty";
        break;
      case ENDUSER_MIM_BUG_MESSAGE_TEXT_CANNOT_BE_EMPTY :
        m = "message text cannot be empty";
        break;
      case ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE :
        m = "please login: can't set non-offline status while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE :
        m = "please login: can't add to a contact list while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE :
        m = "please login: can't remove contact list item while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE :
        m = "please login: can't send message while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE :
        m = "please login: can't send contacts while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE :
        m = "please login: can't get userinfo while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE :
        m = "please login: can't search for users while offline";
        break;
      case ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE :
        m = "please login: can't perform this operation while offline";
        break;

      case ENDUSER_CANNOT_LOGIN_MESSAGING_SERVER_REPORTS_UNKNOWN_ERROR :
        m = "cannot login: messaging server reports error";
        break;
      case ENDUSER_CANNOT_LOGIN_INVALID_OR_NOT_REGISTERED_UIN :
        m = "cannot login: invalid or not registered uin";
        break;
      case ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_UIN :
        m = "cannot login: invalid password or uin";
        break;
      case ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_10_OR_20_MINUTES_LATER :
        m = "cannot login: connect rate exceeded: try 10 or 20 minutes later";
        break;
      case ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_1_OR_2_MINUTES_LATER :
        m = "cannot login: connect rate exceeded: try 1 or 2 minutes later";
        break;
      //case ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_10_OR_20_MINUTES_LATER :
      //  m = "cannot login: connect rate exceeded. mim server will reconnect 20 minutes later; please wait";
      //  break;
      //case ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_1_OR_2_MINUTES_LATER :
      //  m = "cannot login: connect rate exceeded. mim server will reconnect 2 minutes later; please wait";
      //  break;
      case ENDUSER_QUEUE_FULL :
        m = "cannot perform operation: queue full; try again later";
        break;

      //ENDUSER_NO_ERROR marks all places where the end user reason codes are not applicable
      //(and not reported to an user)
      case ENDUSER_NO_ERROR :
        m = "no error";
        break;

      default:
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error(
          "exception ignored, converted to ENDUSER_MIM_BUG_UNKNOWN_ERROR",
          new AssertException("invalid endUserReasonCode: " + endUserReasonCode));

      //ENDUSER_MIM_BUG_UNKNOWN_ERROR marks all unhandled exceptions
      //(that have not been handled by the messaging plugin:
      //any such an exception represents a messaging plugin BUG.)
      case ENDUSER_MIM_BUG_UNKNOWN_ERROR :
        m = "server error 2";
        break;
    }
    return m;
  }

  public String getEndUserReasonMessage()
  {
    return getEndUserReasonMessage(endUserReasonCode);
  }

  public String getLoggerMessage()
  {
    return getLoggerMessage(category);
  }

  public static String getLoggerMessage(int category)
  {
    String c;
    switch (category)
    {
      case CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_LOGOUT_CALLER :
        c = "logged off by caller request";
        break;
      case CATEGORY_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER :
        c = "logged off: logged on from another computer";
        break;
      case CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR :
        c = "logged off on behalf of icq server or a protocol violation";
        break;
      case CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN :
        c = "logged off: server maintenance";
        break;
      case CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR :
        c = "logged off: network error";
        break;
      case CATEGORY_STILL_CONNECTED :
        c = "non-fatal error: still connected";
        break;
      case CATEGORY_NOT_CATEGORIZED :
        c = "no category";
        break;
      default:
        c = "BUGGGG: invalid category: "+category;
        break;
    }
    return c;
  }

  public String toString()
  {
    return super.toString() + " [category: " + getLoggerMessage() + ", enduser: " + getEndUserReasonMessage() + "]";
  }

  public final static void throwOperationNotSupported(String operation)
  throws MessagingNetworkException
  {
    throw new MessagingNetworkException
      ("operation \""+operation+"\" not supported",
        CATEGORY_STILL_CONNECTED,
        ENDUSER_OPERATION_NOT_SUPPORTED);
  }
  
  public final void throwCloned()
  throws MessagingNetworkException
  {
    throw new MessagingNetworkException(getMessage(), category, endUserReasonCode);
  }
}
