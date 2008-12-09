package org.openmim.irc.driver.dcc;

import squirrel_util.Lang;
import squirrel_util.Logger;
import squirrel_util.TimeoutExpiryQueue;

import java.io.*;
import java.net.InetAddress;

//
public abstract class DccReceiver extends DCCRequest implements FileReceiveListener
{
  private String senderNickname;
  private String recipient;
  private String incomingFileName;
  private InputStream istr;
  private OutputStream fout;
  private Object lock;
  private String fullFinalFileName;
  private Object dccResumeId;
  private long incomingFileSize;
  private InetAddress senderInetAddress;
  private int senderPort;
  private long bytesReceived;
  public final int STATE_NORMAL_RECEIVE = 0;
  public final int STATE_RESUME_SELECTED = 1;
  public long filePositionToResumeFrom = 0;
  private int state = STATE_NORMAL_RECEIVE;

protected DccReceiver(TimeoutExpiryQueue q, String senderNickname, String s1, String s2, long l, InetAddress inetaddress, int i)
{
  super(q, 0);
  istr = null;
  fout = null;
  lock = new Object();
  bytesReceived = 0L;
  Lang.ASSERT(senderNickname != null, "senderNickname is null");
  Lang.ASSERT(s1 != null, "recipient is null");
  Lang.ASSERT(s2 != null, "suggestedFileName is null");
  Lang.ASSERT(inetaddress != null, "senderInetAddress is null");
  this.senderNickname = senderNickname;
  recipient = s1;
  incomingFileName = s2;
  incomingFileSize = l;
  senderInetAddress = inetaddress;
  senderPort = i;
}
public void doCloseAll()
{
  synchronized (lock)
  {
	if (fout != null)
	  try
	{
	  fout.close();
	  fout = null;
	}
	catch (Exception exception)
	{
	  Logger.printException(exception);
	}
	if (istr != null)
	  try
	{
	  istr.close();
	  istr = null;
	}
	catch (Exception exception1)
	{
	  Logger.printException(exception1);
	}
  }
}
public void doReceiveFile(OutputStream outputstream)
{
  try
  {
	try
	{
	  fout = outputstream;
	  Lang.ASSERT(fout != null, "fout is null");
	  bytesReceived = filePositionToResumeFrom;
	  istr = getSenderInputStream();
	  byte buf[] = new byte[1024];
	  try
	  {
		for (;;)
		{
		  synchronized (lock)
		  {
			if (istr == null)
			  throw new IOException("Canceled.");
			int i = istr.read(buf);
			if (i == -1)
			  break;
			fout.write(buf, 0, i);
			bytesReceived += i;
			onBytesReceived(buf, 0L, i);
		  }
		}
	  }
	  finally
	  {
		doCloseAll();
	  }
	  if (getIncomingFileSize() != getBytesReceived())
		throw new IOException("File size mismatch: file size declared by sender: " + getIncomingFileSize() + ";\r\n" + "actual bytes received: " + bytesReceived);
	  onFileReceiveDone();
	}
	catch (IOException ex1)
	{
	  Logger.printException(ex1);
	  onFileReceiveAborted("" + ex1.getMessage());
	}
	catch (Exception ex2)
	{
	  Logger.printException(ex2);
	  onFileReceiveAborted("(" + ex2.getClass().getName() + ")\r\n" + ex2.getMessage());
	}
  }
  catch (Exception ex)
  {
	unhandledException(ex);
  }
}
public long getBytesReceived()
{
  return bytesReceived;
}
public Object getDccResumeId()
{
  Lang.ASSERT_NOT_NULL(dccResumeId, "dccResumeId");
  return dccResumeId;
}
/**
   * Insert the method's description here. Creation date: (02.10.00 6:28:13)
   * @return long
   */
public long getFilePositionToResumeFrom()
{
  Lang.ASSERT(filePositionToResumeFrom != -1, "can't call getFilePositionToResumeFrom() before setting one.");
  return filePositionToResumeFrom;
}
public String getFullFinalFileName()
{
  return fullFinalFileName;
}
public String getIncomingFileName()
{
  return incomingFileName;
}
public long getIncomingFileSize()
{
  return incomingFileSize;
}
public String getRecipient()
{
  return recipient;
}
public InetAddress getSenderInetAddress()
{
  return senderInetAddress;
}
public InputStream getSenderInputStream() throws IOException
{
  return new DccInputStream(getSenderInetAddress(), getSenderPort(), filePositionToResumeFrom, getIncomingFileSize());
}
public String getSenderNickname()
{
  return senderNickname;
}
public int getSenderPort()
{
  return senderPort;
}
/**
   * Insert the method's description here. Creation date: (01.10.2000 8:13:07)
   * @return int
   */
public int getState()
{
  return state;
}
public static Object makeDccResumeId(String fileSenderNickname, int fileSenderPort)
{
  Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(fileSenderNickname, "fileSenderNickname");
  return fileSenderNickname.trim().toLowerCase() + ":" + fileSenderPort;
}
public abstract void onBytesReceived(byte[] abyte0, long l, long l1);
/** Should asynchronously transfer the rest of file from the sender. */
public void onDccResumeAccepted()
{
  (new Thread()
  {
	public void run()
	{
	  try
	  {
		//RandomAccessFile rf = new RandomAccessFile(getFullFinalFileName(), "rw");
		//rf.seek(getFilePositionToResumeFrom());
		OutputStream ostr = new BufferedOutputStream(new FileOutputStream(getFullFinalFileName(), true), 32 * 1024);
		doReceiveFile(ostr);
	  }
	  catch (Exception ex)
	  {
		unhandledException(ex);
	  }
	}
  }).start();
}
public abstract void onFileReceiveAborted(String s);
public abstract void onFileReceiveDone();
public abstract void onFileReceiveStart();
/**
   * Insert the method's description here. Creation date: (02.10.00 6:28:13)
   * @param newFilePositionToResumeFrom long
   */
public void setFilePositionToResumeFrom(long newFilePositionToResumeFrom)
{
  filePositionToResumeFrom = newFilePositionToResumeFrom;
}
public void setFullFinalFileName(String s)
{
  fullFinalFileName = s;
}
/**
   * Insert the method's description here. Creation date: (01.10.2000 8:13:07)
   * @param newState int
   */
public void setState(int newState)
{
  state = newState;
  if (state == STATE_RESUME_SELECTED)
	dccResumeId = makeDccResumeId(getSenderNickname(), getSenderPort());
}
}