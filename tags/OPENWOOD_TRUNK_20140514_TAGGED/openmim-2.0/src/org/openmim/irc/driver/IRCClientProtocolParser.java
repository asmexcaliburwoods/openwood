package org.openmim.irc.driver;

import org.openmim.irc.driver.dcc.DCCResumeRegistry;
import org.openmim.irc.driver.dcc.DccReceiver;
import org.openmim.mn2.controller.IRCController;
import org.openmim.mn2.model.*;
import squirrel_util.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

//
public class IRCClientProtocolParser extends IRCConstant {

    public IRCClientProtocolParser() {
    }

    /**
     * Should return null if dcc resume not supported.
     */
    public static DCCResumeRegistry getDccResumeRegistry(IRCListener irclistener) {
        Lang.ASSERT_NOT_NULL(irclistener, "irclistener");
        IRCProtocol prot = irclistener.getProtocolHandler();
        Lang.ASSERT_NOT_NULL(prot, "irclistener.getProtocolHandler()");
        if (!(prot instanceof IRCClient)) {
            IRCProtocol.dbg("!(prot instanceof IRCClient): it is of class " + prot.getClass().getName());
            return null;
        }
        IRCClient client = (IRCClient) prot;
        return client.getDccResumeRegistry();
    }

    protected static String getFileNameWithSpaces(ReversedStringTokenizer str, String lastWordToAssert) throws ExpectException {
        //
        //handles filenames with spaces....
        //via creating a reversed string tokenizer
        StringBuffer fn = new StringBuffer();
        Lang.EXPECT(str.hasMoreTokens(), "str should have at least one token");
        String lastTok = str.nextToken();
        while (str.hasMoreTokens()) {
            if (fn.length() > 0)
                fn.insert(0, " ");
            fn.insert(0, lastTok);
            lastTok = str.nextToken();
        }
        //the very last token is "send"/"accept"/"resume"
        Lang.ASSERT(lastWordToAssert.equalsIgnoreCase(lastTok), "should be true: (" + StringUtil.toPrintableString(lastWordToAssert) + ".equalsIgnoreCase(lastTok)), lastTok=" + StringUtil.toPrintableString(lastTok));
        //
        String fileName = fn.toString();
        Lang.EXPECT_NOT_NULL_NOR_EMPTY(fileName, "fileName");
        //strip possible surrounding mIRC's quotas
        int spos = fileName.startsWith("\"") ? 1 : 0;
        int epos = fileName.length() + (fileName.endsWith("\"") ? -1 : 0);
        if (epos <= spos)
            return "file.ext";
        else
            return fileName.substring(spos, epos);
    }

