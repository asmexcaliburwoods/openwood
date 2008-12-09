package org.openmim.icq2k;

public interface ICQ2KConstants
{
  /*
    msg-subtype is a BYTE:
     CODE   FORMAT        MEANING
      01    plain         msg
      03    ?             file
      04    url-msg       url
      06    user-msg      authorization request
      07    plain         authorization denied
      08    00010000      authorization given
      0C    user-msg      i added you
      0D    ?             wwpager?
      0E    email-msg     emailExpress
      13    contacts-msg  contacts
      E?    plain         auto-message request
      f2    ?             pc2pc call request //msgFlags=0x00ff (255)
  */
  
  static final int MSGKIND_PLAIN_TEXT = 1;
  static final int MSGKIND_FILE = 3;
  static final int MSGKIND_URL = 4;
  static final int MSGKIND_AUTH_REQUEST = 6;
  static final int MSGKIND_AUTH_DENIED = 7;
  static final int MSGKIND_AUTH_GRANTED = 8;
  static final int MSGKIND_ADDED_TO_CONTACT_LIST = 0xC;
  static final int MSGKIND_WWPAGER = 0xD;
  static final int MSGKIND_EMAIL_EXPRESS = 0xE;
  static final int MSGKIND_CONTACTS = 0x13;
  static final int MSGKIND_PC2PC_CALL = 0xF2;
  
  static final int MSGKIND_AUTO_MESSAGE_REQUEST_MASK = 0xE0;
}