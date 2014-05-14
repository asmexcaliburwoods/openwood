package org.openmim.icq2k;

import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.*;
import org.openmim.icq.util.joe.*;

/**
  Parses and represents a FLAP packet header.
*/
public final class FLAPHeader
{
  public final int channel;
  public final int seqnum;
  public final int data_field_length;
  public final byte[] byteArray;

  public static FLAPHeader extract(byte[] buf, int ofs, int len)
  throws MessagingNetworkException
  {
    if (len < 6) Lang.ASSERT_IN_RANGE(len, 6, Integer.MAX_VALUE, "len");
    byte[] fh = new byte[6];
    System.arraycopy(buf, ofs, fh, 0, 6);
    if (fh[0] != 0x2a)
      MLang.EXPECT_EQUAL(//### remove this expect
        fh[0], 0x2a, "fh[0]", "0x2a ('*')",
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
    return new FLAPHeader(fh);
  }

  public FLAPHeader(byte[] flap_header)
  throws MessagingNetworkException
  {
    if ((flap_header) == null) Lang.ASSERT_NOT_NULL(flap_header, "flap_header");
    if ((flap_header.length) != (6)) Lang.ASSERT_EQUAL(flap_header.length, 6, "flap_header.length", "6");
    //Command Start (byte: 0x2a)
    //IRCChannel ID (byte)
    //Sequence Number (word)
    //Data Field Length (word)
    if (flap_header[0] != 0x2a)
      MLang.EXPECT_EQUAL(
        flap_header[0], 0x2a, "flap_header[0]", "flap header magic ('*')",
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
    channel = 0xffff & (int) flap_header[1];
    seqnum = Session.aimutil_get16(flap_header, 2);
    data_field_length = Session.aimutil_get16(flap_header, 4);
    if (!(data_field_length >= 0 && data_field_length <= 32*1024))
      MLang.EXPECT_FALSE(
        "data field length must be >= 0 && <= 32*1024, but it is "+data_field_length,
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
    byteArray = flap_header;
  }
}
