package org.openmim.test;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import org.openmim.icq.util.joe.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkAdapter;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.infrastructure.statistics.Statistics;

/**
  The GUI AWT application that can be used to test
  functionality of the ICQ2KMessagingNetwork and
  ICQMessagingNetwork plugins.
  <p>
  @see org.openmim.icq2k.ICQ2KMessagingNetwork
*/

public class TestApplet extends Panel implements Runnable
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TestApplet.class.getName());
  private final static String STAT_CAT = "openmim.testapplet";
  private final static Statistics.SimpleCounter STAT_ONLINE_USERS = Statistics.getSimpleCounterInstance(STAT_CAT, "online users");
  private final static Statistics.SimpleCounter STAT_ERRORS = Statistics.getSimpleCounterInstance(STAT_CAT, "errors");
  
  private Thread thread;
  private MessagingNetwork plugin;

  static Properties prop;
  static String configId;

  static
  {
    try
    {
      String res = PropertyUtil.getResourceFilePathName(TestApplet.class, "TestApplet.properties");
      prop = PropertyUtil.loadResourceProperties(TestApplet.class, res);
      configId = prop.getProperty("config.id");
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Using config.id=="+StringUtil.quote(configId));
      if (StringUtil.isNullOrTrimmedEmpty(configId))  throw new AssertException("config.id property is not specified");
      configId = configId.trim();
    }
    catch (RuntimeException ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error loading properties", ex);
      throw ex;
    }
  }

  int getStatus() throws MessagingNetworkException
  {
    return plugin.getClientStatus(getMyLoginId());
  }

  static int getInt(String key)
  {
    String s = getString(key);
    try
    {
      return Integer.parseInt(s);
    }
    catch (NumberFormatException ex)
    {
      throw new AssertException(key+" property must be int, but it is "+StringUtil.quote(s));
    }
  }

  static String getString(String key)
  {
    key = configId + '.' + key;
    String s = prop.getProperty(key);
    if (s == null)  throw new AssertException(key+" property is not specified");
    s = s.trim();
    return s;
  }

  Button userDetailsButton;
  Checkbox msgCheckbox;
  Checkbox authreqCheckbox;
  Checkbox denyCheckbox;
  Checkbox grantCheckbox;

  private Button getUserDetailsButton()
  {
    if (userDetailsButton == null)
    {
      userDetailsButton = new Button("info");
      userDetailsButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          try
          {
            fetchUserDetails();
          }
          catch (Throwable tr)
          {
            printException(tr);
          }
        }
      });
    }
    return userDetailsButton;
  }

  private Button sendContactsButton;

  private Button getSendContactsButton()
  {
    if (sendContactsButton == null)
    {
      sendContactsButton = new Button("send random contacts");
      sendContactsButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          sendContacts();
        }
      });
    }
    return sendContactsButton;
  }

  Properties config;
  TextArea eventLog = new TextArea("[incoming events]\n");
  {
    eventLog.setEditable(false);
  }
  Object eventLogLock = new Object();
  Button closeBtn = new Button("close");
  {
    closeBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        try
        {
          quit();
        }
        catch (Throwable tr)
        {
          printException(tr);
        }
      }
    });
  }
  Choice clientStatus = new Choice();
  {
    clientStatus.add("Online");
    clientStatus.add("Offline");
    clientStatus.add("Busy");
    clientStatus.select("Offline");
    clientStatus.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
        new Thread("icqtest/chooser control handler")
        {
          public void run()
          {
            try
            {
              if (clientStatus.getSelectedItem().equals("Online"))
              {
                if (plugin.getClientStatus(getMyLoginId()) == MessagingNetwork.STATUS_OFFLINE)
                  login();
                plugin.setClientStatus(getMyLoginId(), MessagingNetwork.STATUS_ONLINE, MessagingNetworkException.ENDUSER_NO_ERROR);
              }
              else
                if (clientStatus.getSelectedItem().equals("Busy"))
                {
                  if (plugin.getClientStatus(getMyLoginId()) == MessagingNetwork.STATUS_OFFLINE)
                    login();
                  plugin.setClientStatus(getMyLoginId(), MessagingNetwork.STATUS_BUSY, MessagingNetworkException.ENDUSER_NO_ERROR);
                }
                else
                  if (clientStatus.getSelectedItem().equals("Offline"))
                  {
                    if (plugin.getClientStatus(getMyLoginId()) != MessagingNetwork.STATUS_OFFLINE)
                      plugin.setClientStatus(getMyLoginId(), MessagingNetwork.STATUS_OFFLINE, MessagingNetworkException.ENDUSER_NO_ERROR);
                  }
                  else
                  {
                    org.openmim.icq.util.joe.Lang.ASSERT_FALSE("invalid clientStatus.getSelectedItem()");
                  }
            }
            catch (Throwable tr)
            {
              printException(tr);
            }
          }
        }
        .start();
      }
    });
  }
  TextField loginId = new TextField();
  TextField contactListEntry = new TextField(getString("dst-login-id-2"));
  Button addToContactList = new Button("add");
  {
    addToContactList.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        try
        {
          String cl = contactList.getText();
          for (;;)
          {
            int pos = cl.indexOf(contactListEntry.getText());
            if (pos == -1)
              break;
            cl = cl.substring(0, pos) + cl.substring(pos + contactListEntry.getText().length());
          }
          cl += "\n" + contactListEntry.getText();
          for (;;)
          {
            int pos = cl.indexOf("\n\n");
            if (pos == -1)
              break;
            cl = cl.substring(0, pos) + cl.substring(pos + "\n".length());
          }
          if (cl.startsWith("\n"))
            cl = cl.substring("\n".length());
          if (cl.endsWith("\n"))
            cl = cl.substring(0, cl.length() - "\n".length());
          contactList.setText(cl);

          //
          plugin.addToContactList(getMyLoginId(), contactListEntry.getText());
        }
        catch (Throwable tr)
        {
          printException(tr);
        }
      }
    });
  }
  void fetchUserDetails()
  {
    try
    {
      UserDetails d = plugin.getUserDetails(getMyLoginId(), contactListEntry.getText());
      printUserInfo(d);
    }
    catch (Exception ex)
    {
      printException(ex);
    }
  }

  void printUserInfo(UserDetails d)
  {
    String ar = "exception: property not supported";
    try { ar = ""+d.isAuthorizationRequired(); } catch (Exception ex) {}

    String smsen = "exception retrieving property";
    try { smsen = ""+d.isCellPhoneSMSEnabled(); } catch (Exception ex) {}

    String s = getMyLoginId()+" reports: user details for "+contactListEntry.getText()+
      " are:\r\n  nick="+StringUtil.toPrintableString(d.getNick())+
      ",\r\n  real name="+StringUtil.toPrintableString(d.getRealName())+
      ",\r\n  email="+StringUtil.toPrintableString(d.getEmail())+
      ",\r\n  cell phone="+StringUtil.toPrintableString(d.getCellPhone())+
      ",\r\n  cell phone SMS enabled="+(d.getCellPhone() == null ? "N/A" : smsen)+
      ",\r\n  home city="+StringUtil.toPrintableString(d.getHomeCity())+
      ",\r\n  home state="+StringUtil.toPrintableString(d.getHomeState())+
      ",\r\n  home phone="+StringUtil.toPrintableString(d.getHomePhone())+
      ",\r\n  home fax="+StringUtil.toPrintableString(d.getHomeFax())+
      ",\r\n  home street="+StringUtil.toPrintableString(d.getHomeStreet())+
      ",\r\n  home zipcode="+StringUtil.toPrintableString(d.getHomeZipcode())+
      ",\r\n  authorization required="+ar+
      ".";
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
    log(s);
  }

  void sendContacts()
  {
    try
    {
      int n = (int) (7 * Math.random());
      if (n < 1) n = 2;

      String[] nicks = new String[n];
      String[] loginIds = new String[n];

      int i = 0;
      while (i < n)
      {
        nicks[i] = "random uin #" + i;
        loginIds[i]  = "" + (22222+(int) (10000000 * Math.random()));
        i++;
      }
      plugin.sendContacts(getMyLoginId(), contactListEntry.getText(), nicks, loginIds);
    }
    catch (Exception ex)
    {
      printException(ex);
    }
  }


  Button removeFromContactList = new Button("remove");
  {
    removeFromContactList.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        try
        {
          String cl = contactList.getText();
          for (;;)
          {
            int pos = cl.indexOf(contactListEntry.getText());
            if (pos == -1)
              break;
            cl = cl.substring(0, pos) + cl.substring(pos + contactListEntry.getText().length());
          }
          for (;;)
          {
            int pos = cl.indexOf("\n\n");
            if (pos == -1)
              break;
            cl = cl.substring(0, pos) + cl.substring(pos + "\n\n".length());
          }
          if (cl.startsWith("\n"))
            cl = cl.substring("\n".length());
          if (cl.endsWith("\n"))
            cl = cl.substring(0, cl.length() - "\n".length());
          contactList.setText(cl);

          plugin.removeFromContactList(getMyLoginId(), contactListEntry.getText());
        }
        catch (Throwable tr)
        {
          printException(tr);
        }
      }
    });
  }
  TextField password = new TextField();
  {
    password.setEchoChar('*');
  }
  TextArea contactList = new TextArea();
  {
    String s = getString("contact-list");
    StringTokenizer st = new StringTokenizer(s, ",\t ");
    StringBuffer sb = new StringBuffer(s.length());
    while (st.hasMoreTokens())
    {
      sb.append(st.nextToken()).append("\n");
    }
    contactList.setText(sb.toString());
  }
  TextField dstLoginId = new TextField(getString("dst-login-id-1"));
  TextField sendMsg = new TextField("type msg and press enter");

  {
    ActionListener al = new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        try
        {
          if (denyCheckbox.getState())
            plugin.authorizationResponse(getMyLoginId(), dstLoginId.getText(), false);
          else
          {
            if (grantCheckbox.getState())
              plugin.authorizationResponse(getMyLoginId(), dstLoginId.getText(), true);
            else
            {
              if (authreqCheckbox.getState())
                plugin.authorizationRequest(getMyLoginId(), dstLoginId.getText(), sendMsg.getText());
              else
              {
                if (msgCheckbox.getState())
                {
                  if (StringUtil.isNullOrTrimmedEmpty(dstLoginId.getText()))
                  {
                    return;
                  }
                  else
                  {
                    final String text111 = sendMsg.getText();
                    final String dst111 = dstLoginId.getText();
                    sendMsg.setText(RandomStringUtil.randomString());
                    (new Thread("sendm "+text111)
                    {
                      public void run() {
                        try
                        {
                          plugin.startSendMessage(getMyLoginId(), dst111, text111);
                        }
                        catch (Exception exs)
                        {
                          printException(exs);
                        }
                      }
                    }).start();
                  }
                }
              }
            }
          }
        }
        catch (Throwable tr)
        {
          printException(tr);
        }
      }
    };
    sendMsg.selectAll();
    sendMsg.addActionListener(al);
    dstLoginId.addActionListener(al);
  }
  Button clearEventLogBtn = new Button("clear");
  {
    clearEventLogBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        try
        {
          clearEventLog();
        }
        catch (Throwable tr)
        {
          printException(tr);
        }
      }
    });
  }
  Button loginBtn = new Button("login");
  {
    loginBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        new Thread("ta/login button handler")
        {
          public void run()
          {
            try
            {
              login();
            }
            catch (Throwable tr)
            {
              printException(tr);
            }
          }
        }
        .start();
      }
    });
  }
  Button logoutBtn = new Button("logout");

    {
    logoutBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        try
        {
          logout();
        }
        catch (Throwable tr)
        {
          printException(tr);
        }
      }
    });
  }
  public void clearEventLog()
  {
    synchronized (eventLogLock)
    {
      eventLog.setText("");
    }
  }
  void enableLoginUI()
  {
    try
    {
      logoutBtn.setEnabled(false);
      contactList.setEditable(true);
      loginBtn.setEnabled(true);
      loginId.setEnabled(true);
      password.setEnabled(true);
    }
    catch (Throwable tr)
    {
      printException(tr);
    }
  }
  String[] getContactList()
  {
    java.util.List cl = new java.util.LinkedList();
    StringTokenizer st = new StringTokenizer(contactList.getText());
    StringBuffer sb = new StringBuffer();
    StringBuffer dbg = new StringBuffer("test applet contactlist: ");
    while (st.hasMoreTokens())
    {
      String loginId = st.nextToken().trim();
      if (loginId.length() == 0)
        continue;
      dbg.append("'" + loginId + "' ");
      cl.add(loginId);
      sb.append(loginId).append('\n');
    }
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(dbg.toString());
    contactList.setText(sb.toString());
    return (String[]) cl.toArray(new String[cl.size()]);
  }
  String getMyLoginId()
  {
    return loginId.getText();
  }

  TextField whitepagesNick;
  TextField whitepagesFname;
  TextField whitepagesLname;
  TextField whitepagesEmail;
  
  Checkbox asyncLogin = new Checkbox("async", false);
  
  Button whitepagesButton;

  public void init()
  {
    try
    {
      //data init
      loginId.setText(getString("login-id"));
      password.setText(getString("password"));
      
      

      //ui init
      setLayout(new GridLayout(2, 1));
      Panel inputArea = new Panel(new BorderLayout(2, 2));
      inputArea.add(contactList, "Center");
      Panel bottomR = new Panel(new FlowLayout(FlowLayout.RIGHT));
      bottomR.add(asyncLogin);
      bottomR.add(loginBtn);
      bottomR.add(logoutBtn);
      bottomR.add(closeBtn);
      
      

      Panel bottomL2 = new Panel(new FlowLayout(FlowLayout.LEFT));
      bottomL2.add(new Label("nick:"));
      bottomL2.add(whitepagesNick=new TextField(6));
      bottomL2.add(new Label("fname:"));
      bottomL2.add(whitepagesFname=new TextField(8));
      bottomL2.add(new Label("lname:"));
      bottomL2.add(whitepagesLname=new TextField(8));
      bottomL2.add(new Label("email:"));
      bottomL2.add(whitepagesEmail=new TextField(8));
      bottomL2.add(whitepagesButton=new Button("search"));

      Button authorizationRequiredB;

      bottomL2.add(authorizationRequiredB=new Button("authreq?"));

      authorizationRequiredB.addActionListener(
      new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          String dst = dstLoginId.getText();
          try
          {
            String s = dst+"'s authorizationRequired flag is "+
              plugin.isAuthorizationRequired(
                loginId.getText(),
                dstLoginId.getText()
            );
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
          }
          catch (Throwable tr)
          {
            String s = dst+"'s authorizationRequired flag retrieval failed:";
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
            printException(tr);
          }
        }
      });

      ActionListener al = new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          try
          {
            UserSearchResults results = plugin.searchUsers(
              loginId.getText(),
              whitepagesEmail.getText(),
              whitepagesNick.getText(),
              whitepagesFname.getText(),
              whitepagesLname.getText());
            StringBuffer sb = new StringBuffer(getMyLoginId()).append(" reports: ");
            if (results == null) sb.append("no users found.");
            else
            {
              sb.append(results.getSearchResults().size()+" users found:\r\n");
              java.util.Iterator it = results.getSearchResults().iterator();
              while(it.hasNext())
              {
                UserSearchResults.SearchResult sr = (UserSearchResults.SearchResult) it.next();

                sb.append("uin="+StringUtil.toPrintableString(sr.getLoginId())+
                          " nick="+StringUtil.toPrintableString(sr.getNick())+
                          " real name="+StringUtil.toPrintableString(sr.getRealName())+
                          " email="+StringUtil.toPrintableString(sr.getEmail())+";\r\n");
              }
              if (results.areTruncated())
                sb.append("some search results were truncated.");
              else
                sb.append("no search results were truncated.");
            }
            String s = sb.toString();
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
          }
          catch (Throwable tr)
          {
            printException(tr);
          }
        }
      };

      whitepagesNick.addActionListener(al);
      whitepagesFname.addActionListener(al);
      whitepagesLname.addActionListener(al);
      whitepagesEmail.addActionListener(al);
      whitepagesButton.addActionListener(al);

      Panel bottomL = new Panel(new FlowLayout());
      bottomL.add(new Label("status:"));
      bottomL.add(clientStatus);
      bottomL.add(new Label("contact:"));
      bottomL.add(contactListEntry);
      bottomL.add(addToContactList);
      bottomL.add(removeFromContactList);
      bottomL.add(getUserDetailsButton());
      bottomL.add(getSendContactsButton());

      Panel bottom = new Panel(new BorderLayout());
      bottom.add("Center", bottomR);
      bottom.add("West", bottomL);

      /*
      Panel middle = new Panel(new BorderLayout());
      middle.add("North", bottom);
      middle.add("South", bottomL2);
      */

      inputArea.add(bottom, "South");

      Panel sp = new Panel(new FlowLayout(FlowLayout.LEFT));
      sp.add(new Label("send"));
      CheckboxGroup cbg = new CheckboxGroup();
      sp.add(msgCheckbox = new Checkbox("msg", cbg, true));
      sp.add(authreqCheckbox = new Checkbox("authreq", cbg, false));
      sp.add(denyCheckbox = new Checkbox("deny", cbg, false));
      sp.add(grantCheckbox = new Checkbox("grant", cbg, false));

      Panel leftTop = new Panel(new GridLayout(8, 1));
      leftTop.add(new Label("login id:")); //1
      leftTop.add(loginId); //2
      leftTop.add(new Label("password:"));
      leftTop.add(password); //4

      leftTop.add(sp);
      leftTop.add(sendMsg); //8
      leftTop.add(new Label("to"));
      leftTop.add(dstLoginId); //10

      Panel left = new Panel(new FlowLayout());
      left.add(leftTop);
      inputArea.add(left, "West");
      Panel eventLogPanel = new Panel(new BorderLayout());
      eventLogPanel.add("Center", eventLog);
      Panel eventLogPanelButtons = new Panel(new FlowLayout(FlowLayout.RIGHT));
      eventLogPanelButtons.add(clearEventLogBtn);
      eventLogPanel.add("South", eventLogPanelButtons);
      eventLogPanel.add("North", bottomL2);
      add(inputArea);
      add(eventLogPanel);
      setBackground(SystemColor.control);
      doLayout();
      sendMsg.requestFocus();
    }
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
      System.exit(1);
    }
  }
  public void log(String s)
  {
    synchronized (eventLogLock)
    {
      eventLog.append("[" + org.openmim.icq.util.joe.Logger.formatCurrentDate() + "]\t" + s);
      eventLog.append("\n");
    }
  }
  void login()
  {
    try
    {
      logoutBtn.setEnabled(true);
      contactList.setEditable(false);
      loginBtn.setEnabled(false);
      loginId.setEnabled(false);
      password.setEnabled(false);

      logoutBtn.setEnabled(true);
      
      if (asyncLogin.getState() == true) 
        plugin.startLogin(getMyLoginId(), password.getText(), getContactList(), MessagingNetwork.STATUS_ONLINE);
      else
        plugin.login(getMyLoginId(), password.getText(), getContactList(), MessagingNetwork.STATUS_ONLINE);
    }
    catch (Throwable tr)
    {
      printException(tr);
      boolean loggedIn = false;
      try
      {
        loggedIn = plugin.getClientStatus(getMyLoginId()) != MessagingNetwork.STATUS_OFFLINE;
      }
      catch (Throwable tr2)
      {
        printException(tr2);
      }
      if (!loggedIn)
      {
        enableLoginUI();
      }
    }
  }
  void logout()
  {
    try
    {
      plugin.logout(getMyLoginId(), MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_END_USER);
    }
    catch (Throwable tr)
    {
      printException(tr);
    }
    enableLoginUI();
  }
