package org.openmim.icq2k;

import org.openmim.infrastructure.statistics.*;
import org.openmim.messaging_network.MessagingNetworkException;

class Stats
{
  final static String STAT_CAT = "icq";
  
  /*   */ final static Statistics.SimpleCounter STAT_ONLINE_USERS = Statistics.getSimpleCounterInstance(STAT_CAT, "online users");
  
  private final static Statistics.SimpleCounter STAT_TOTAL_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "total errors");
  private final static Statistics.SimpleCounter STAT_NETWORK_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "network errors");
  /*   */ final static Statistics.SimpleCounter STAT_UNKAUTH14_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "unkauth14 errors");
  private final static Statistics.SimpleCounter STAT_UNKAUTH_TOTAL_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "total of unkauth errors");
  private final static Statistics.SimpleCounter STAT_CONNECT_RATE_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "connect rate errors");
  private final static Statistics.SimpleCounter STAT_SENDTEXTMSG_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "send text message errors");
  private final static Statistics.SimpleCounter STAT_SENDNONTEXTMSG_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "send nontext message errors");
  private final static Statistics.SimpleCounter STAT_GETINFOTIMEOUT_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "getinfo timeout errors");
  private final static Statistics.SimpleCounter STAT_GETINFOOTHER_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "getinfo other errors");
  private final static Statistics.SimpleCounter STAT_SEARCH_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "search errors");
  private final static Statistics.SimpleCounter STAT_LOGGEDONFROMANOTHERPC_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "logged on from another pc errors");
  private final static Statistics.SimpleCounter STAT_LOGGEDOFF_BY_USER_REQUEST = Statistics.getSimpleCounterInstance(STAT_CAT, "logged off by user request errors");
  private final static Statistics.SimpleCounter STAT_ERRORS_CAUSED_BY_USER = Statistics.getSimpleCounterInstance(STAT_CAT, "errors caused by user");
  private final static Statistics.SimpleCounter STAT_ERRORS_CAUSED_BY_CORE = Statistics.getSimpleCounterInstance(STAT_CAT, "errors caused by core");
  private final static Statistics.SimpleCounter STAT_MIM_BUGS = Statistics.getSimpleCounterInstance(STAT_CAT, "mim bugs");
  private final static Statistics.SimpleCounter STAT_OTHER_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "other errors");

  final static void report(final int endUserReasonCode)
  {
    STAT_TOTAL_ERRORS.inc();
    
    switch (endUserReasonCode)
    {
       //new enduser codes (used in msn plugin)
       case MessagingNetworkException.ENDUSER_OPERATION_NOT_SUPPORTED :
       case MessagingNetworkException.ENDUSER_TOO_MANY_PARTICIPANTS_IN_THE_ROOM_NOT_LOGGED_OFF :
         break;
       case MessagingNetworkException.ENDUSER_LOGGED_OFF_MESSAGING_SERVER_REPORTED_YOU_AS_OFFLINE :
         STAT_OTHER_ERRORS.inc();
         break;
       case MessagingNetworkException.ENDUSER_CANNOT_SEND_MESSAGE_RECIPIENT_IS_OFFLINE :
         break;
       case MessagingNetworkException.ENDUSER_INVALID_LOGIN_ID_SPECIFIED_LOGGED_OFF :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;
       case MessagingNetworkException.ENDUSER_INVALID_LOGIN_ID_SPECIFIED_STILL_CONNECTED :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;
       case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_WRONG_PASSWORD :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;
       case MessagingNetworkException.ENDUSER_LOGGED_OFF_MESSAGING_SERVER_PROBLEMS :
         STAT_OTHER_ERRORS.inc();
         break;
       case MessagingNetworkException.ENDUSER_MESSAGING_SERVER_PROBLEMS_NOT_LOGGED_OFF :
         STAT_OTHER_ERRORS.inc();
         break;
       case MessagingNetworkException.ENDUSER_LOGGED_OFF_USER_IS_TOO_ACTIVE :
         STAT_OTHER_ERRORS.inc();
         break;
       case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_LOGIN_ID :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;
       case MessagingNetworkException.ENDUSER_CANNOT_COMPLETE_REQUEST_RECIPIENT_IS_OFFLINE :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;
       //new enduser codes end
 
       case MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USER_SEARCH_RESULTS :
         STAT_SEARCH_ERRORS.inc();
       case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;
       case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE :
         STAT_ERRORS_CAUSED_BY_USER.inc();
         break;

///////////////

      case MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_END_USER :
        STAT_LOGGEDOFF_BY_USER_REQUEST.inc();
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_STATUS_CHANGED_UNDEFINED_REASON :
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER :
        STAT_LOGGEDONFROMANOTHERPC_ERRORS.inc();
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_SECOND_LOGIN_REQUEST_IGNORED :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_YOU_WERE_ABSENT_FOR_LONG_TIME:
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_ACCOUNT_REMOVAL:
        STAT_ERRORS_CAUSED_BY_CORE.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_ACCOUNT_ADDING_FAILURE:
        STAT_ERRORS_CAUSED_BY_CORE.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_REINITIALIZATION:
        STAT_ERRORS_CAUSED_BY_CORE.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR :
        STAT_NETWORK_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT :
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_RECONNECT_EXCEPTION_DURING_SERVER_START:
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_MESSAGING_OPERATION_TIMED_OUT_NOT_LOGGED_OFF :
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR :
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF :
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_ICQ_SERVER_REPORTED_YOU_AS_OFFLINE :
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN :
        STAT_ERRORS_CAUSED_BY_CORE.inc();
        break;
      case MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_UIN_CANNOT_START_WITH_ZERO :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_BE_EMPTY_STRING :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_MUST_BE_INTEGER_IN_THE_RANGE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_CONTAIN_WHITESPACE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_CANNOT_ADD_YOURSELF_TO_CONTACT_LIST :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_WITH_YOURSELF_ON_CONTACT_LIST :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USERINFO :
        STAT_GETINFOOTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REPORTS_CANNOT_SEND_MESSAGE :
        STAT_SENDTEXTMSG_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_PASSWORD_IS_TOO_LONG :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_TEXT_MESSAGE_IS_TOO_LONG :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_NETWORK_ERROR_RECONNECTING :
        STAT_OTHER_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_MIM_BUG :
        STAT_MIM_BUGS.inc();
        break;
      case MessagingNetworkException.ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY :
        STAT_MIM_BUGS.inc();
        break;
      case MessagingNetworkException.ENDUSER_MIM_BUG_MESSAGE_TEXT_CANNOT_BE_EMPTY :
        STAT_MIM_BUGS.inc();
        break;
      case MessagingNetworkException.ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE :
        STAT_MIM_BUGS.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_MESSAGING_SERVER_REPORTS_UNKNOWN_ERROR :
        STAT_UNKAUTH_TOTAL_ERRORS.inc();
        break;
      case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_INVALID_OR_NOT_REGISTERED_UIN :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
      case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_UIN :
        STAT_ERRORS_CAUSED_BY_USER.inc();
        break;
        
      case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_1_OR_2_MINUTES_LATER :
      case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_10_OR_20_MINUTES_LATER :
      
      //case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_1_OR_2_MINUTES_LATER :
      //case MessagingNetworkException.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_WILL_RECONNECT_10_OR_20_MINUTES_LATER:
      
        STAT_CONNECT_RATE_ERRORS.inc();
        break;
        
      case MessagingNetworkException.ENDUSER_NO_ERROR :
        //////
        break;

      default:
      case MessagingNetworkException.ENDUSER_MIM_BUG_UNKNOWN_ERROR :
        STAT_MIM_BUGS.inc();
        break;
    }
  }
}