package org.openmim.irc.regexp_util;

import org.openmim.mn2.model.IRCUser;
import squirrel_util.Lang;

public class IRCMaskUtil {

    public static String createDefaultIRCMask(IRCUser user) {
        Lang.ASSERT_NOT_NULL(user, "user");
        String hostName = user.getHostName();
        if (hostName == null)
            return user.getActiveNick() + "!*@*";
        else
            return "*!*@" + hostName;
    }
}
