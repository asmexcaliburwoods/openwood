package org.openmim.irc.driver;

import org.openmim.mn2.controller.IRCController;
import squirrel_util.ExpectException;
import squirrel_util.Lang;
import squirrel_util.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

public class IRCClientProtocolScanner extends IRCConstant {

    public IRCClientProtocolScanner() {
    }

    public static long convertDccAckToLong(byte[] abyte0) {
        return ((long) abyte0[0] & 255L) << 24 | ((long) abyte0[1] & 255L) << 16 | ((long) abyte0[2] & 255L) << 8 | (long) abyte0[3] & 255L;
    }

    public static InetAddress convertDccLongIpToInetAddress(long l) throws UnknownHostException {
        return InetAddress.getByName((l >> 24 & 255L) + "." + (l >> 16 & 255L) + "." + (l >> 8 & 255L) + "." + (l & 255L));
    }

    public static byte[] convertDccLongToAck(long l) {
        byte abyte0[] = new byte[4];
        abyte0[0] = (byte) (int) (l >> 24 & 255L);
        abyte0[1] = (byte) (int) (l >> 16 & 255L);
        abyte0[2] = (byte) (int) (l >> 8 & 255L);
        abyte0[3] = (byte) (int) (l & 255L);
        return abyte0;
    }

    public static long convertInetAddressToDccLongIp(InetAddress inetaddress) {
        byte abyte0[] = inetaddress.getAddress();
        return ((long) abyte0[0] & 255L) << 24 | ((long) abyte0[1] & 255L) << 16 | ((long) abyte0[2] & 255L) << 8 | (long) abyte0[3] & 255L;
    }

    protected static int parseCommand(String s) throws ExpectException {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException _ex) {
            //eat
        }
        switch (s.charAt(0)) {
            default:
                break;
            case 80:
                /* 'P' */
                if (s.equals("PING"))
                    return -100;
                else if (s.equals("PART"))
                    return -107;
                else if (s.equals("PRIVMSG"))
                    return -101;
            case 69:
                /* 'E' */
                if (s.equals("ERROR"))
                    return -3;
            case 78:
                /* 'N' */
                if (s.equals("NICK"))
                    return -104;
                else if (s.equals("NOTICE"))
                    return RPL_EXT_NOTICE;
            case 84:
                /* 'T' */
                if (s.equals("TOPIC"))
                    return -106;
            case 74:
                /* 'J' */
                if (s.equals("JOIN"))
                    return -103;
            case 81:
                /* 'Q' */
                if (s.equals("QUIT"))
                    return -105;
            case 75:
                /* 'K' */
                if (s.equals("KICK"))
                    return -108;
            case 77:
                /* 'M' */
                if (s.equals("MODE"))
                    return RPL_EXT_MODE;
        }
        Lang.EXPECT(false, "Unknown command: [" + s + "]");
        return -1;
    }

    public static IRCMessage tokenizeLine(IRCController queryClient, String line) {
        Lang.ASSERT_NOT_NULL(queryClient, "queryClient");
        Lang.ASSERT_NOT_NULL(line, "server response line");

        //
        IRCProtocol.dbg(" IN: " + line);

        //
        String prefix = null;
        int command;
        java.util.Vector<String> middleParts = new Vector<String>();
        String trailing = null;

        //
        parsing:
        {
            int i = 0;
            char c = '\u0000';
            try {
                i = 0;
                c = line.charAt(0);
                if (c == ':') {
                    c = line.charAt(++i);
                    StringBuffer sb = new StringBuffer();
                    for (; c != ' '; c = line.charAt(++i))
                        sb.append(c);
                    prefix = sb.toString();
                    Lang.EXPECT(c == ' ', "Malformed server response: must be space after <prefix>");
                    //noinspection StatementWithEmptyBody
                    for (; c == ' '; c = line.charAt(++i)) ;
                }
                StringBuffer sb1 = new StringBuffer();
                for (; c != ' '; c = line.charAt(++i))
                    sb1.append(c);
                command = parseCommand(sb1.toString());
                Lang.EXPECT(c == ' ', "Malformed server response: must be space after <command>");
                //noinspection StatementWithEmptyBody
                for (; c == ' '; c = line.charAt(++i)) ;
                while (c != ':') {
                    StringBuffer sb2 = new StringBuffer();
                    while (c != ' ' && c != 0 && c != '\r' && c != '\n') {
                        sb2.append(c);
                        if (++i < line.length())
                            c = line.charAt(i);
                        else
                            c = '\u0000';
                    }
                    Lang.EXPECT(sb2.length() != 0, "Malformed server response: <middle> can't be empty");
                    middleParts.addElement(sb2.toString());
                    if (i >= line.length())
                        break parsing;
                    Lang.EXPECT(c == ' ', "Malformed server response: must be space after <middle>");
                    while (c == ' ') {
                        if (++i < line.length()) {
                            c = line.charAt(i);
                        } else {
                            c = '\u0000';
                            break parsing;
                        }
                    }
                }
                Lang.EXPECT(c == ':', "Malformed server response: must be ':' before <trailing>");
                trailing = line.substring(i + 1);
            }
            catch (Exception exception) {
                System.err.println("Error parsing message from IRC server, it is probably malformed:");
                if (line == null) {
                    System.err.println("  line: null");
                } else {
                    System.err.println("  line: [" + line + "]");
                    System.err.println("  pos: " + i);
                    System.err.println("  tok: " + c);
                }
                Logger.printException(exception);
                command = -2;
            }
        } //of parsing

        return new IRCMessage(queryClient, command, prefix, middleParts, trailing);
    }
}