    protected static void handleCommand(int i, IRCMessage msg, IRCListener il) throws Exception {
        Lang.ASSERT_NOT_NULL(il, "il");
        Lang.ASSERT_NOT_NULL(msg, "msg");
        IRCController queryClient = il.getProtocolHandler().getLocalClient();
        Lang.ASSERT_NOT_NULL(queryClient, "queryClient");
        //org.openmim.irc.mvc_impl.model.User_ clientFrom = msg.getUser();
        //Lang.ASSERT_NOT_NULL(clientFrom, "clientFrom");
        int middlePartsCount = msg.getMiddlePartsCount();
        //
        switch (i) {
            default:
                break;
                //:goo!webchat@127.0.0.*** NOTICE goo :pidarrr
            case RPL_EXT_ISON:
                //":127.0.0.1 303 jj_mirc_foo :"
                //":127.0.0.1 303 jj_mirc_foo :jj_mirc_foo "
                //":127.0.0.1 303 jj_mirc_foo :jjj_mirc_foo jj_mirc_foo "
                String[] nicksOnline = Acme.Utils.splitStr(msg.getTrailing().trim(), ' ');
                Lang.ASSERT_NOT_NULL(nicksOnline, "nicksOnline");
                il.handleIsonReply(/*server*/ msg.getPrefix(), nicksOnline);
                return;
            case RPL_EXT_NOTICE:
                try {
                    il.handleNotice(msg.getPrefix(), msg.getSender(), msg.getTrailing());
                }
                catch (Exception ex) {
                    il.handleNotice(msg.getPrefix(), null, msg.getTrailing());
                }
                return;
            case 002:
                String s002 = msg.getTrailing();
//	  if (s002 != null && s002.toLowerCase().lastIndexOf(".ircz.") != -1)
//		il.setIrczMuteSupported();
                break;
//	case 387 :
//	  :Beauce-R.Qc.UnderZ.Org 387 goo #goo :End of IRCChannel Mute List
//	  if (il.isIrczMuteSupported())
//	  {
//		Lang.EXPECT(middlePartsCount >= 1, "middlePartsCount is expected to be >= 1, but it is " + middlePartsCount);
//		String channelName = msg.getMiddlePart(1);
//		Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(channelName, "channelName");
//		il.handleMuteListEnd(channelName);
//		return;
//	  }
//	  break;
//	case 386 :
//	  //:Beauce-R.Qc.UnderZ.Org 386 goo #z jp!*@* sKaTeR 971112601
//	  //                                1  2      3      4
//	  if (il.isIrczMuteSupported())
//	  {
//		Lang.EXPECT(middlePartsCount >= 4, "middlePartsCount is expected to be >= 4, but it is " + middlePartsCount);
//		String channelName = msg.getMiddlePart(1);
//		String ircMaskMuted = msg.getMiddlePart(2);
//		String muterNickName = msg.getMiddlePart(3);
//		String timeAsString = msg.getMiddlePart(4);
//		Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(channelName, "channelName");
//		Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(ircMaskMuted, "ircMaskMuted");
//		il.handleMuteListItem(new org.openmim.irc.driver.ircz.MuteListItem(channelName, ircMaskMuted, muterNickName, timeAsString));
//		return;
//	  }
//	  break;
                //:127.0.0.1 401 goo goojhg :No such nick/channel
            case 401:
                il.handleNoSuchNickChannel(msg.getMiddlePart(1), msg.getTrailing());
                return;
                //:127.0.0.1 301 goo goo :away message
            case 301:
                il.handleAwayMessage(msg.getMiddlePart(1), msg.getTrailing());
                return;
            case 405:
            case 471:
            case 473:
            case 474:
            case 475:
                il.cannotJoinChannel(msg.getMiddlePart(1), msg.getTrailing());
                return;
            case 432:
            case 433:
            case 436:
                il.invalidNickName(msg.getMiddlePart(1), msg.getTrailing());
                return;
            case 311:
                il.handleWhoisUser(msg.getMiddlePart(1), msg.getMiddlePart(2), msg.getMiddlePart(3), msg.getTrailing());
                return;
            case 312:
                il.handleWhoisServer(msg.getMiddlePart(1), msg.getMiddlePart(2), msg.getTrailing());
                return;
            case 313:
                il.handleWhoisOperator(msg.getMiddlePart(1), msg.getTrailing());
                return;
            case 317:
                il.handleWhoisIdleTime(msg.getMiddlePart(1), Integer.parseInt(msg.getMiddlePart(2)), msg.getTrailing());
                return;
            case 319:
                il.handleWhoisChannelsOn(msg.getMiddlePart(1), msg.getTrailing());
                return;
            case 318:
                il.handleWhoisEnd(msg.getMiddlePart(1), msg.getTrailing());
                return;
            case 331:
                il.onInitialTopic(msg.getMiddlePart(1), false, msg.getTrailing());
                return;
            case 332:
                il.onInitialTopic(msg.getMiddlePart(1), true, msg.getTrailing());
                return;
            case 367:
                il.handleBanListItem(msg.getMiddlePart(1), msg.getMiddlePart(2));
                return;
            case 368:
                il.handleBanListEnd(msg.getMiddlePart(1));
                return;
            case -101:

            {
                String to = msg.getMiddlePart(0);
                Lang.ASSERT_NOT_NULL_NOR_EMPTY(to, "privmsg.to");
                //char c = to.charAt(0);
                //boolean isToChannel = (c == '#' || c == '&');
                ////
                User userFrom = msg.getSender();
                Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
                //if (!isToChannel)
                //{
                ////new Query should not be created at all for ignored users.
                //if (il.shouldCreateQuery(userFrom, msg.getTrailing()))
                //queryClient.onJoinedQuery(userFrom);
                //}
                if (msg.getTrailing() != null) {
                    String s = msg.getTrailing();
                    if (s.startsWith("\001")) {
                        boolean flag1 = s.endsWith("\001") && s.length() > 1;
                        int j = flag1 ? s.length() - 1 : s.length();
                        handleCtcp(msg, s.substring(1, j), il);
                        return;
                    }
                }
                il.textMessageReceived(//
                        userFrom, //
                        to, //
                        msg.getTrailing());
                return;
            }
            case -100:
                il.ping(msg.getTrailing());
                return;
            case -103:
                handleJoin(il, msg);
                return;
            case 353:
                parseNamReply(il, msg);
                return;
            case RPL_EXT_PART:

            {
                String channelName = msg.getMiddlePart(0);
                IRCUser userFrom = msg.getSender();
                Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
                IRCChannelParticipant IRCChannelRole = queryClient.getChannelRoleByChannelName(userFrom, channelName);
                Lang.EXPECT_NOT_NULL(IRCChannelRole, "IRCChannelRole");
                IRCChannel channel = (IRCChannel) IRCChannelRole.getRoom();
                channel.parts(IRCChannelRole, msg.getTrailing());
                il.handlePart(channelName, userFrom, msg.getTrailing() != null ? msg.getTrailing() : userFrom.getActiveNick());
                return;
            }
            case -106:
                il.handleTopic(msg.getMiddlePart(0), msg.getTrailing());
                return;
            case -105:

            {
                IRCUser userFrom = msg.getSender();
                Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
                queryClient.onQuit(userFrom);
                il.quit(userFrom.getActiveNick(), msg.getTrailing());
                return;
            }
            case -104:

            {
                IRCUser userFrom = msg.getSender();
                Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
                String oldNickName = userFrom.getActiveNick();
                String newNickName = msg.getTrailing();
                if (newNickName == null) newNickName = msg.getMiddlePart(0);
                il.nickChange(oldNickName, newNickName);
                userFrom.setActiveNick(newNickName);
                return;
            }
            case -108:

            {
                IRCUser userFrom = msg.getSender();
                Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
                String channelName = msg.getMiddlePart(0);
                String kickedNick = msg.getMiddlePart(1);
                IRCChannel IRCChannel = queryClient.getChannelJoinedByChannelName(channelName);
                Lang.EXPECT_NOT_NULL(IRCChannel, "IRCChannel named " + StringUtil.toPrintableString(channelName));
                IRCChannelParticipant IRCChannelRole = queryClient.getChannelRoleByNickName(IRCChannel,kickedNick);
                Lang.EXPECT_NOT_NULL(IRCChannelRole, "IRCChannelRole for " + StringUtil.toPrintableString(kickedNick) + " on " + StringUtil.toPrintableString(channelName));
                IRCChannel.kicked(IRCChannelRole,userFrom,msg.getTrailing());
                il.kick(userFrom.getActiveNick(), channelName, kickedNick, msg.getTrailing());
                return;
            }
            case 1:
                /* '\001' */
                il.welcome(msg.getMiddlePart(0), msg.getTrailing());
                queryClient.welcome_setNickName(msg.getMiddlePart(0));
                return;
            case RPL_EXT_MODE:
            case 324:
                //:127.0.0.1 324 goo #goo +
                final boolean is324 = i == 324;
                Vector vector = msg.getMiddleParts();
                if (vector.size() >= 1) {
                    if (is324) {
                        vector.removeElementAt(0);
                    }
                    String channelName = null;
                    String modeChars = null;
                    if (vector.size() >= 1) {
                        channelName = msg.getMiddlePart(is324 ? 1 : 0);
                        vector.removeElementAt(0);
                        if (vector.size() >= 1) {
                            modeChars = msg.getMiddlePart(is324 ? 2 : 1);
                            vector.removeElementAt(0);
                        }
                    }
                    il.handleModeChangeRaw(msg.getPrefix(), channelName != null ? channelName : msg.getTrailing(), modeChars != null ? modeChars : msg.getTrailing(), vector);
                }
                if (!is324)
                    parseModeChange(msg, il);
                return;
            case 321:
                il.handleListStart();
                return;
            case 322:
                il.handleListItem(msg.getMiddlePart(1), Integer.parseInt(msg.getMiddlePart(2)), msg.getTrailing());
                return;
            case 323:
                il.handleListEnd();
                return;
        }
        il.unhandledCommand(msg);
    }

