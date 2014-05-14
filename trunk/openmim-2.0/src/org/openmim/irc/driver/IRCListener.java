package org.openmim.irc.driver;

//
import java.io.*;
import java.net.*;
import java.util.*;

import org.openmim.irc.driver.dcc.DccReceiver;
import org.openmim.messaging_network2.model.User;

//
public interface IRCListener
{

void actionMessageReceived(User c, String s1, String s2);
void cannotJoinChannel(String s, String s1) throws IOException;
void connected() throws IOException;
void connecting() throws IOException;
void disconnected() throws IOException;
IRCClient getProtocolHandler();
void handleAwayMessage(String nick, String comment) throws IOException;
void handleBanListEnd(String s) throws IOException;
void handleBanListItem(String s, String s1) throws IOException;
void handleException(Throwable throwable);
/**
The nicksOnline parameter is never null;
nicksOnline.length can be 0.
*/
void handleIsonReply(String serverAddr, String[] nicksOnline) throws IOException;
void handleListEnd();
void handleListItem(String s, int i, String s1);
void handleListStart();
void handleModeChangeRaw(String senderSpecification, String channelNameOrNickNames, String modeChars, Vector modeStrings);
void handleNoSuchNickChannel(String nickOrChannel, String comment) throws IOException;
void handleNotice(String sender, User senderUser, String text) throws IOException;
void handlePart(String s, User c, String s2);
void handleTopic(String s, String s1);
void handleWhoisChannelsOn(String s, String s1);
void handleWhoisEnd(String s, String s1);
void handleWhoisIdleTime(String s, int i, String s1);
void handleWhoisOperator(String s, String s1);
void handleWhoisServer(String s, String s1, String s2);
void handleWhoisUser(String s, String s1, String s2, String s3);
void invalidNickName(String s, String s1) throws IOException;
boolean isIrczMuteSupported();
void join(String s) throws IOException;
void join(String s, User c) throws IOException;
void kick(String s, String s1, String s2, String s3) throws IOException;
void namReplyAddNick(String nickName, char channelRoleChar) throws IOException;
void namReplyFinish() throws IOException;
void namReplyStart(String channelName) throws IOException;
void nickChange(String s, String s1) throws IOException;
DccReceiver onCreateDccReceiver(User c, String s1, String s2, long l, InetAddress inetaddress, int i);
void onDccResumeAccepted(String nickFrom, int port, long filePos);
void onDccResumeRequest(String nickFrom, int port, long filePos) throws IOException;
void onInitialTopic(String s, boolean flag, String s1);
void ping(String s) throws IOException;
void quit(String s, String s1) throws IOException;
void registering() throws IOException;
void setBanned(String senderSpec, String channelName, String banMask, boolean isModeOn);
void setInviteOnly(String senderSpec, String channelName, boolean isModeOn);
void setKeyOff(String senderSpec, String channelName);
void setKeyOn(String senderSpec, String channelName, String key);
void setLimitOff(String senderSpec, String channelName);
void setLimitOn(String senderSpec, String channelName, int limit);
void setModerated(String senderSpec, String channelName, boolean isModeOn);
void setNoExternalMessages(String senderSpec, String channelName, boolean isModeOn);
void setOnlyOpsChangeTopic(String senderSpec, String channelName, boolean isModeOn);
void setOperator(String senderSpec, String channelName, String nickNameAffected, boolean isModeOn);
void setPrivate(String senderSpec, String channelName, boolean isModeOn);
void setProtocolHandler(IRCClient ircprotocol);
void setSecret(String senderSpec, String channelName, boolean isModeOn);
void setVoice(String senderSpec, String channelName, String nickNameAffected, boolean isModeOn);
/**
If this returns true (this is the default behavior),
new Query room will be automatically created by
the underlying model.  Otherwise, the Query
creation will be skipped.
<p>
This method should be used for implementing
ignore users feature: text messages from ignored users
should not create new Query objects in the underlying
model.
<p>
Note: textMessage can be null here.
*/
boolean shouldCreateQuery(User userFrom, String textMessage);
void textMessageReceived(User c, String s1, String s2) throws IOException;
void unhandledCommand(IRCMessage ircmessage) throws IOException;
void welcome(String newNick, String msg) throws IOException;
}
