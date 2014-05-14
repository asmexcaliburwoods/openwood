package org.openmim.icq.util.Acme/*CloseableInputStream*/;

import org.openmim.*;
import java.io.*;

public class CloseableInputStream extends InputStream implements Closeable
{
  private final InputStream in;
  private boolean closed = false;
  
  public CloseableInputStream(InputStream in)
  {
    if (in == null) throw new NullPointerException();
    this.in = in;
  }
    
  public int read() throws IOException {return in.read();}
  public int read(byte b[]) throws IOException {return in.read(b);}
  public int read(byte b[], int a, int c) throws IOException {return in.read(b, a, c);}
  public long skip(long n) throws IOException {return in.skip(n);}
  public synchronized int available() throws IOException 
  {
    if (closed) throw new IOException("stream closed");
    else return in.available();
  }
  public synchronized boolean isClosed() { return closed; }
  public synchronized void close() throws IOException {closed = true; in.close();}
  public void reset() throws IOException { in.reset(); }
  public boolean markSupported() { return in.markSupported(); }
}