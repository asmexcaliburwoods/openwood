package org.openmim.msn;

import org.openmim.*;
import org.openmim.stuff.Defines;
import org.openmim.wrapper.*;
import org.openmim.icq.utils.*;

public class ContactListItem
implements MessagingNetworkContactListItem
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ContactListItem.class.getName());

  private final String dstLoginId;
  private int statusNative = StatusUtil.NATIVE_STATUS_OFFLINE;
  private long contactStatusLastChangeTimeMillis = 0; //in the year 1970
  private long scheduledStatusChangeSendTimeMillis = Long.MAX_VALUE; //currently no plans to send anything
  private boolean statusObsolete = false;
  private boolean ignored;

  private final Session session;

  public ContactListItem(Session session, String dstLoginId, boolean ignored)
  {
    if ((session) == null) Lang.ASSERT_NOT_NULL(session, "session");
    this.session = session;
    if ((dstLoginId) == null) Lang.ASSERT_NOT_NULL(dstLoginId, "dstLoginId");
    this.dstLoginId = dstLoginId;
    setIgnored(ignored);
  }

  public String getDstLoginId()
  {
    return dstLoginId;
  }

  public boolean isIgnored()
  {
    return ignored;
  }

  public void setIgnored(boolean ignored)
  {
    this.ignored = ignored;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(dstLoginId+" ignored: "+ignored);
  }

  public int getStatusNative()
  { return statusNative; }

  public void setStatusNative(int status)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatusNative to "+StatusUtil.translateStatusNativeToString(status));
    statusNative = status;
    setStatusObsolete(false);
  }

  public void setStatusObsolete(boolean b)
  {
    statusObsolete = b;
  }

  public boolean isStatusObsolete()
  {
    return statusObsolete;
  }

  /**
    The time of the last time when MessagingNetwork impl
    has fired the listeners.statusChanged(...) event.

    @see org.openmim.messaging_network.MessagingNetworkListener
  */
  public long getContactStatusLastChangeTimeMillis()
  { return contactStatusLastChangeTimeMillis; }

  /**
    @see #getContactStatusLastChangeTimeMillis()
  */
  public void setContactStatusLastChangeTimeMillis(long time)
  { contactStatusLastChangeTimeMillis = time; }

  public void setScheduledStatusChangeSendTimeMillis(long time)
  {
    scheduledStatusChangeSendTimeMillis = time;
    session.setScheduledSendStatus(session.isScheduledSendStatus() || (time < Long.MAX_VALUE));
  }

  public long getScheduledStatusChangeSendTimeMillis()
  { return scheduledStatusChangeSendTimeMillis; }
}