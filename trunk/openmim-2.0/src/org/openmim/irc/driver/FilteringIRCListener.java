package org.openmim.irc.driver;

import org.openmim.irc.driver.dcc.DccReceiver;
import org.openmim.messaging_network2.model.User;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;

//
public class FilteringIRCListener implements IRCListener
{
  private IgnoreList ignoreList;
  private IRCListener coreIrcListener;

public FilteringIRCListener(IRCListener irclistener, IgnoreList ignorelist)
{
  coreIrcListener = irclistener;
  ignoreList = ignorelist;
}
public void actionMessageReceived(User c, String s1, String s2)
{
  if (!isIgnored(c))
	getCoreIrcListener().actionMessageReceived(c, s1, s2);
}
public void cannotJoinChannel(String s, String s1) throws IOException
{
  getCoreIrcListener().cannotJoinChannel(s, s1);
}
public void connected() throws IOException
{
  getCoreIrcListener().connected();
}
public void connecting() throws IOException
{
  getCoreIrcListener().connecting();
}
public void disconnected() throws IOException
{
  getCoreIrcListener().disconnected();
}
protected IRCListener getCoreIrcListener()
{
  return coreIrcListener;
}
public IgnoreList getIgnoreList()
{
  return ignoreList;
}
public IRCClient getProtocolHandler()
{
  return getCoreIrcListener().getProtocolHandler();
}
/**
 * handleNoSuchNickChannel method comment.
 */
public void handleAwayMessage(java.lang.String nick, java.lang.String comment) throws java.io.IOException
{
  getCoreIrcListener().handleAwayMessage(nick, comment);
}
public void handleBanListEnd(String s) throws IOException
{
  getCoreIrcListener().handleBanListEnd(s);
}
public void handleBanListItem(String s, String s1) throws IOException
{
  getCoreIrcListener().handleBanListItem(s, s1);
}
public void handleException(Throwable throwable)
{
  getCoreIrcListener().handleException(throwable);
}
/**
 * handleIsonReply method comment.
 */
public void handleIsonReply(java.lang.String serverAddr, java.lang.String[] nicksOnline) throws java.io.IOException
{
  getCoreIrcListener().handleIsonReply(serverAddr, nicksOnline);
}
public void handleListEnd()
{
  getCoreIrcListener().handleListEnd();
}
public void handleListItem(String s, int i, String s1)
{
  getCoreIrcListener().handleListItem(s, i, s1);
}
public void handleListStart()
{
  getCoreIrcListener().handleListStart();
}
public void handleModeChangeRaw(String senderSpecification, String s, String s1, Vector vector)
{
  getCoreIrcListener().handleModeChangeRaw(senderSpecification, s, s1, vector);
}
//public void handleMuteListEnd(String channelName) throws IOException
//{
//  getCoreIrcListener().handleMuteListEnd(channelName);
//}
//public void handleMuteListItem(MuteListItem item) throws IOException
//{
//  getCoreIrcListener().handleMuteListItem(item);
//}
/**
 * handleNoSuchNickChannel method comment.
 */
public void handleNoSuchNickChannel(java.lang.String nickOrChannel, java.lang.String comment) throws java.io.IOException
{
  getCoreIrcListener().handleNoSuchNickChannel(nickOrChannel, comment);
}
/**
 * handleNotice method comment.
 */
public void handleNotice(java.lang.String sender, User senderUser, java.lang.String text) throws java.io.IOException
{
  getCoreIrcListener().handleNotice(sender, senderUser, text);
}
public void handlePart(String s, User c, String s2)
{
  getCoreIrcListener().handlePart(s, c, s2);
}
public void handleTopic(String s, String s1)
{
  getCoreIrcListener().handleTopic(s, s1);
}
public void handleWhoisChannelsOn(String s, String s1)
{
  getCoreIrcListener().handleWhoisChannelsOn(s, s1);
}
public void handleWhoisEnd(String s, String s1)
{
  getCoreIrcListener().handleWhoisEnd(s, s1);
}
public void handleWhoisIdleTime(String s, int i, String s1)
{
  getCoreIrcListener().handleWhoisIdleTime(s, i, s1);
}
public void handleWhoisOperator(String s, String s1)
{
  getCoreIrcListener().handleWhoisOperator(s, s1);
}
public void handleWhoisServer(String s, String s1, String s2)
{
  getCoreIrcListener().handleWhoisServer(s, s1, s2);
}
public void handleWhoisUser(String s, String s1, String s2, String s3)
{
  getCoreIrcListener().handleWhoisUser(s, s1, s2, s3);
}
public void invalidNickName(String s, String s1) throws IOException
{
  getCoreIrcListener().invalidNickName(s, s1);
}
public boolean isIgnored(User c)
{
  return getIgnoreList().isIgnored(c);
}
public boolean isIrczMuteSupported()
{
  return getCoreIrcListener().isIrczMuteSupported();
}
public void join(String s) throws IOException
{
  getCoreIrcListener().join(s);
}
public void join(String s, User c) throws IOException
{
  getCoreIrcListener().join(s, c);
}
public void kick(String s, String s1, String s2, String s3) throws IOException
{
  getCoreIrcListener().kick(s, s1, s2, s3);
}
public void namReplyAddNick(String s, char c) throws IOException
{
  getCoreIrcListener().namReplyAddNick(s, c);
}
public void namReplyFinish() throws IOException
{
  getCoreIrcListener().namReplyFinish();
}
public void namReplyStart(String s) throws IOException
{
  getCoreIrcListener().namReplyStart(s);
}
public void nickChange(String s, String s1) throws IOException
{
  getCoreIrcListener().nickChange(s, s1);
}
public DccReceiver onCreateDccReceiver(User c, String s1, String s2, long l, InetAddress inetaddress, int i)
{
  if (!isIgnored(c))
	return getCoreIrcListener().onCreateDccReceiver(c, s1, s2, l, inetaddress, i);
  else
	return null;
}
/** onDccResumeAccepted method comment. */
public void onDccResumeAccepted(String nickFrom, int port, long filePos)
{
  this.coreIrcListener.onDccResumeAccepted(nickFrom, port, filePos);
}
/** onDccResumeRequest method comment. */
public void onDccResumeRequest(String nickFrom, int port, long filePos) throws IOException
{
  this.coreIrcListener.onDccResumeRequest(nickFrom, port, filePos);
}
public void onInitialTopic(String s, boolean flag, String s1)
{
  getCoreIrcListener().onInitialTopic(s, flag, s1);
}
public void ping(String s) throws IOException
{
  getCoreIrcListener().ping(s);
}
public void quit(String s, String s1) throws IOException
{
  getCoreIrcListener().quit(s, s1);
}
public void registering() throws IOException
{
  getCoreIrcListener().registering();
}
/**
 * setBanned method comment.
 */
public void setBanned(java.lang.String senderSpec, java.lang.String channelName, java.lang.String banMask, boolean isModeOn)
{
  getCoreIrcListener().setBanned(senderSpec, channelName, banMask, isModeOn);
}
/**
 * setInviteOnly method comment.
 */
public void setInviteOnly(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
  getCoreIrcListener().setInviteOnly(senderSpec, channelName, isModeOn);
}
//public void setIrczMuteSupported()
//{
//  getCoreIrcListener().setIrczMuteSupported();
//}
/**
 * setKeyOff method comment.
 */
public void setKeyOff(java.lang.String senderSpec, java.lang.String channelName)
{
  getCoreIrcListener().setKeyOff(senderSpec, channelName);
}
/**
 * setKeyOn method comment.
 */
public void setKeyOn(java.lang.String senderSpec, java.lang.String channelName, java.lang.String key)
{
  getCoreIrcListener().setKeyOn(senderSpec, channelName, key);
}
/**
 * setLimitOff method comment.
 */
public void setLimitOff(java.lang.String senderSpec, java.lang.String channelName)
{
  getCoreIrcListener().setLimitOff(senderSpec, channelName);
}
/**
 * setLimitOn method comment.
 */
public void setLimitOn(java.lang.String senderSpec, java.lang.String channelName, int limit)
{
  getCoreIrcListener().setLimitOn(senderSpec, channelName, limit);
}
/**
 * setModerated method comment.
 */
public void setModerated(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
  getCoreIrcListener().setModerated(senderSpec, channelName, isModeOn);
}
///**
// * setMuted method comment.
// */
//public void setMuted(java.lang.String senderSpec, java.lang.String channelName, java.lang.String muteMask, boolean isModeOn)
//{
//  getCoreIrcListener().setMuted(senderSpec, channelName, muteMask, isModeOn);
//}
/**
 * setNoExternalMessages method comment.
 */
public void setNoExternalMessages(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
  getCoreIrcListener().setNoExternalMessages(senderSpec, channelName, isModeOn);
}
/**
 * setOnlyOpsChangeTopic method comment.
 */
public void setOnlyOpsChangeTopic(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
  getCoreIrcListener().setOnlyOpsChangeTopic(senderSpec, channelName, isModeOn);
}
/**
 * setOperator method comment.
 */
public void setOperator(java.lang.String senderSpec, java.lang.String channelName, java.lang.String nickNameAffected, boolean isModeOn)
{
  getCoreIrcListener().setOperator(senderSpec, channelName, nickNameAffected, isModeOn);
}
/**
 * setPrivate method comment.
 */
public void setPrivate(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
  getCoreIrcListener().setPrivate(senderSpec, channelName, isModeOn);
}
public void setProtocolHandler(IRCClient ircprotocol)
{
  getCoreIrcListener().setProtocolHandler(ircprotocol);
}
/**
 * setSecret method comment.
 */
public void setSecret(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn)
{
  getCoreIrcListener().setSecret(senderSpec, channelName, isModeOn);
}
/**
 * setVoice method comment.
 */
public void setVoice(java.lang.String senderSpec, java.lang.String channelName, java.lang.String nickNameAffected, boolean isModeOn)
{
  getCoreIrcListener().setVoice(senderSpec, channelName, nickNameAffected, isModeOn);
}
/**
 * shouldCreateQuery method comment.
 * @return boolean
 * @param userFrom org.openmim.irc.mvc_impl.model.User_
 * @param textMessage java.lang.String
 */
public boolean shouldCreateQuery(User userFrom, java.lang.String textMessage)
{
  return getCoreIrcListener().shouldCreateQuery(userFrom, textMessage);
}
public void textMessageReceived(User c, String s1, String s2) throws IOException
{
  if (!isIgnored(c))
	getCoreIrcListener().textMessageReceived(c, s1, s2);
}
public void unhandledCommand(IRCMessage ircmessage) throws IOException
{
  getCoreIrcListener().unhandledCommand(ircmessage);
}
public void welcome(String s0, String s) throws IOException
{
  getCoreIrcListener().welcome(s0, s);
}
}