public static void main(java.lang.String[] args)
{
  String className = null;
  try
  {
    System.err.println("logging is done using log4j.");
    final TestApplet applet = new TestApplet();
    className = getString("plugin-class-name");
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("Instantiating class \"" + className + "\"...");
    try
    {
      applet.plugin = (MessagingNetwork) Class.forName(className).newInstance();
      applet.plugin.init();
    }
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ex in hl", tr);
      System.exit(1);
    }
    java.awt.Frame frame = new java.awt.Frame("TestApplet - " + configId);
    frame.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        applet.quit();
      }
    });
    frame.add("Center", applet);
    frame.setSize(getInt("window.width"), getInt("window.height"));
    frame.setLocation(getInt("screen.x"), getInt("screen.y"));
    applet.init();
    frame.setVisible(true);
    frame.invalidate();
    frame.validate();
    applet.start();
  }
  catch (Throwable tr)
  {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
    System.exit(1);
  }
}
  void printException(Throwable tr)
  {
    STAT_ERRORS.increase();
    
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
    //StringWriter sw = new StringWriter();
    //PrintWriter pw = new PrintWriter(sw);
    //tr.p rintStackTrace(pw);
    //pw.flush();
    //sw.flush();
    if (tr instanceof MessagingNetworkException)
    {
      MessagingNetworkException mex = (MessagingNetworkException) tr;
      log(getMyLoginId() + " reports exception:\r\n  " + tr.getClass().getName() + "\r\n  end user message: [" + mex.getEndUserReasonMessage()+"]\r\n  message: "+tr.getMessage()); //sw.toString());
    }
    else
      log(getMyLoginId() + " reports an unknown exception\r\n(BUG! should be a MessagingNetworkException):\r\n  " + tr.getClass().getName() + "\r\n  message: "+tr.getMessage()); //sw.toString());
  }
  
  public void quit()
  {
    System.exit(0);
  }
  
  public void run()
  {
    try
    {
      /*
      try
      {
        the following does not work
        ((org.openmim.icq2k.ICQ2KMessagingNetwork)plugin).registerLoginId("p");
      }
      catch (Throwable tr)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error registering new uin", tr);
      }
      System.exit(1);
      */


      //start logging in
      plugin.addMessagingNetworkListener(new MessagingNetworkAdapter()
      {
        /** @see MessagingNetwork#startSendMessage(String, String, String) */
        public void sendMessageFailed(byte networkId, long operationId,
          String originalMessageSrcLoginId, String originalMessageDstLoginId, String originalMessageText,
          MessagingNetworkException ex)
          {
            StringBuffer sb = new StringBuffer(
              "sendmsg from " + originalMessageSrcLoginId +
              " to " + originalMessageDstLoginId +
              " failed, text: "+StringUtil.toPrintableString(originalMessageText));
            String s = sb.toString();
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
            printException(ex);
          }

        /** @see MessagingNetwork#startSendMessage(String, String, String) */
        public void sendMessageSuccess(byte networkId, long operationId,
          String originalMessageSrcLoginId, String originalMessageDstLoginId, String originalMessageText)
        {
            StringBuffer sb = new StringBuffer(
              "sendmsg from " + originalMessageSrcLoginId +
              " to " + originalMessageDstLoginId +
              " success, text: "+StringUtil.toPrintableString(originalMessageText));
            String s = sb.toString();
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
        }

        public void getUserDetailsFailed(byte networkId, long operationId,
          String originalSrcLoginId, String originalDstLoginId,
          MessagingNetworkException ex)
        {
            StringBuffer sb = new StringBuffer(
              "getuserinfo src " + originalSrcLoginId +
              " dst " + originalDstLoginId +
              " failed");
            String s = sb.toString();
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
            printException(ex);
        }

        public void getUserDetailsSuccess(byte networkId, long operationId,
          String originalSrcLoginId, String originalDstLoginId,
          UserDetails userDetails)
        {
          printUserInfo(userDetails);
        }

        /** @see org.openmim.mn.MessagingNetwork#startLogin(String, String, String[], int) */
        public void setStatusFailed(byte networkId, long operationId, String originalSrcLoginId,
          MessagingNetworkException ex)
          {
            StringBuffer sb = new StringBuffer(
              "setstatus src " + originalSrcLoginId +
              " failed");
            String s = sb.toString();
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
            log(s);
            printException(ex);
            try
            {
              if (getStatus() == MessagingNetwork.STATUS_OFFLINE)
                enableLoginUI();
            }
            catch (Exception exx)
            {
              printException(exx);
            }
          }

        public void messageReceived(byte networkId, String from, String to, String text)
        {
          if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("incoming message from " + from + " to " + to + " (len: " + text.length() + "):\r\n\"" + text + "\"");
          log("incoming message from " + from + " to " + to + (text == null ? ": null (BUGGG!!)" : " (" + text.length() + " chars):\r\n\"" + text + "\""));
        }

        public void authorizationResponse(byte networkId, String srcLoginId, String dstLoginId, boolean grant)
        {
          StringBuffer sb = new StringBuffer(
            "incoming authorization response from " + srcLoginId +
            " to " + dstLoginId + ", granted: "+grant);
          String s = sb.toString();
          if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
          log(s);
        }

        public void authorizationRequest(byte networkId, String srcLoginId, String dstLoginId, String reason)
        {
          StringBuffer sb = new StringBuffer(
            "incoming authorization request from " + srcLoginId +
            " to " + dstLoginId + ", reason"+
            (reason == null ? ": null" : " (" + reason.length() + " chars):\r\n\"" + reason + "\""));
          String s = sb.toString();
          if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
          log(s);
        }

        public void contactsReceived(byte networkId, String from, String to, String[] contactsUins, String[] contactsNicks)
        {
          StringBuffer sb = new StringBuffer("incoming contacts from " + from + " to " + to + ", number of contacts="+contactsNicks.length+":\r\n");
          int i = 0;
          while (i < contactsNicks.length)
          {
            sb.append(
              "  nick="+StringUtil.toPrintableString(contactsNicks[i])+"\r\n"+
              "  uin ="+StringUtil.toPrintableString(contactsUins[i])+"\r\n");
            i++;
          }
          String s = sb.toString();
          if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
          log(s);
        }

        public void statusChanged(byte networkId, String srcLoginId, String dstLoginId, int status, int reasonLogger, String reasonMessage, int endUserReasonCode)
        {
          String status_s = "invalid: " + status + " (BUGGG!)";
          switch (status)
          {
            case MessagingNetwork.STATUS_OFFLINE :
              status_s = "offline";
              break;
            case MessagingNetwork.STATUS_ONLINE :
              status_s = "online";
              break;
            case MessagingNetwork.STATUS_BUSY :
              status_s = "busy";
              break;
          }
          if (srcLoginId.equals(dstLoginId))
            log(srcLoginId + " changed its client status to " + status_s + (status != MessagingNetwork.STATUS_OFFLINE ? "" : " [" + MessagingNetworkException.getEndUserReasonMessage(endUserReasonCode) + "]"));
          else
            log(srcLoginId + " reports: " + dstLoginId + " changed status to " + status_s);
          //java.awt.Toolkit.getDefaultToolkit().beep();
          if (getMyLoginId().equals(srcLoginId) && srcLoginId.equals(dstLoginId))
          {
            switch (status)
            {
              case MessagingNetwork.STATUS_OFFLINE :
                STAT_ONLINE_USERS.decrease();
                clientStatus.select("Offline");
                enableLoginUI();
                break;
              case MessagingNetwork.STATUS_ONLINE :
                STAT_ONLINE_USERS.increase();
                clientStatus.select("Online");
                break;
              case MessagingNetwork.STATUS_BUSY :
                STAT_ONLINE_USERS.increase();
                clientStatus.select("Busy");
                break;
            }
          }
        }
      });
      login();
      thread = null;
    }
    catch (Throwable tr)
    {
      printException(tr);
    }
  }
  public void start()
  {
    if (thread == null)
    {
      thread = new Thread(this);
      thread.start();
    }
  }
  public void stop()
  {
    try
    {
      logout();
    }
    catch (Throwable tr)
    {
      printException(tr);
    }
    thread = null;
  }
}
