package org.openmim.icq2k;

import org.openmim.*;
import org.openmim.stuff.Defines;
import org.openmim.wrapper.*;
import org.openmim.icq.utils.*;

public class ContactListItem
implements MessagingNetworkContactListItem
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ContactListItem.class.getName());
  private final Session session;
  private final String dstLoginId;
  private boolean statusObsolete = false;

  public ContactListItem(Session session, String dstLoginId)
  {
    if ((session) == null) Lang.ASSERT_NOT_NULL(session, "session");
    this.session = session;
    if ((dstLoginId) == null) Lang.ASSERT_NOT_NULL(dstLoginId, "dstLoginId");
    this.dstLoginId = dstLoginId;
  }

  public String getDstLoginId()
  {
    return dstLoginId;
  }


  private int statusOscar = StatusUtil.OSCAR_STATUS_OFFLINE;

  public int getStatusOscar()
  { return statusOscar; }

  public void setStatusOscar(int status)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatusOscar to "+StatusUtil.translateStatusOscarToString(status));
    statusOscar = status;
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


  private long contactStatusLastChangeTimeMillis = 0; //in the year 1970

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


  private long scheduledStatusChangeSendTimeMillis = Long.MAX_VALUE; //currently no plans to send anything

  public void setScheduledStatusChangeSendTimeMillis(long time)
  {
    scheduledStatusChangeSendTimeMillis = time;

    if (time < Long.MAX_VALUE)
    {
      //run sched task
      session.runAt(
        time,
        new MessagingTask("delayed status update", session)
        {
          public void run() throws Exception
          {
            session.sendDeferredStatus(ContactListItem.this);
          }
        });
    }
  }

  public long getScheduledStatusChangeSendTimeMillis()
  { return scheduledStatusChangeSendTimeMillis; }
}
