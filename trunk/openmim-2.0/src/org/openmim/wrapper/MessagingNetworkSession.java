package org.openmim.wrapper;

import java.util.*;
import java.io.*;
import java.net.*;

import org.openmim.*;
import org.openmim.icq.utils.*;

//
public interface MessagingNetworkSession
{
  /** 
    Should never return null for contact list entries.
    Returning non-null means that the ContactListItem is in the contact list
    (possibly, with the offline status).
    Returning null means that the ContactListItem is not in the contact list.
  */
  public MessagingNetworkContactListItem getContactListItem(String dstLoginId);
  public Object getFireStatusLock();
}