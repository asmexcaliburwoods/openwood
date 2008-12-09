package org.openmim.wrapper;

//
public interface MessagingNetworkContactListItem
{
  /**
    The time of the last time when MessagingNetwork impl 
    has fired the listeners.statusChanged (...) event.

    @see org.openmim.mn.MessagingNetworkListener
  */
  public long getContactStatusLastChangeTimeMillis();

  /**
    @see #getContactStatusLastChangeTimeMillis()
  */
  public void setContactStatusLastChangeTimeMillis(long time);

  public void setScheduledStatusChangeSendTimeMillis(long time);
  public long getScheduledStatusChangeSendTimeMillis();
}