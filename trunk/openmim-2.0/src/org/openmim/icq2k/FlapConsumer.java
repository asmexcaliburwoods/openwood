package org.openmim.icq2k;

import java.io.*;
import java.net.*;
import java.util.*;

import org.openmim.*;
import org.openmim.icq.utils.*;

public abstract class FlapConsumer
{
  //private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(FlapConsumer.class.getName());

  //int consumed
  //int emptyPos
  //byte[] buf
  //protected byte[] buf = null;

  /**
    Here, bytes in <code>b</code>, at positions from <code>ofs</code> to <code>ofs+len-1</code>
    contains one FLAP packet body.
    If incoming connection contains an invalid FLAP packet,
    the connection (and user session) breaks.
  */
  public abstract void parse(final int flapChannel, final byte[] flapBody, final int ofs, final int len) throws Exception;

  /* copies & saves the (little) rest of incoming data which cannot be parsed until more data received */
  //public final void keep(byte[] buf, int ofs, int len);

  /*
  public final byte[] getBuffer() { return buf = getBufferFromPool(); }
  protected final releaseBuffer()
  {
    ...
    buf = null;
  }
  */
  
  public abstract boolean isBosConn();
}
