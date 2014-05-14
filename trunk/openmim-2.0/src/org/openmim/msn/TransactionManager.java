package org.openmim.msn;

import java.io.*;
import java.util.*;

import org.openmim.messaging_network.MessagingNetworkException;

public final class TransactionManager
{
  protected final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TransactionManager.class.getName());

  private TransactionContext ctx;
  
  public TransactionManager(ServerConnection srv)
  {
    ctx = new TransactionContext(srv);
  }
  
  private final Hashtable trid2trans = new Hashtable();
  
  private Transaction getTransaction(int trid)
  {
    return (Transaction) trid2trans.get(new Integer(trid));
  }

  public final void close(String msg, int cat, int endUserCode)
  {
    synchronized (this)
    {
      Enumeration e = trid2trans.elements();
      while (e.hasMoreElements())
      {
        Transaction t = (Transaction) e.nextElement();
        t.finish(new MessagingNetworkException(msg, cat, endUserCode));
      }
      trid2trans.clear();
    }
  }
  
  public final void start(Transaction t)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    synchronized (this)
    {
      int trid = nextTrID();
      //Lang.ASSERT(getTransaction(trid) == null, "transaction with this id already exists");
      t.init(trid);
      trid2trans.put(new Integer(trid), t);
    }
    boolean success = false;
    try
    {
      t.clientRequest(ctx);
      success = true;
    }
    catch (RuntimeException ex1)
    {
      t.finish(ex1);
      throw ex1;
    }
    catch (IOException ex2)
    {
      t.finish(ex2);
      throw ex2;
    }
    catch (InterruptedException ex3)
    {
      t.finish(ex3);
      throw ex3;
    }
    catch (MessagingNetworkException ex4)
    {
      t.finish(ex4);
      throw ex4;
    }
    finally
    {
      checkFinished(t);
    }
  }
  
  /** Returns true if delivered */
  public final boolean deliver(int trid, String cmd, String args, ErrorInfo ei, Session ses, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    Transaction t;
    synchronized (this)
    {
      t = getTransaction(trid);
    }
    if (t == null) return false;
    try
    {
      if (ei != null)
        t.errorServerResponse(ei, args, ctx, ses, pctx);
      else
        t.serverResponse(cmd, args, ctx, ses, pctx);
    }
    catch (RuntimeException ex1)
    {
      t.finish(ex1);
      throw ex1;
    }
    catch (IOException ex2)
    {
      t.finish(ex2);
      throw ex2;
    }
    catch (InterruptedException ex3)
    {
      t.finish(ex3);
      throw ex3;
    }
    catch (MessagingNetworkException ex4)
    {
      t.finish(ex4);
      throw ex4;
    }
    finally
    {
      checkFinished(t);
    }
    return true;
  }
  
  /** Removes all finished and/or expired transactions */
  public final synchronized void cleanup()
  {
    Enumeration e = trid2trans.elements();
    Vector finished = new Vector(trid2trans.size());
    while (e.hasMoreElements())
    {
      Transaction t = (Transaction) e.nextElement();
      if (t.isFinished() || t.checkTimeout()) finished.addElement(t);
    }
    for (int i = 0; i < finished.size(); i++)
    {
      remove((Transaction) finished.elementAt(i));
    }
  }

  private final void remove(Transaction t)
  {
    trid2trans.remove(new Integer(t.getTrID()));
  }
  
  private final synchronized void checkFinished(Transaction t)
  {
    if (t.isFinished() || t.checkTimeout()) remove(t);
  }

  private int nextTrID = 1; //0 is the predefined "server request" trid.

  public synchronized int nextTrID()
  {
    if (nextTrID == 0) //skip 0
      nextTrID = 1;
    return (nextTrID == Integer.MAX_VALUE
      ? nextTrID = Integer.MIN_VALUE
      : nextTrID++
      );
  }

  public static String trid2string(int trid)
  {
    if (trid >= 0)
      return Integer.toString(trid);
    else
      return Long.toString( ((long) trid) & 0xffffFFFFL);
  }
}
