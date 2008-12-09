package org.openmim.irc.driver.dcc;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   DccInputStream.java
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.openmim.irc.driver.IRCClientProtocolScanner;

import squirrel_util.Lang;
public class DccInputStream extends InputStream
{
  private Socket sok;
  private InputStream sokInput;
  private byte[] buf;
  private boolean eof;
  private int count;
  private long expectedFileSize;
  private long filePositionToStartFrom;
  private DataOutputStream dataOutputStream;
  private long totalBytesReceived;
  private int getPos;

public DccInputStream(InetAddress inetaddress, int i, long filePositionToStartFrom, long expectedFileSize) throws IOException
{
  this(inetaddress, i, filePositionToStartFrom, expectedFileSize, 4096);
}
public DccInputStream(InetAddress inetaddress, int i, long filePositionToStartFrom, long expectedfileSize, int j) throws IOException
{
  eof = false;
  count = 0;
  totalBytesReceived = 0L;
  Lang.ASSERT(expectedfileSize >= 0, "DCC sender sent negative file size: " + expectedfileSize);
  this.expectedFileSize = expectedfileSize;
  Lang.ASSERT(filePositionToStartFrom <= expectedfileSize && filePositionToStartFrom >= 0, "filePositionToStartFrom is invalid: " + filePositionToStartFrom);
  this.filePositionToStartFrom = filePositionToStartFrom;
  buf = new byte[j];
  try
  {
//	PrivilegeManager.enablePrivilege("UniversalConnect");
//	PolicyEngine.assertPermission(PermissionID.NETIO);
  }
  catch (Exception exception)
  {
	throw new IOException("Can't create socket because permission not granted: " + exception.getMessage());
  }
  sok = new Socket(inetaddress, i);
  sokInput = sok.getInputStream();
  dataOutputStream = new DataOutputStream(sok.getOutputStream());
}
public synchronized int available() throws IOException
{
  return count;
}
public synchronized void close() throws IOException
{
  sok.close();
}
private void fillBuf() throws IOException
{
  Lang.ASSERT(!eof && count == 0, "(!eof && count == 0)==true violated, count=" + count + ", eof=" + eof);
  if (filePositionToStartFrom + totalBytesReceived >= expectedFileSize)
  {
	eof = true;
	return;
  }
  else
  {
	int i = (int) Math.min(buf.length, expectedFileSize - totalBytesReceived);
	getPos = 0;
	count = sokInput.read(buf, 0, i);
	Lang.ASSERT(count != 0, "dccInput: read() returned 0");
	if (count <= 0)
	  throw new java.io.IOException("Connection broken before the transfer is completed.");
	Lang.ASSERT(count <= expectedFileSize - (filePositionToStartFrom + totalBytesReceived), "(count <= expectedFileSize - (totalBytesReceived+filePositionToStartFrom))==true violated, count=" + count + ", expectedFileSize=" + expectedFileSize + ", totalBytesReceived=" + totalBytesReceived + ", filePositionToStartFrom=" + filePositionToStartFrom);
	totalBytesReceived += count;
	sendAck(filePositionToStartFrom + totalBytesReceived);
	Lang.ASSERT(eof || count > 0 && count <= buf.length, "(eof || (count > 0 && count <= buf.length))==true is violated, count=" + count + ", buf.length=" + buf.length + ", eof=" + eof);
	return;
  }
}
public synchronized int read() throws IOException
{
  if (!eof && count == 0)
	fillBuf();
  if (eof)
	return -1;
  count--;
  int i = buf[getPos++] & 0xff;
  if (getPos >= buf.length)
	getPos = 0;
  return i;
}
public synchronized int read(byte[] abyte0, int i, int j) throws IOException
{
  Lang.ASSERT(j >= 0 && i + j <= buf.length, "pre: len must be >= 0 && <= buf.length-ofs");
  if (!eof && count == 0)
	fillBuf();
  if (eof)
	return -1;
  int k = Math.min(j, count);
  if (k == 0)
  {
	return 0;
  }
  else
  {
	System.arraycopy(buf, getPos, abyte0, i, k);
	getPos += k;
	count -= k;
	return k;
  }
}
private void sendAck(long l) throws IOException
{
  Lang.ASSERT(l > 0L, "sendAck(): arg must be > 0, it is " + l);
  dataOutputStream.write(IRCClientProtocolScanner.convertDccLongToAck(l));
  dataOutputStream.flush();
}
}
