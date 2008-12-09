package org.openmim.transport_simpletcp;

import java.util.*;
import java.io.*;
import org.openmim.icq.util.joe.*;
import org.openmim.*;

final class SocketRegistry
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(SocketRegistry.class.getName());

  private final static boolean DEBUG = Defines.DEBUG && true;
  
  SocketRegistry() {}

  void addSocket(InputStream is, AbstractConsumer c, int timeoutMillis)
  {
    SocketWrapper w = new SocketWrapper(is, c, timeoutMillis);
    synchronized (this)
    {
      if (deferUpdates)
      {
        if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/deferred add: "+w);
        additions.add(w);
      }
      else
      {
        if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/added: "+w);
        wrappers.add(w);
      }
    }
  }

  /** for closed sockets */
  synchronized void remove(SocketWrapper w)
  {
    if (deferUpdates)
    {
      if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/deferred remove: "+w);
      deletions.add(w);
    }
    else
    {
      if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/removed: "+w);
      wrappers.remove(w);
    }
  }

  private final Set wrappers = new HashSet();
  private final Set additions = new HashSet();
  private final Set deletions = new HashSet();

  private boolean deferUpdates = false;

  private final class LockingEnumeration implements Enumeration
  {
    private Iterator nested;

    private final void setNested(Iterator it)
    {
      synchronized (LockingEnumeration.this)
      {
        if (nested != null)
          Lang.ASSERT(nested == null, "`nested' must be null here, but it is `"+it+"'");
        if (it == null)
          Lang.ASSERT_NOT_NULL(it, "it");
        nested = it;
      }
    }

    public final boolean hasMoreElements()
    {
      boolean hasMoreElements;
      synchronized (LockingEnumeration.this)
      {
        if (nested == null) return false;
        hasMoreElements = nested.hasNext();
        if (!hasMoreElements)
        {
          nested = null;
        }
      }

      if (!hasMoreElements)
      {
        synchronized (SocketRegistry.this)
        {
          applyUpdates();
          deferUpdates = false;
          //if (additions.size() != 0) Lang.ASSERT(additions.size() == 0, "additions.size() must be 0, but it is "+additions.size());
          //if (deletions.size() != 0) Lang.ASSERT(deletions.size() == 0, "deletions.size() must be 0, but it is "+deletions.size());
        }
      }

      return hasMoreElements;
    }

    public final Object nextElement()
    {
      synchronized (LockingEnumeration.this)
      {
        if (nested == null) Lang.ASSERT_NOT_NULL(nested, "nested");

        return nested.next();
      }
    }
  }

  private final LockingEnumeration lockingEnumeration = new LockingEnumeration();

  final Enumeration getSocketWrappers()
  {
    synchronized (this)
    {
      //if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/getWrappers");
      if (deferUpdates) Lang.ASSERT(!deferUpdates, "!deferUpdates");
      if (additions.size() != 0) Lang.ASSERT(additions.size() == 0, "additions.size() must be 0, but it is "+additions.size());
      if (deletions.size() != 0) Lang.ASSERT(deletions.size() == 0, "deletions.size() must be 0, but it is "+deletions.size());
      deferUpdates = true;
    }
    lockingEnumeration.setNested(wrappers.iterator());
    return lockingEnumeration;
  }

  private final void applyUpdates()
  {
    Iterator it;
    SocketWrapper w;
    //if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/applyUpdates enter, deferred a/r: "+additions.size()+"/"+deletions.size());
    it = additions.iterator();
    while (it.hasNext())
    {
      w = (SocketWrapper) it.next();
      if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/applyUpdates/added: "+w);
      wrappers.add(w);
    }
    additions.clear();

    it = deletions.iterator();
    while (it.hasNext())
    {
      w = (SocketWrapper) it.next();
      if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/applyUpdates/removed: "+w);
      wrappers.remove(w);
    }
    deletions.clear();
    //if (DEBUG && CAT.isDebugEnabled()) CAT.debug("sr/applyUpdates leave, deferred a/r: "+additions.size()+"/"+deletions.size());
  }
}