    protected static void handleCtcp(IRCMessage ircmessage, String s, IRCListener irclistener) throws Exception {
        StringTokenizer stringtokenizer = new StringTokenizer(s, " ");
        String s1 = stringtokenizer.nextToken().toLowerCase();
        String s2 = "";
        if (stringtokenizer.hasMoreTokens())
            s2 = stringtokenizer.nextToken("");
        if (s1.equals("action")) {
            irclistener.actionMessageReceived(ircmessage.getSender(), ircmessage.getMiddlePart(0), s2);
            return;
        }
        if (s1.equals("dcc")) {
            handleCtcpDcc(ircmessage, s2, irclistener);
            return;
        } else {
            //irclistener.textMessageReceived(ircmessage.getSender(), ircmessage.getMiddlePart(0), ircmessage.getTrailing());
            return;
        }
    }

    protected static void handleCtcpDcc(IRCMessage ircmessage, String line, IRCListener irclistener) throws UnknownHostException, ExpectException, IOException {
        StringTokenizer st = new StringTokenizer(line, " ");
        String dccCommand = st.nextToken().toLowerCase();
        if (dccCommand.equals("send"))
            handleCtcpDccSend(irclistener, ircmessage, line);
        else if (dccCommand.equals("resume"))
            handleCtcpDccResumeRequest(irclistener, ircmessage, line);
        else if (dccCommand.equals("accept"))
            handleCtcpDccResumeAccept(irclistener, ircmessage, line);
        else {
            irclistener.textMessageReceived(ircmessage.getSender(), ircmessage.getMiddlePart(0), ircmessage.getTrailing());
            return;
        }
    }

