package org.openmim.msn;

import org.openmim.mn.MessagingNetworkException;

import java.io.*;

public final class TransactionContext
{
  private final ServerConnection srv;

  public TransactionContext(ServerConnection srv)
  {
    this.srv = srv;
  }

  /**
    Emits a line to server.
    IOExceptions & InterruptedExceptions are stored in and handled by
    the ServerConnection object.
    IOExceptions & InterruptedExceptions cause the user session shutdown.
  */
  public void post(int trid, String cmd, String args)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    srv.post(trid, cmd, args);
  }
  
  /**
    Emits a MSG to server.  args do not include msgBody length in bytes.
    IOExceptions & InterruptedExceptions are stored in and handled by
    the ServerConnection object.
    IOExceptions & InterruptedExceptions cause the user session shutdown.
  */
  protected final void postMSG(int trid, String args, String msgBody)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    srv.postMSG(trid, args, msgBody);
  }
  
  public final void closeServerConnection(String msg, int cat, int endUserCode)
  {
    srv.close(msg, cat, endUserCode); 
  }
  
  public boolean isNS()
  {
    return srv.isNS();
  }
}
