package org.openmim.irc.driver.dcc;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   DccSender.java
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.openmim.irc.driver.IRCClient;
import org.openmim.irc.driver.IRCClientProtocolScanner;

import squirrel_util.Lang;
import squirrel_util.Logger;
import squirrel_util.TimeoutExpiryQueue;

// Referenced classes of package org.openmim.irc.dcc:
//      DCCRequest, DccOutputStream
public abstract class DccSender extends DCCRequest
{
  private int port;
  private InetAddress addr;
  private String recipient;
  private OutputStream ostr;
  private ServerSocket ss;
  private InputStream fin;
  private Object lock;
  private String fullFinalFileName;
  private long fullFileSize;
  private long filePositionToStart;
  private long bytesSent;

protected DccSender(TimeoutExpiryQueue q)
{
  super(q, 1);
  ostr = null;
  fin = null;
  lock = new Object();
  fullFileSize = -1L;
  bytesSent = 0L;
}
public void doAccepConnections(IRCClient ircclient, InputStream inputstream, String recipient_nick)
{
	try
	{
		Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(recipient_nick, "recipient_nick");
		Lang.ASSERT(inputstream != null, "InputStream (fin) is null");
		Lang.ASSERT(ircclient != null, "IRCClient is null");
		fin = inputstream;
		recipient = recipient_nick;
		try
		{
//			PrivilegeManager.enablePrivilege("UniversalAccept");
//			PrivilegeManager.enablePrivilege("UniversalListen");
//			PolicyEngine.assertPermission(PermissionID.NETIO);
		}
		catch (Exception exception)
		{
			throw new IOException("Can't create listening socket because permission not granted: " + exception.getMessage());
		}
		ss = new ServerSocket(0);
		addr = ircclient.getLocalInetAddress();
		port = ss.getLocalPort();
		this.filePositionToStart = 0;
		ircclient.getDccResumeRegistry().addDccSender(this);
		(new Thread("DCCSEND server on port " + port)
		{
			public void run()
			{
				try
				{
					try
					{
						try
						{
							//ss.setSoTimeout((int) getTimeoutExpiryQueue().getTimeoutMillis() - 1);
							Socket socket = ss.accept();
							//here, after accept() and before doReceive(),
							//the onDccResumeRequested() method
							//can be called by the thread that is
							//processing the irc server input.
							//This method changes filePositionToStart
							//
							Lang.ASSERT_NON_NEGATIVE(filePositionToStart, "filePositionToStart");
							touch();
							socket.setSoTimeout((int) getTimeoutExpiryQueue().getTimeoutMillis() - 1);
							doReceive(socket, filePositionToStart);
						}
						finally
						{
							doCloseAll();
						}
					}
					catch (IOException ioexception)
					{
						Logger.printException(ioexception);
						if (!isExpired())
							onFileSendAborted("" + ioexception.getMessage());
					}
					catch (Exception exception2)
					{
						Logger.printException(exception2);
						if (!isExpired())
							onFileSendAborted("(" + exception2.getClass().getName() + ")\r\n" + exception2.getMessage());
					}
				}
				catch (Exception ex)
				{
					unhandledException(ex);
				}
			}
		}).start();
		ircclient.sendDccCommand(recipient_nick, "SEND " + prepareFileName(fullFinalFileName) + " " + prepareAddr(addr) + " " + port + " " + fullFileSize);
	}
	catch (Exception exception1)
	{
		try
		{
			doCloseAll();
			Logger.printException(exception1);
			onFileSendAborted("(" + exception1.getClass().getName() + ")\r\n" + exception1.getMessage());
		}
		catch (Exception ex)
		{
			unhandledException(ex);
		}
	}
}
public void doCloseAll()
{
	synchronized (lock)
	{
		doCloseSS();
		if (fin != null)
			try
		{
			fin.close();
			fin = null;
		}
		catch (Exception exception)
		{
			Logger.printException(exception);
		}
		if (ostr != null)
		{
			try
			{
				ostr.flush();
			}
			catch (Exception exception1)
			{
				Logger.printException(exception1);
			}
			try
			{
				ostr.close();
				ostr = null;
			}
			catch (Exception exception1)
			{
				Logger.printException(exception1);
			}
		}
	}
}
public void doCloseSS()
{
  try
  {
	if (ss != null)
	  ss.close();
	ss = null;
  }
  catch (Exception exception)
  {
	Logger.printException(exception);
  }
}
private void doReceive(Socket socket, long filePosToStart) throws IOException
{
	touch();
	if (filePosToStart > 0)
	{
		long skipPos = 0;
		while (skipPos < filePosToStart)
		{
			long delta = Math.min(128, filePosToStart - skipPos);
			fin.skip(delta);
			skipPos += delta;
			touch();
		}
	}
	ostr = getReceiverOutputStream(socket, fullFileSize);
	touch();
	byte buf[] = new byte[1024];
	try
	{
		for (;;)
		{
			synchronized (lock)
			{
				if (ostr == null)
					throw new IOException("File sending canceled.");
				int i = (int) Math.min(fullFileSize - bytesSent, fin.read(buf));
				touch();
				if (i == -1 || i == 0)
					break;
				ostr.write(buf, 0, i);
				//ostr.flush();
				bytesSent += i;
				touch();
				onBytesSent(buf, 0L, i);
			}
		}
	}
	finally
	{
		doCloseAll();
	}
	Lang.ASSERT(getFullFileSize() == getBytesSent(), "getFullFileSize() != getBytesSent(): size:" + fullFileSize + ", sent:" + bytesSent);
	onFileSendDone();
}
public void expired()
{
	super.expired();
	doCloseAll();
	onFileSendAborted("Timeout expired");
}
public long getBytesSent()
{
  return bytesSent;
}
public long getFullFileSize()
{
  return fullFileSize;
}
public String getFullFinalFileName()
{
  return fullFinalFileName;
}
public OutputStream getReceiverOutputStream(Socket socket, long l) throws IOException
{
  return new java.io.BufferedOutputStream(new DccOutputStream(socket, l), 8*4096);
}
public String getRecipient()
{
  return recipient;
}
public InetAddress getServerInetAddress()
{
  return addr;
}
public int getServerPort()
{
  return port;
}
public abstract void onBytesSent(byte[] abyte0, long l, long l1);
public void onDccResumeRequested(long filePosition)
{
  Lang.ASSERT(filePosition >= 0 && filePosition <= fullFileSize, "filePosition must be 0.." + fullFileSize + ", but it is " + filePosition);
  this.filePositionToStart = filePosition;
  bytesSent = filePositionToStart;
}
public abstract void onFileSendAborted(String s);
public abstract void onFileSendDone();
public long prepareAddr(InetAddress inetaddress)
{
  return IRCClientProtocolScanner.convertInetAddressToDccLongIp(inetaddress);
}
public String prepareFileName(String s)
{
  String s1 = s;
  int i = s.lastIndexOf(System.getProperty("file.separator"));
  if (i != -1)
	s1 = s.substring(i + 1);
  if (s1.length() == 0)
	s1 = "somefile.ext";
  StringBuffer stringbuffer = new StringBuffer();
  for (StringTokenizer stringtokenizer = new StringTokenizer(s1); stringtokenizer.hasMoreTokens();)
  {
	stringbuffer.append(stringtokenizer.nextToken());
	if (stringtokenizer.hasMoreTokens())
	  stringbuffer.append("_");
  }
  return stringbuffer.toString();
}
public void setFullFileSize(long l)
{
  Lang.ASSERT(l >= 0L, "fileSize < 0: " + l);
  fullFileSize = l;
}
public void setFullFinalFileName(String s)
{
  Lang.ASSERT(s != null, "fileName is null");
  fullFinalFileName = s;
}
}
