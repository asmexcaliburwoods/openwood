package org.openmim.test;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import org.apache.log4j.Logger;
import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkListener;
import org.openmim.mn.MessagingNetworkException;

public class TestMessagingNetwork implements MessagingNetwork {
  java.util.List networkListeners = new ArrayList();
  UsersEntries entries =  new UsersEntries();
  ArrayList cmdList = new ArrayList();
  ArrayList lstatusList = new ArrayList();
  ArrayList lmessageList = new ArrayList();
  StreamTokenizer st;
  JFrame frame;
  private static final Logger CAT = Logger.getLogger(TestMessagingNetwork.class.getName());

  public void logout(String srcLoginId, int endUserReason) throws MessagingNetworkException {
    logout(srcLoginId);
  }

  public void setClientStatus(String srcLoginId, int status, int endUserReason) throws MessagingNetworkException
  {
    setClientStatus(srcLoginId, status);
  }

  public TestMessagingNetwork()
  {
    frame = new JFrame();
    frame.setTitle("IM network script runner");
    frame.setBackground(Color.lightGray);
    frame.getContentPane().setLayout(new BorderLayout());
    JButton jb = null;
    JPanel jp;
    frame.getContentPane().add("Center", jp = new JPanel());
    jp.setLayout(new FlowLayout());
    jp.add(jb = new JButton("Run"));
    jb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        readFile();
        runScript();
      }
    });

    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        e.getWindow().dispose();
      }
    });
    frame.pack();
    frame.setVisible(true);
  }

  void readFile()
  {
    try
    {
      File testFile = new File("src/ru/openmim/mim/server/messaging/TestMessagingNetwork.scr");
      FileReader fileReader = new FileReader(testFile);
      StreamTokenizer strt = new StreamTokenizer(fileReader);
      st = strt;
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
    }
  }

  void runScript()
  {
    try
    {
      boolean lstatusFlag = false;
      boolean lmessageFlag = false;
      for(;;)
      {
        String tmp_cmd = getCMD();
        if(!tmp_cmd.equals(""))
        {
          if(tmp_cmd.equals("lstatus"))
            lstatusFlag = true;
          else
            if(tmp_cmd.equals("lmessage"))
              lmessageFlag = true;
            else
              if(tmp_cmd.equals("changeStatus"))
              {
                String srcLoginId = "";
                String dstLoginId = "";
                int status = 0;
                for(int i=0; i<3; i++)
                {
                  String tmp_param = getParamName();
                  if(tmp_param.equals("srcLoginId"))
                    srcLoginId = getStringValue();
                  else
                    if(tmp_param.equals("dstLoginId"))
                      dstLoginId = getStringValue();
                    else
                      if(tmp_param.equals("status"))
                        status = getIntValue();
                      else
                        System.err.println("Parameters didn't correct " + tmp_param);
                }
                if(!srcLoginId.equals("") && !dstLoginId.equals("") && status!=0)
                {
                  if(lstatusFlag)
                  {
                    LogicCommand ls = new LogicCommand("changeStatus", srcLoginId, dstLoginId, "", status);
                    lstatusList.add(ls);
                  }
                  else
                    if(lmessageFlag)
                    {
                      LogicCommand ls = new LogicCommand("changeStatus", srcLoginId, dstLoginId, "", status);
                      lmessageList.add(ls);
                    }
                    else
                      chStatus(getNetworkId(), srcLoginId, dstLoginId, status);
                }
              }
              else
                if(tmp_cmd.equals("outMsg"))
                {
                  String srcLoginId = "";
                  String dstLoginId = "";
                  String text = "";
                  for(int i=0; i<3; i++)
                  {
                    String tmp_param = getParamName();
                    if(tmp_param.equals("srcLoginId"))
                      srcLoginId = new Integer(getIntValue()).toString();
                    else
                      if(tmp_param.equals("dstLoginId"))
                        dstLoginId = new Integer(getIntValue()).toString();
                      else
                        if(tmp_param.equals("text"))
                          text = getStringValue();
                        else
                          System.err.println("Parameter didn't correct " + tmp_param);
                  }
                  if(!srcLoginId.equals("") && !dstLoginId.equals("") && !text.equals(""))
                  {
                    if(lstatusFlag)
                    {
                      LogicCommand ls = new LogicCommand("outMsg", srcLoginId, dstLoginId, text, 0);
                      lstatusList.add(ls);
                    }
                    else
                      if(lmessageFlag)
                      {
                        LogicCommand ls = new LogicCommand("outMsg", srcLoginId, dstLoginId, text, 0);
                        lmessageList.add(ls);
                      }
                      else
                        sendMsg(srcLoginId, dstLoginId, text)  ;
                  }
                }
                else
                  if(tmp_cmd.equals("endlogic"))
                  {
                    lstatusFlag = false;
                    lmessageFlag = false;
                  }
                  else
                    System.err.println("Command didn't correct " + tmp_cmd);
        }
        else
        {
          System.out.println("File is end");
          break;
        }
      }
    }
    catch(Exception ex)
    {
      System.out.println("Exception "+ex);
      ex.printStackTrace();
    }
  }

  public int getIntValue() throws java.io.IOException
  {
    int tmp_value = -1;
    if(st.nextToken()!=st.TT_EOF)
      tmp_value = (int)st.nval;
    System.out.println("getIntValue " + tmp_value);
    return tmp_value;
  }

  public String getStringValue() throws java.io.IOException
  {
    String tmp_value = "";
    if(st.nextToken()!=st.TT_EOF)
      tmp_value = st.sval;
    System.out.println("getStringValue " + tmp_value);
    return tmp_value;
  }

  public String getParamName() throws java.io.IOException
  {
    String tmp_param = "";
    if(st.nextToken()!=st.TT_EOF)
      tmp_param = st.sval;
    System.out.println("getParamName " + tmp_param);
    return tmp_param;
  }

  public String getCMD() throws java.io.IOException
  {
    String tmp_cmd = "";
    while(st.nextToken()!=st.TT_EOF)
    {
      if(st.sval.equals("cmd"))
        if(st.nextToken()!=st.TT_EOF)
        {
          tmp_cmd = st.sval;
          break;
        }
    }
    System.out.println("getCMD " + tmp_cmd);
    return tmp_cmd;
  }

  void chStatus(byte nid, String srcLoginId, String dstLoginId, int status)
  {
    for(Iterator nli = networkListeners.iterator(); nli.hasNext(); )
    {
      ((MessagingNetworkListener)nli.next()).statusChanged(nid, srcLoginId, dstLoginId, status, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
       System.out.println("statusChanged(getNetworkId() = "+nid+", srcLoginId() = "+srcLoginId+", dstLoginId() = "+dstLoginId+", status = "+status+");");
    }
  }

  public void addToContactList(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {
    System.out.println(srcLoginId + " addToContactList " + dstLoginId);
    entries.get(srcLoginId).getContacts().add(entries.get(dstLoginId));
  }

  public void login(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException
  {
    NetworkEntry ne = entries.get(srcLoginId);
    ne.setStatus((byte)status);
    ne.setLoggedIn(true);
    ne.getContacts().clear();
    System.out.println("--------------------");
    System.out.println("login :");
    System.out.println("id "+srcLoginId);
    System.out.println("status "+status);
    System.out.println("contacts :");
    for(int i = 0; i < contactList.length; i++)
      ne.getContacts().add(entries.get(contactList[i]));
    System.out.println("--------------------");
  }

  public void logout(String srcLoginId) throws MessagingNetworkException
  {
    NetworkEntry ne = entries.get(srcLoginId);
    ne.setLoggedIn(false);
    System.out.println(srcLoginId + "is logout");
  }

  public void removeFromContactList(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {
    System.out.println(srcLoginId + " removeFromContactList " + dstLoginId);
    entries.get(srcLoginId).getContacts().remove(entries.get(dstLoginId));
  }

  public void sendMsg(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {
    System.out.println("NetworkId " + getNetworkId() + " srcLoginId " + srcLoginId + " dstloginId " + dstLoginId + " text " + text);
    for(Iterator nli = networkListeners.iterator(); nli.hasNext(); )
      ((MessagingNetworkListener)nli.next()).messageReceived(getNetworkId(), srcLoginId, dstLoginId, text);
  }

  public void sendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {
    System.out.println(srcLoginId + " send message " + text + " to " + dstLoginId);

    for(Iterator msg_iter = lmessageList.iterator(); msg_iter.hasNext(); )
    {
      LogicCommand lc = (LogicCommand)msg_iter.next();
      if(lc.dstLoginId.equals(srcLoginId))
      {
        if(lc.cmd.equals("changeStatus"))
        {
            chStatus(getNetworkId(), lc.dstLoginId, lc.srcLoginId, lc.status);
        }
        else
        {
          sendMsg(lc.srcLoginId, lc.dstLoginId, lc.text);
        }
      }
    }
    //    messageSend(srcLoginId, dstLoginId, text);
  }


  void messageSend(String from, String to, String text)
  {

    if(from == null || to == null || from.length() == 0 || to.length() == 0)
      return;
    NetworkEntry nef = entries.get(from);
    NetworkEntry net = entries.get(to);

    if(net.getLoggedIn())
      for(Iterator nli = networkListeners.iterator(); nli.hasNext(); )
        ((MessagingNetworkListener)nli.next()).messageReceived(getNetworkId(), from, to, text);
  }

  public void setClientStatus(String srcLoginId, int status) throws MessagingNetworkException
  {
    System.out.println("setClientStatus " + srcLoginId + " in " + status);
    entries.get(srcLoginId).setStatus((byte)status);
    if(status==1 || status==2)
      for(Iterator status_iter = lstatusList.iterator(); status_iter.hasNext(); )
      {
        LogicCommand lc = (LogicCommand)status_iter.next();
        if(lc.dstLoginId.equals(srcLoginId))
        {
          if(lc.cmd.equals("changeStatus"))
          {
            chStatus(getNetworkId(), lc.dstLoginId, lc.srcLoginId, lc.status);
          }
          else
          {
            sendMsg(lc.srcLoginId, lc.dstLoginId, lc.text);
          }
        }
      }
  }

  public int getClientStatus(String srcLoginId) throws MessagingNetworkException
  {
    System.out.println("getClientStatus " + srcLoginId + " is " + entries.get(srcLoginId).getStatus());
    return entries.get(srcLoginId).getStatus();
  }

  public int getStatus(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {
    System.out.println("getStatus " + dstLoginId + " is " + entries.get(dstLoginId).getStatus());
    return entries.get(dstLoginId).getStatus();
  }

  public byte getNetworkId()
  {
    System.out.println("getNetworkId() = 1");
    return (byte)1;
  }

  public String getName()
  {
    System.out.println("getName() = ICQ");
    return "ICQ";
  }

  public String getComment()
  {
    return "";
  }

  public void removeMessagingNetworkListener(MessagingNetworkListener l) throws MessagingNetworkException
  {
    System.out.println("removeMessagingNetworkListener");
    networkListeners.remove(l);
  }

  public void addMessagingNetworkListener(MessagingNetworkListener l)
  {
    System.out.println("addMessagingNetworkListener");
    networkListeners.add(l);
  }

  class UsersEntries
  {
    Hashtable entries = new Hashtable();
    java.util.List entriesList = new ArrayList();

    public Collection values()
    {
      return entries.values();
    }

    public NetworkEntry get(String id)
    {
      NetworkEntry ne = (NetworkEntry)entries.get(id);
      if(ne == null)
        return put(id, (byte)3, false);
      else
        return ne;
    }

    public NetworkEntry get(int index)
    {
      return (NetworkEntry)entriesList.get(index);
    }

    public NetworkEntry put(String id, byte st, boolean loggedIn)
    {
      NetworkEntry ne = new NetworkEntry(id, st, loggedIn);
      Object o = entries.get(id);

      if(o != null) {
        int index = entriesList.indexOf(o);
        entriesList.set(index, ne);
        entries.put(id, ne);
      } else {
        entriesList.add(ne);
        entries.put(id, ne);
      }
      return ne;
    }
  }

  class NetworkEntry
  {
    private String loginId;
    private byte status;
    private boolean loggedIn;
    private java.util.List contacts = new ArrayList();

    NetworkEntry(String id, byte st, boolean loggedIn)
    {
      super();
      setLoginId(id);
      setStatus(st);
      setLoggedIn(loggedIn);
    }

    String getLoginId()
    {
      return loginId;
    }

    private void setLoginId(final String id)
    {
      loginId = id;
      System.out.println("setLoginId " + id);
    }

    java.util.List getContacts()
    {
      return contacts;
    }

    void receivedMessage(String srcLoginId, String dstLoginId, String text)
    {
      for(Iterator nli = networkListeners.iterator(); nli.hasNext(); )
      {
        ((MessagingNetworkListener)nli.next()).messageReceived(getNetworkId(), srcLoginId, dstLoginId, text);
        System.out.println("messageRecieved(getNetworkId() = "+getNetworkId()+", getLoginId() = "+srcLoginId+", dstLoginId = "+dstLoginId+", text = "+text+");");
      }
    }

    boolean getLoggedIn()
    {
      return loggedIn;
    }

    void setLoggedIn(boolean li)
    {
      loggedIn = li;
    }

    byte getStatus()
    {
      return status;
    }

    void setStatus(byte st)
    {
      status = st;

      for(Iterator i = entries.values().iterator(); i.hasNext(); )
      {
        NetworkEntry ne = (NetworkEntry)i.next();
          for(Iterator nli = networkListeners.iterator(); nli.hasNext(); ) {
            ((MessagingNetworkListener)nli.next()).statusChanged(getNetworkId(), ne.getLoginId(), getLoginId(), status, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
            System.out.println("statusChanged(getNetworkId() = "+getNetworkId()+", ne.getLoginId() = "+ne.getLoginId()+", getLoginId() = "+getLoginId()+", status = "+status+");");
          }
      }
    }
  }

  class LogicCommand
  {
    public String cmd;
    public String srcLoginId;
    public String dstLoginId;
    public String text;
    public int status;

    public LogicCommand(String cmd, String srcLoginId, String dstLoginId, String text, int status)
    {
      this.cmd = cmd;
      this.srcLoginId = srcLoginId;
      this.dstLoginId = dstLoginId;
      this.text = text;
      this.status = status;
    }
  }

  public UserDetails getUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {TODO(); return null;}
  /**
    If null is returned, then no users found.
    @see org.openmim.UserSearchResults
  */
  public UserSearchResults searchUsers(
    String srcLoginId,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern) throws MessagingNetworkException
  {TODO(); return null;}

  public void sendContacts(String srcLoginId, String dstLoginId, String[] nicks, String[] loginIds) throws MessagingNetworkException
  {TODO();}
  public boolean isAuthorizationRequired(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {TODO();return false;}
  public void authorizationRequest(String srcLoginId, String dstLoginId, String reason)
      throws MessagingNetworkException
  {TODO();}
  public void authorizationResponse(String srcLogin, String dstLogin, boolean grant)
      throws MessagingNetworkException
  {TODO();}

  public void init()
  {if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("TODO: empty init();");}
  public void deinit()
  {if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("TODO: empty deinit();");}

  private void TODO() { throw new RuntimeException("TODO: not implemented."); }
  public long startLogin(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startSendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startGetUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
}
