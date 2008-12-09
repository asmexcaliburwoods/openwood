package org.openmim.irc.mn;

import org.openmim.infrastructure.Context;
import org.openmim.mn2.controller.IRCIMNetwork;
import org.openmim.mn2.controller.IRCServerImpl;
import org.openmim.mn2.controller.IMNetwork;
import org.openmim.mn2.controller.MN2Factory;
import org.openmim.mn2.controller.NameConvertor;
import org.openmim.mn2.controller.NameConvertorImpl;
import org.openmim.mn2.model.*;

import java.util.ArrayList;
import java.util.List;

public class IRCMN2FactoryImpl implements MN2Factory {
    private NameConvertor nc=new NameConvertorImpl();

    public static MN2Factory createFactory() {
        return new IRCMN2FactoryImpl();
    }

    private IRCMN2FactoryImpl(){
    }

    public NameConvertor getNameConvertor() {
        return nc;
    }

    public IMNetwork createIMNetwork(IMNetwork.Type type, String key, IMListener imListener,
                                     StatusRoomListenerInternal statusRoomListenerInternal,
                                     List<Server> listOfIRCServersToKeepConnectionWith, Context ctx) {
        switch (type){
            case irc:
                return new IRCIMNetwork(key, ctx, imListener, statusRoomListenerInternal,
                        listOfIRCServersToKeepConnectionWith);
            default:throw new AssertionError();
        }
    }

    public Server createServer(String hostNameOfRealServer, String redirdHostName, int redirdPort, String realName,
                                  List<String> nickNames, String password, String identdUserName) {
        return new IRCServerImpl(
                hostNameOfRealServer, redirdHostName, redirdPort, realName, nickNames, 
                password, identdUserName);
    }

    public List<Server> createListOfServersToKeepConnectionWith(
            List<ServerBean> listFromStorage, ConfigurationBean config) {
        List<Server> list2=new ArrayList<Server>(listFromStorage==null?1:listFromStorage.size());
        if(listFromStorage!=null)
            for (ServerBean bean : listFromStorage) {
                final GlobalParameters gp = config.getGlobalParameters();
                IRCServerBean irc= (IRCServerBean) bean;
                String realName = gp.isRealNameForIRCUsed() ? gp.getRealNameForIRC() : gp.getFirstName() + " " + gp.getLastName();
                if(realName==null)realName="";
                realName=realName.trim();
                if(realName.equals(""))realName="...";
                list2.add(createServer(irc.getHostName(),irc.getHostName(),irc.getPort(),
                        realName,gp.getNickNameList(), irc.getPassword(), gp.getIdentdUserName()));
            }
        return list2;
    }
}