    protected static void handleCtcpDccResumeAccept(IRCListener irclistener, IRCMessage ircmessage, String line) throws ExpectException, UnknownHostException {
        ReversedStringTokenizer str = new ReversedStringTokenizer(line, " ");
        Lang.EXPECT(str.hasMoreTokens(), "filePos is unknown");
        String filePos_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "port is unknown");
        String senderPort_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "file name is unknown");
        String suggestedFileName = getFileNameWithSpaces(str, "accept");
        //incoming suggestedFileName should be ignored here;
        //if incoming dccaccept is sent by mIRC of latest versions,
        //we always have suggestedFileName=="file.ext" here
        //
        int senderPort = (int) Util.parseLong(senderPort_s, 0);
        Lang.EXPECT(senderPort != 0, "Incoming DCC RESUME: Bad port specified: " + StringUtil.toPrintableString(senderPort_s));
        long filePos = Util.parseLong(filePos_s, -1);
        Lang.EXPECT(filePos > 0, "Incoming DCC RESUME: Bad file pos: " + StringUtil.toPrintableString(filePos_s));
        String senderNick = ircmessage.getSender().getActiveNick();
        //
        irclistener.onDccResumeAccepted(senderNick, senderPort, filePos);
    }

    protected static void handleCtcpDccResumeRequest(IRCListener irclistener, IRCMessage ircmessage, String line) throws ExpectException, IOException {
        ReversedStringTokenizer str = new ReversedStringTokenizer(line, " ");
        Lang.EXPECT(str.hasMoreTokens(), "filePos is unknown");
        String filePos_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "port is unknown");
        String senderPort_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "file name is unknown");
        String suggestedFileName = getFileNameWithSpaces(str, "resume");
        //incoming suggestedFileName should be ignored here;
        //if incoming dccresume is sent by mIRC of latest versions,
        //we always have suggestedFileName=="file.ext" here
        //
        int senderPort = (int) Util.parseLong(senderPort_s, 0);
        Lang.EXPECT(senderPort != 0, "Incoming DCC ACCEPT: Bad port specified: " + StringUtil.toPrintableString(senderPort_s));
        long filePos = Util.parseLong(filePos_s, -1);
        Lang.EXPECT(filePos > 0, "Incoming DCC ACCEPT: Bad file pos: " + StringUtil.toPrintableString(filePos_s));
        String senderNick = ircmessage.getSender().getActiveNick();
        //
        irclistener.onDccResumeRequest(senderNick, senderPort, filePos);
    }

    protected static void handleCtcpDccSend(IRCListener irclistener, IRCMessage ircmessage, String line) throws ExpectException, UnknownHostException {
        ReversedStringTokenizer str = new ReversedStringTokenizer(line, " ");
        //String dccCommand = st.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "fileSize is unknown");
        String fileSize_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "port is unknown");
        String senderPort_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "ip address is unknown");
        String senderIPAddr_s = str.nextToken();
        Lang.EXPECT(str.hasMoreTokens(), "file name is unknown");
        String suggestedFileName = getFileNameWithSpaces(str, "send");
        //
        long senderIPAddr = Util.parseLong(senderIPAddr_s, -1);
        Lang.EXPECT(senderIPAddr != -1, "Incoming DCC SEND: Bad ip address specified: " + StringUtil.toPrintableString(senderIPAddr_s));
        java.net.InetAddress inetaddress = IRCClientProtocolScanner.convertDccLongIpToInetAddress(senderIPAddr);
        int senderPort = (int) Util.parseLong(senderPort_s, 0);
        Lang.EXPECT(senderPort != 0, "Incoming DCC SEND: Bad port specified: " + StringUtil.toPrintableString(senderPort_s));
        long sentFileSize = Util.parseLong(fileSize_s, -1);
        Lang.EXPECT(sentFileSize >= 0L, "Incoming DCC SEND: Bad file size: " + StringUtil.toPrintableString(fileSize_s));
        IRCUser sender = ircmessage.getSender();
        //
        final DccReceiver dr = irclistener.onCreateDccReceiver(sender, ircmessage.getMiddlePart(0), suggestedFileName, sentFileSize, inetaddress, senderPort);
        if (dr == null) {
            IRCProtocol.dbg("onCreateDccReceiver() returned null, DCC SEND not handled or is ignored: skipped.");
            return;
        } else {
            Thread thread = new Thread("DCCRECV of " + suggestedFileName + " from " + sender.getActiveNick() + "/" + inetaddress) {
                public void run() {
                    try {
                        IRCProtocol.dbg("Thread " + Thread.currentThread().getName() + " spawned.");
                        dr.onFileReceiveStart();
                    }
                    catch (Throwable tr) {
                        dr.unhandledException(tr);
                    }
                }
            };
            thread.start();
            return;
        }
    }

    protected static void handleJoin(IRCListener irclistener, IRCMessage ircmessage) throws IOException, ExpectException {
        IRCUser userJoined = ircmessage.getSender();
        String nickJoined = userJoined.getActiveNick();
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(nickJoined, "nickJoined");
        String channelName = ircmessage.getTrailing();
        if (channelName == null)
            channelName = ircmessage.getMiddlePart(0);
        if (channelName != null)
            channelName = channelName.trim();
        IRCController queryClient = irclistener.getProtocolHandler().getLocalClient();
        if (nickJoined.equalsIgnoreCase(irclistener.getProtocolHandler().getNick())) {
            queryClient.onMeJoinedChannel(channelName, userJoined);
            irclistener.join(channelName);
        } else {
            IRCChannel ch = queryClient.getChannelJoinedByChannelName(channelName);
            Lang.EXPECT_NOT_NULL(ch, "channel named " + StringUtil.toPrintableString(channelName));
            final RoomParticipant rp = queryClient.createDefaultRole(ch, userJoined);
            rp.getRoom().addRoomRole(rp);
            ch.joins((IRCChannelParticipant) rp);
            irclistener.join(channelName, userJoined);
        }
    }

    public static void parseLineFromServer(String s, IRCListener irclistener) throws IOException {
        try {
            IRCMessage ircmessage = IRCClientProtocolScanner.tokenizeLine(irclistener.getProtocolHandler().getLocalClient(), s);
            handleCommand(ircmessage.getCommand(), ircmessage, irclistener);
        }
        catch (IOException ioexception) {
            throw ioexception;
        }
        catch (Exception exception) {
            Logger.printException(exception);
            irclistener.handleException(exception);
            //Lang.ASSERT_FALSE("in parse line from server");
        }
    }

    /**
     * Parses MODE message received from server and calls related handlers.
     * <pre>
     * MODE <channel> {[+|-]|o|p|s|i|t|n|m|l|b|v|k} {<limit>|<nickname>|<banmask>|<key>}
     * o - give/take channel operator privileges;
     * p - private channel;
     * s - secret channel;
     * i - invite-only channel;
     * t - topic settable by channel operator only;
     * n - no messages to channel from clients on the outside;
     * m - moderated channel;
     * l - set the user limit to channel;
     * b - set a ban mask to keep users out;
     * M - ircz: set a mute mask to keep users silent;
     * v - give/take the ability to speak on a moderated channel;
     * k - set a channel key (password).
     * <p/>
     * MODE <nickname> {[+|-]|i|s|w|o}
     * i - marks a users as invisible;
     * s - marks a user for receipt of server notices;
     * w - user receives wallops;
     * o - operator.
     * </pre>
     */
    private static void parseModeChange(IRCMessage msg, IRCListener il) throws ExpectException {
        final String senderSpec = msg.getPrefix();
        Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(senderSpec, "senderSpec");
        final int argCount = msg.getMiddlePartsCount();
        Lang.EXPECT(argCount >= 1, "arg count must be at least 1, but it is " + argCount);
        final String channelOrNickname = msg.getMiddlePart(0);
        Lang.EXPECT_NOT_NULL_NOR_EMPTY(channelOrNickname, "channelOrNickname");
        final String modeChars = argCount == 1 ? msg.getTrailing() : msg.getMiddlePart(1);
        Lang.EXPECT_NOT_NULL_NOR_EMPTY(modeChars, "modeChars");
        final boolean isChannel = "#&".indexOf(channelOrNickname.charAt(0)) > -1;
        int argPos = 2;
        boolean plusDefined = false;
        boolean plus = false;
        for (int modeCharsPos = 0; modeCharsPos < modeChars.length(); modeCharsPos++) {
            final char modeChar = modeChars.charAt(modeCharsPos);
            switch (modeChar) {
                case '+':
                    plusDefined = true;
                    plus = true;
                    continue;
                case '-':
                    plusDefined = true;
                    plus = false;
                    continue;
                default:
                    Lang.EXPECT(plusDefined, "+ or - expected before any mode chars");
            }
            if (isChannel) {
                //channel modes
                switch (modeChar) {
                    case 'o':
                    case 'v':
                    case 'b':
                    case 'M':
                        Lang.EXPECT(argPos < argCount, "MODE: not enough arguments");
                }
                switch (modeChar) {
                    case 'b':
                        il.setBanned(senderSpec, channelOrNickname, msg.getMiddlePart(argPos++), plus);
                        break;
//		case 'M' :
//		  il.setMuted(senderSpec, channelOrNickname, msg.getMiddlePart(argPos++), plus);
//		  break;
                    case 'o':
                        il.setOperator(senderSpec, channelOrNickname, msg.getMiddlePart(argPos++), plus);
                        break;
                    case 'v':
                        il.setVoice(senderSpec, channelOrNickname, msg.getMiddlePart(argPos++), plus);
                        break;
                    case 'l':
                        if (plus) {
                            Lang.EXPECT(argPos < argCount, "MODE: not enough arguments");
                            String limit_s = msg.getMiddlePart(argPos++);
                            int limit = -1;
                            try {
                                limit = Integer.parseInt(limit_s);
                            }
                            catch (NumberFormatException ex) {
                                Lang.EXPECT_FALSE();
                            }
                            Lang.EXPECT_POSITIVE(limit, "IRCChannel user limit must be a positive integer number, but it is " + StringUtil.toPrintableString(limit_s));
                            il.setLimitOn(senderSpec, channelOrNickname, limit);
                        } else {
                            il.setLimitOff(senderSpec, channelOrNickname);
                        }
                        break;
                    case 'k':
                        if (plus) {
                            Lang.EXPECT(argPos < argCount, "MODE: not enough arguments");
                            il.setKeyOn(senderSpec, channelOrNickname, msg.getMiddlePart(argPos++));
                        } else {
                            il.setKeyOff(senderSpec, channelOrNickname);
                        }
                        break;
                    case 'n':
                        il.setNoExternalMessages(senderSpec, channelOrNickname, plus);
                        break;
                    case 't':
                        il.setOnlyOpsChangeTopic(senderSpec, channelOrNickname, plus);
                        break;
                    case 'p':
                        il.setPrivate(senderSpec, channelOrNickname, plus);
                        break;
                    case 's':
                        il.setSecret(senderSpec, channelOrNickname, plus);
                        break;
                    case 'i':
                        il.setInviteOnly(senderSpec, channelOrNickname, plus);
                        break;
                    case 'm':
                        il.setModerated(senderSpec, channelOrNickname, plus);
                        break;
                }
            }
            //else
            //{
            ////user modes
            ////i - marks a users as invisible;
            ////s - marks a user for receipt of server notices;
            ////w - user receives wallops;
            ////o - operator.

            //}
        } //of modeChars loop
    }

    protected static void parseNamReply(IRCListener irclistener, IRCMessage ircmessage) throws IOException, ExpectException {
        String channelName = ircmessage.getMiddlePart(2);
        Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(channelName, "channelName");
        IRCController queryClient = irclistener.getProtocolHandler().getLocalClient();
        IRCChannel IRCChannel = queryClient.getChannelJoinedByChannelName(channelName);
        Lang.EXPECT_NOT_NULL(IRCChannel, "IRCChannel");
        irclistener.namReplyStart(channelName);
        String nickName;
        char modifierChar;
        StringTokenizer st = new StringTokenizer(ircmessage.getTrailing());
        while (st.hasMoreElements()) {
            nickName = st.nextToken();
            Lang.EXPECT_POSITIVE(nickName.length(), "@nick length for " + StringUtil.toPrintableString(nickName));
            modifierChar = nickName.charAt(0);
            if (modifierChar == '@' || modifierChar == '+')
                nickName = nickName.substring(1);
            else
                modifierChar = ' ';
            Lang.EXPECT_POSITIVE(nickName.length(), "nick length for " + StringUtil.toPrintableString(nickName));
            if (!nickName.equalsIgnoreCase(queryClient.getActiveNick())) {
//	  User_ client = queryClient.getCreateUser(nickName);
//	  IRCChannel.createDefaultRole_addToRoom(client);
            }
            irclistener.namReplyAddNick(nickName, modifierChar);
        }
        irclistener.namReplyFinish();
    }
}
