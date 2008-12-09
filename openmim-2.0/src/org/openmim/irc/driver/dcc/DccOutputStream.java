package org.openmim.irc.driver.dcc;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   DccOutputStream.java
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import org.openmim.irc.driver.IRCClientProtocolScanner;
import squirrel_util.Lang;
import squirrel_util.Logger;

public class DccOutputStream extends OutputStream
{
  private Socket sok;
  private OutputStream sokOutput;
  private byte[] buf;
  private boolean eof;
  private int count;
  private DataInputStream dataInputStream;
  private long requestedContentLength;

public DccOutputStream(Socket socket, long l) throws IOException
{
  eof = false;
  count = 0;
  Lang.ASSERT(l >= 0L, "requestedContentLength < 0");
  Lang.ASSERT(socket != null, "socket is null");
  sok = socket;
  requestedContentLength = l;
  sokOutput = sok.getOutputStream();
  dataInputStream = new DataInputStream(sok.getInputStream());
}
public synchronized void close() throws IOException
{
	try
	{
		super.close();
	}
	catch (Exception exception)
	{
		Logger.printException(exception);
	}
	try
	{
		skipAllPendingAcks();
	}
	catch (Exception exception)
	{
		Logger.printException(exception);
	}
	sok.close();
}
private boolean isAckAvailable() throws IOException
{
  return dataInputStream.available() >= 4;
}
private long readAck() throws IOException
{
  byte abyte0[] = new byte[4];
  if (dataInputStream.read(abyte0) == -1)
	throw new IOException("Unexpected end of file");
  else
	return IRCClientProtocolScanner.convertDccAckToLong(abyte0);
}
private void skipAllPendingAcks() throws IOException
{
  for (; isAckAvailable(); readAck());
}
public synchronized void write(byte[] abyte0, int i, int j) throws IOException
{
  Lang.ASSERT(j >= 0 && i + j <= abyte0.length, "pre: len must be >= 0 && <= buf.length-ofs");
  int k = j;
  int l = i;
  int i1;
  for (; k > 0; k -= i1)
  {
	i1 = Math.min(k, 4096);
	Lang.ASSERT(i1 > 0, "chunk must be > 0");
	sokOutput.write(abyte0, l, i1);
	readAck();
	l += i1;
  }
  Lang.ASSERT(k == 0, "post: rest must be 0");
}
public synchronized void write(int i) throws IOException
{
  sokOutput.write(i);
  readAck();
}
}
