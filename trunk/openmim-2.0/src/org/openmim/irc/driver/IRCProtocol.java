package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   IRCProtocol.java

import org.openmim.irc.driver.dcc.DCCResumeRegistry;
import squirrel_util.Lang;
import squirrel_util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

// Referenced classes of package org.openmim.irc:

//      IRCListener, IRCClientProtocolParser
public abstract class IRCProtocol {
    private DCCResumeRegistry dccResumeRegistry = new org.openmim.irc.driver.dcc.DCCResumeRegistry();
    private BufferedReader reader;
    private PrintWriter writer;
    protected IRCListener listener;
    private boolean interrupted;
    protected String nick;

    public IRCProtocol() {
        interrupted = false;
    }

    public void changeNick(String s) throws IOException {
        sendCommand("NICK", new String[]{s}, null);
    }

    public void close() {
        interrupt();
    }

    public static void dbg(String s) {
        Logger.log(s);
    }

    /**
     * Insert the method's description here. Creation date: (04.10.00 10:27:45)
     *
     * @return <{DCCResumeRegistry}>
     */
    public DCCResumeRegistry getDccResumeRegistry() {
        return dccResumeRegistry;
    }

    public String getNick() {
        return nick;
    }

    public void init(BufferedReader bufferedreader, PrintWriter printwriter, IRCListener irclistener) {
        if (bufferedreader == null || printwriter == null || irclistener == null) {
            throw new NullPointerException();
        } else {
            reader = bufferedreader;
            writer = printwriter;
            listener = irclistener;
            listener.setProtocolHandler((IRCClient) this);
            return;
        }
    }

    public void interrupt() {
        setInterrupted(true);
    }

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    public void join(String s) throws IOException {
        sendCommand("JOIN", new String[]{s}, null);
    }

    public void part(String s, String s1) throws IOException {
        sendCommand("PART", new String[]{s}, s1);
    }

    public void pong(String s) throws IOException {
        sendCommand("PONG", null, s);
    }

    public void process() throws IOException {
        //try
        //{
        do processLine(reader.readLine());
        while (!isInterrupted());
        //}
        //finally
        //{
        //setInterrupted(false);
        //}
    }

    private void processLine(String s) throws IOException {
        if (s == null) {
            listener.disconnected();
            interrupt();
            return;
        }
        if (!isInterrupted())
            IRCClientProtocolParser.parseLineFromServer(s, listener);
    }

    public void quit(String s) throws IOException {
        sendCommand("QUIT", null, s);
    }

    public void register(String loginPassword, String nickName, String userName, String s3, String realIrcServerHostName, String realName) throws IOException {
        nick = nickName;
        sendCommand("PASS", new String[]{loginPassword}, null);
        sendCommand("NICK", new String[]{nickName}, null);
        sendCommand("USER", new String[]{userName, realIrcServerHostName, realIrcServerHostName}, realName);
    }

    public void sendActionMessage(String recipientNick, String message) throws IOException {
        sendCtcpMessage(recipientNick, "ACTION " + message);
    }

    public void sendBanModeChange(boolean isBanned, String channelName, String bannedUserIrcMask) throws IOException {
        sendModeChange(channelName, isBanned ? "+b" : "-b", bannedUserIrcMask, null);
    }

    public void sendChannelBanListRequest(String s) throws IOException {
        sendCommand("MODE", new String[]{s, "+b"}, null);
    }

    public void sendChannelBasicModesRequest(String s) throws IOException {
        sendCommand("MODE", new String[]{s}, null);
    }

    public void sendChannelMuteListRequest(String channelName) throws IOException {
        sendCommand("MODE", new String[]{channelName, "+M"}, null);
    }

    public void sendCommand(String s, String[] as, String s1) throws IOException {
        StringBuffer stringbuffer = new StringBuffer(s);
        if (as != null) {
            for (int i = 0; i < as.length; i++) {
                stringbuffer.append(' ');
                stringbuffer.append(as[i]);
            }
        }
        if (s1 != null)
            stringbuffer.append(" :").append(s1);
        IRCProtocol.dbg("OUT: " + stringbuffer);
        writer.print(stringbuffer.append("\r\n").toString());
        writer.flush();
    }

    public void sendCtcpMessage(String recipientNick, String message) throws IOException {
        sendMessage(recipientNick, "\001" + message + "\001");
    }

    public void sendDccCommand(String recipientNick, String dccCommandArgs) throws IOException {
        sendCtcpMessage(recipientNick, "DCC " + dccCommandArgs);
    }

    public void sendDccResumeAccepted(String dccFileReceiverNick, String proposedFileName, int port, long filePos) throws IOException {
        sendDccCommand(dccFileReceiverNick, "ACCEPT file.ext " + port + " " + filePos);
    }

    public void sendDccResumeRequest(String dccFileSenderNick, String proposedFileName, int port, long filePos) throws IOException {
        sendDccCommand(dccFileSenderNick, "RESUME file.ext " + port + " " + filePos);
    }

    /**
     * <pre>
     * "ISON jj_mirc_foo nick2 tab tabtab zlodey jjj_mirc_foo"
     * </pre>
     */
    public void sendIsonRequest(String[] ircMasks) throws IOException {
        Lang.ASSERT_NOT_NULL(ircMasks, "ircMasks");
        sendCommand("ISON", ircMasks, null);
    }

    public void sendJoin(String s) throws IOException {
        join(s);
    }

    public void sendKick(String s, String s1, String s2) throws IOException {
        sendCommand("KICK", new String[]{s, s1}, s2);
    }

    public void sendListRequest(String s) throws IOException {
        sendCommand("LIST", new String[]{s != null ? s : ""}, null);
    }

    public void sendMessage(String s, String s1) throws IOException {
        sendCommand("PRIVMSG", new String[]{s}, s1);
    }

    public void sendModeChange(String channelName, String modeModifiersString, String s2, String s3) throws IOException {
        sendCommand("MODE", new String[]{//
                channelName, //
                modeModifiersString, //
                s3 != null ? s3 : "", s2 != null ? s2 : ""}, null);
    }

    public void sendMuteModeChange(boolean isMuted, String channelName, String mutedUserIrcMask) throws IOException {
        sendModeChange(channelName, isMuted ? "+M" : "-M", mutedUserIrcMask, null);
    }

    public void sendNickServIdentify(String s, String s1) throws IOException {
        sendCommand("NS", new String[]{"IDENTIFY", "" + s, s1 != null ? s1 : ""}, null);
    }

    protected synchronized void setInterrupted(boolean flag) {
        interrupted = flag;
        if (flag) {
            try {
                if (writer != null)
                    writer.close();
            }
            catch (Exception ex) {
                Logger.printException(ex);
            }
            //try
            //{
            //if (reader != null)
            //reader.close();
            //}
            //catch (Exception ex)
            //{
            //squirrel_util.Logger.printException(ex);
            //}
        }
    }

    public void setNick(String s) {
        nick = s;
    }
}
