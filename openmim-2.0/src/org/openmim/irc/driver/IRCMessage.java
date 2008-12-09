package org.openmim.irc.driver;

import org.openmim.mn2.controller.IRCController;
import org.openmim.mn2.model.IRCUser;
import squirrel_util.ExpectException;
import squirrel_util.Lang;
import squirrel_util.StringUtil;

import java.util.Vector;

//
public class IRCMessage {
    private String trailing;
    private int command;
    private String prefix;
    private IRCController queryClient;
    private Vector<String> middleParts;
    private IRCUser user;
    /**
     * loginName is filled in by extractNickFromPrefix()
     */
    private String loginName;
    /**
     * nickNameOrServerName is filled in by extractNickFromPrefix()
     */
    private String nickNameOrServerName;
    /**
     * hostName is filled in by extractNickFromPrefix()
     */
    private String hostName;

    /**
     * queryClient cannot be null.
     */
    public IRCMessage(//
                      IRCController queryClient, //
                      int command, //
                      String prefix, Vector<String> middleParts, String trailing) {
        this.command = command;
        this.middleParts = (middleParts == null ? new Vector<String>() : middleParts);
        this.prefix = prefix;
        this.trailing = trailing;
        //user attribute is instantiated later,
        //on getUser()
        //user = getCreateClient(queryClient);
        Lang.ASSERT_NOT_NULL(queryClient, "queryClient");
        this.queryClient = queryClient;
    }

    public IRCUser getSender() throws ExpectException {
        synchronized (this) {
            if (user == null)
                user = getModifyCreateClient();
        }
        return user;
    }

    public int getCommand() {
        return command;
    }

    public String getMiddlePart(int i) {
        Lang.ASSERT(i >= 0 && i < middleParts.size(), "requested middle part #" + i + " cannot be retrieved: parts #0...#" + (middleParts.size() - 1) + " are only available.");
        return middleParts.elementAt(i);
    }

    public Vector<String> getMiddleParts() {
        Vector<String> cloned = new Vector<String>(getMiddlePartsCount());
        for (int i = 0; i < middleParts.size(); i++) {
            cloned.addElement(middleParts.elementAt(i));
        }
        return cloned;
    }

    public int getMiddlePartsCount() {
        return middleParts.size();
    }

    /**
     * Gets existing User_ from the queryClient,
     * or, if it does not exist, creates a new RemoteClientImpl,
     * but does not associates a queryClient with this new created instance.
     * This association should be done using queryClient's joinXxx() methods
     * instead, if needed.
     * <p/>
     * If the user exists but does not know its login name/host name,
     * and these are already known, they are assigned here.
     * <p/>
     * The operation getModifyCreateClient() is needed to restore
     * (and update) User_'s association links if they exist.
     */
    private IRCUser getModifyCreateClient() throws ExpectException {
        //squirrel_util.Lang.ASSERT(this.user == null, "this.user must be null here.");
        parsePrefix();
        if (nickNameOrServerName == null)
            return null;
        Lang.EXPECT(nickNameOrServerName.indexOf('.') == -1, "A bug OR malformed server response: nickname expected instead of " + StringUtil.toPrintableString(nickNameOrServerName));
        return queryClient.getModifyCreateUser(nickNameOrServerName, loginName, hostName);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getTrailing() {
        return trailing;
    }

    /**
     * Takes the <code>this.prefix</code> field, and
     * creates the following fields:<br>
     * <br>
     * - <code>this.nickNameOrServerName</code>
     * (created always), <br>
     * - <code>this.loginName</code>
     * (created if found, otherwise null is assigned), and <br>
     * - <code>this.hostName</code>
     * (created if found, otherwise null is assigned).
     * <p/>
     * Examples of possible prefix attribute:<br>
     * <br>
     * - <code>goo!goo@goo.ru</code><br>
     * - <code>goo</code><br>
     * - <code>irc.ru</code><br>
     * - <code>(null)</code><br>
     */
    private void parsePrefix() throws ExpectException {
        //squirrel_util.Lang.ASSERT_NOT_NULL(prefix, "prefix");
        if (prefix == null)
            return;
        int exclamationPos = prefix.indexOf('!');
        Lang.ASSERT(this.nickNameOrServerName == null, "this.nickNameOrServerName must be null here");
        Lang.ASSERT(this.loginName == null, "this.loginName must be null here");
        Lang.ASSERT(this.hostName == null, "this.hostName must be null here");
        if (exclamationPos == -1)
            this.nickNameOrServerName = prefix;
        else {
            this.nickNameOrServerName = prefix.substring(0, exclamationPos);
            int frogPos = prefix.indexOf('@', exclamationPos + 1);
            Lang.EXPECT(frogPos != -1, "Malformed server response: No '@' char found after '!' in the irc user address spec.");
            loginName = prefix.substring(exclamationPos + 1, frogPos);
            Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(this.loginName, "login name in (malformed) server response");
            hostName = prefix.substring(frogPos + 1);
            Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(this.hostName, "hostname in (malformed) server response");
        }
        //squirrel_util.Lang.EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(this.nickNameOrServerName, "nickName/servername in (malformed) server response");
    }
}
