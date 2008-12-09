package org.openmim.transport_simpletcp;

import java.io.*;

import org.openmim.*;

public final class SimpleTcp
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(SimpleTcp.class.getName());

  private Dispatcher dispatcher;
  private SocketRegistry sr;
  private final int maxThreads;
  private final int optThreads;

  public SimpleTcp(int maxThreads, int optimumThreads)
  {
    this.maxThreads = maxThreads;
    this.optThreads = optimumThreads;
  }

  public synchronized void init()
  {
    sr = new SocketRegistry();
    dispatcher = new Dispatcher(sr, maxThreads, optThreads);
    dispatcher.start();
  }

  public synchronized void deinit()
  {
    dispatcher.interrupt();
    dispatcher = null;
    sr = null;
  }

  public synchronized void addSocket(InputStream is, AbstractConsumer c, int timeoutMillis)
  {
    if (sr == null) return;
    sr.addSocket(is, c, timeoutMillis);
  }
}
