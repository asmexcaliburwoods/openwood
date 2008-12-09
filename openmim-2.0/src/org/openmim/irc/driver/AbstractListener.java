package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   AbstractListener.java
import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;
import org.openmim.irc.driver.dcc.*;
import org.openmim.mn2.model.User;
import squirrel_util.Logger;
//import org.openmim.irc.driver.ircz.MuteListItem;

//
public abstract class AbstractListener implements IRCListener
{
  protected IRCClient protocolHandler;

public AbstractListener()
{
}
public void actionMessageReceived(User c, String s1, String s2)
{
}
public abstract void cannotJoinChannel(String s, String s1) throws IOException;
public abstract void connected() throws IOException;
public abstract void connecting() throws IOException;
public abstract void disconnected() throws IOException;
public IRCClient getProtocolHandler()
{
  return protocolHandler;
}
public void handleBanListEnd(String s) throws IOException
{
}
public void handleBanListItem(String s, String s1) throws IOException
{
}
public void handleException(Throwable throwable)
{
  Logger.printException(throwable);
}
/**
 * handleIsonReply method comment.
 */
public void handleIsonReply(java.lang.String serverAddr, java.lang.String[] nicksOnline) throws java.io.IOException
{
}
public void handleListEnd()
{
}
public void handleListItem(String s, int i, String s1)
{
}
public void handleListStart()
{
}
public void handleModeChangeRaw(String sender, String s, String s2, Vector v)
{
}
public void handleMuteListEnd(String channelName) throws IOException
{
}
//public void handleMuteListItem(MuteListItem item) throws IOException
//{
//}
public abstract void handleTopic(String s, String s1);
public void handleWhoisChannelsOn(String s, String s1)
{
}
public void handleWhoisEnd(String s, String s1)
{
}
public void handleWhoisIdleTime(String s, int i, String s1)
{
}
public void handleWhoisOperator(String s, String s1)
{
}
public void handleWhoisServer(String s, String s1, String s2)
{
}
public void handleWhoisUser(String s, String s1, String s2, String s3)
{
}
public abstract void invalidNickName(String s, String s1) throws IOException;
public boolean isIrczMuteSupported()
{
  return false;
}
public abstract void join(String s) throws IOException;
public abstract void kick(String s, String s1, String s2, String s3) throws IOException;
public abstract void namReplyAddNick(String s, char c) throws IOException;
public abstract void namReplyFinish() throws IOException;
public abstract void namReplyStart(String s) throws IOException;
public void nickChange(String oldNick, String newNick) throws IOException
{
  String localClientNick = getProtocolHandler().getNick();
  if (localClientNick.equalsIgnoreCase(oldNick))
	getProtocolHandler().setNick(newNick);
}
public DccReceiver onCreateDccReceiver(User c, String s1, String s2, long l, InetAddress inetaddress, int i)
{
  return null;
}
/** onDccResumeAccepted method comment. */
public void onDccResumeAccepted(String nickFrom, int port, long filePos)
{
}
/** onDccResumeRequest method comment. */
public void onDccResumeRequest(String nickFrom, int port, long filePos) throws IOException
{
}
public FileReceiveListener onGetFileReceiveListener()
{
  return null;
}
public void onInitialTopic(String s, boolean flag, String s1)
{
  System.err.println("[" + s + "] " + flag + " [" + s1 + "]");
  handleTopic(s, s1);
}
public void ping(String s)
{
  try
  {
	protocolHandler.pong(s);
  }
  catch (Exception exception)
  {
	handleException(exception);
  }
}
public abstract void quit(String s, String s1) throws IOException;
public abstract void registering() throws IOException;
/**
 * setBanned method comment.
 */
public void setBanned(java.lang.String senderSpec, java.lang.String channelName, java.lang.String banMask, boolean isModeOn)
{
}
/**
 * setInviteOnly method comment.
 */
public void setInviteOnly(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
}
public void setIrczMuteSupported()
{
}
/**
 * setKeyOff method comment.
 */
public void setKeyOff(java.lang.String senderSpec, java.lang.String channelName)
{
}
/**
 * setKeyOn method comment.
 */
public void setKeyOn(java.lang.String senderSpec, java.lang.String channelName, java.lang.String key)
{
}
/**
 * setLimitOff method comment.
 */
public void setLimitOff(java.lang.String senderSpec, java.lang.String channelName)
{
}
/**
 * setLimitOn method comment.
 */
public void setLimitOn(java.lang.String senderSpec, java.lang.String channelName, int limit)
{
}
/**
 * setModerated method comment.
 */
public void setModerated(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
}
/**
 * setMuted method comment.
 */
public void setMuted(java.lang.String senderSpec, java.lang.String channelName, java.lang.String muteMask, boolean isModeOn)
{
}
/**
 * setNoExternalMessages method comment.
 */
public void setNoExternalMessages(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
}
/**
 * setOnlyOpsChangeTopic method comment.
 */
public void setOnlyOpsChangeTopic(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
}
/**
 * setOperator method comment.
 */
public void setOperator(java.lang.String senderSpec, java.lang.String channelName, java.lang.String nickNameAffected, boolean isModeOn)
{
}
/**
 * setPrivate method comment.
 */
public void setPrivate(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
}
public void setProtocolHandler(IRCClient ircprotocol)
{
  protocolHandler = ircprotocol;
}
/**
 * setSecret method comment.
 */
public void setSecret(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
}
/**
 * setVoice method comment.
 */
public void setVoice(java.lang.String senderSpec, java.lang.String channelName, java.lang.String nickNameAffected, boolean isModeOn)
{
}
/**
 * shouldCreateQuery method comment.
 * @return boolean
 * @param userFrom org.openmim.irc.mvc_impl.model.User_
 * @param textMessage java.lang.String
 */
public boolean shouldCreateQuery(User userFrom, java.lang.String textMessage)
{
  return true;
}
public abstract void textMessageReceived(User c, String s1, String s2) throws IOException;
public abstract void unhandledCommand(IRCMessage ircmessage) throws IOException;
public void welcome(String newNick, String msg) throws IOException
{
  String nick = getProtocolHandler().getNick();
  if (!nick.equalsIgnoreCase(newNick))
	nickChange(nick, newNick);
}
}
