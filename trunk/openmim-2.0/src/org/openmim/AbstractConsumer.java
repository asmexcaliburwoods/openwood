package org.openmim;

import java.io.*;
import java.net.*;
import java.util.*;
import org.openmim.icq.util.joe.*;

/**
  The normal lifetime would consist of zero or more usage loops.
  <p>
  Each usage loop should consist of:
  <ol>
    <li>lockBuffer() call;
    <li>several deliver() calls (zero or more);
    <li>unlockBuffer() call.
  </ol>
  <p>
  lockBuffer() does a single call to allocateBuffer(), and unlockBuffer() does a single call to deallocateBuffer().
  <p>
  Implementations provide concrete allocateBuffer(), deallocateBuffer(), and deliver() methods.
*/
public abstract class AbstractConsumer
{
  private final static org.apache.log4j.Logger CAT =
    org.apache.log4j.Logger.getLogger(AbstractConsumer.class.getName());

  private final static boolean DEBUGLOCKS = true;

  private Thread lockingThread = null;

  protected AbstractConsumer()
  {
  }
  
  private final byte[] lockBuffer() throws InterruptedException
  {
    Thread t;
    final Thread currentThread = Thread.currentThread();
    synchronized (this)
    {
      for (;;)
      {
        t = this.lockingThread;
        if (t == null)
        {
          this.lockingThread = currentThread;
          if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE && DEBUGLOCKS) CAT.debug(this+" locked by thread "+currentThread, new Exception("dumpstack"));
          return allocateBuffer();
        }
        else
        {
          if (t.equals(currentThread)) throw new AssertException("not reenterable");
          if (Thread.interrupted()) throw new InterruptedException();
          wait();
        }
      }
    }
  }
    
  private final void unlockBuffer(byte[] buffer)
  {
    Thread t;
    final Thread currentThread = Thread.currentThread();
    synchronized (this)
    {
      t = this.lockingThread;
      if (t == null) 
        throw new AssertException("unlock of a consumer which is not locked");
      if (!t.equals(currentThread)) 
        throw new AssertException("attempt to unlock a consumer locked by another thread (another thread=="+t+")");
      deallocateBuffer(buffer);
      this.lockingThread = null;
      notify();
      if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE && DEBUGLOCKS) CAT.debug(this+" unlocked by thread "+currentThread, new Exception("dumpstack"));
    }
  }

  protected abstract byte[] allocateBuffer() throws InterruptedException;
  protected abstract void deallocateBuffer(byte[] b);
  protected abstract void deliver(byte[] b, int ofs, int len);

  public abstract void connectionClosed();
  
  /** @deprecated */  
  public final void wrote(int len) {}
  
  public final void consume(final InputStream is, int datalen, long abortTimeMillis) 
  throws IOException
  {
    if (datalen <= 0 || datalen >= 1024*64) 
    {
      AssertException ex = new AssertException("datalen is out of range: "+datalen);
//WD1###       Log log = new Log("I:/datalen-oorange.log");
//WD1###       log.log(""+this, ex);
//WD1###       log.close();
      throw ex;
    }

    //critical section start
    byte[] b = null;
    try
    {
      b = lockBuffer(); 
      int ofs;
    
      for (;;)
      {
        ofs = 0;

        //fill the buffer with data, as much as possible        
        for (;;)
        {
          if (Thread.interrupted()) throw new InterruptedIOException();
          
          int toread = b.length - ofs;
          if (toread <= 0) break;
          if (toread > datalen) toread = datalen;
          if (toread <= 0) break;

          int read = -2;
          synchronized (b)
          {          
            read = is.read(b, ofs, toread);
            if (read < 0) throw new IOException("socket closed");
            if (read == 0) { Thread.yield(); continue; }
            if (Defines.DEBUG && Defines.DEBUG_FULL_DUMPS) 
              Log4jUtil.dump(CAT, "ac: recv "+read+" bytes from "+is, b, ofs, read, "");
            //WD1### debugVerify(b, ofs, read);
          }
          ofs += read;
          datalen -= read;
          checkTimedOut(is, datalen, abortTimeMillis);
        }
        
        deliver(b, 0, ofs);
        if (datalen <= 0) return;
      }
    }
    catch (InterruptedException ex)
    {
      throw new InterruptedIOException();
    }
    finally
    {
      //critical section end
      if (b != null) unlockBuffer(b);
    }
  }
  
//WD1###   protected abstract void debugVerify(byte[] b, int ofs, int len);

  public synchronized final void consume(byte[] b0, int ofs0, int datalen0) 
  throws IOException
  {
    if (datalen0 <= 0) 
    {
      AssertException ex = new AssertException("datalen0 is out of range: "+datalen0);
//WD1###       Log log = new Log("I:/datalen-oorange.log");
//WD1###       log.log(""+this, ex);
//WD1###       log.close();
      throw ex;
    }
    deliver(b0, ofs0, datalen0);
  }
  
  private final void checkTimedOut(InputStream is, int datalen, long abortTimeMillis) throws IOException
  {
    if (System.currentTimeMillis() >= abortTimeMillis)
    {
//WD1###       Log log = new Log("I:/timed-out.log");
//WD1###       log.log(""+this+": timed-out, datalen: "+datalen, new Exception("dumpstack"));
//WD1###       log.close();
      skip(is, datalen);
      throw new IOException("timed out");
    }
  }

  private static final void skip(InputStream is, int datalen) throws IOException
  {
    try 
    {
      for (;;) 
      {
        if (datalen <= 0) break;
        int read = (int) is.skip(datalen);
        if (read < 0) throw new IOException("socket closed");
        if (read == 0) { Thread.sleep(10); continue; }
        datalen -= read;
      }
    } 
    catch (InterruptedException ex) 
    { 
      throw new InterruptedIOException(); 
    }
  }
}