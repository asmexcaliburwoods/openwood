package org.openmim;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import org.apache.log4j.Logger;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkListener;
import org.openmim.mn.MessagingNetworkException;

public class ConsoleMessagingNetwork implements MessagingNetwork
{
  EntriesTableModel entries =  new EntriesTableModel();
  EntriesTableCellRenderer renderer = new EntriesTableCellRenderer();
  private static final Logger CAT = Logger.getLogger(ConsoleMessagingNetwork.class.getName());

  java.util.List networkListeners = new ArrayList();
  JFrame frame;
  JPopupMenu entryPopup;
  JMenu changeStatus;
  JTable jt;
  SendDialog sendDialog;

  public ConsoleMessagingNetwork() {
    entryPopup = new JPopupMenu();

    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        entryPopup.setVisible(false);
        if(jt.getSelectedRow() < 0)
          return;
        NetworkEntry ne = entries.get(jt.getSelectedRow());

        if(e.getActionCommand().equals("Send to"))
          send(ne);
        else if(e.getActionCommand().equals("Online"))
          setOnline(ne);
        else if(e.getActionCommand().equals("Busy"))
          setBusy(ne);
        else if(e.getActionCommand().equals("Offline"))
          setOffline(ne);
      }
    };

    entryPopup.add("Send to").addActionListener(al);
    entryPopup.add(changeStatus = new JMenu("Change status"));
    changeStatus.add("Online").addActionListener(al);
    changeStatus.add("Busy").addActionListener(al);
    changeStatus.add("Offline").addActionListener(al);

    frame = new JFrame();
    frame.setTitle("IM network");
    frame.setBackground(Color.lightGray);
    frame.getContentPane().setLayout(new BorderLayout());

    frame.getContentPane().add("Center", jt = new JTable(entries));
    jt.setDefaultRenderer(NetworkEntry.class, renderer);
    jt.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if(e.isPopupTrigger())
          entryPopup.show(e.getComponent(), e.getX(), e.getY());
      }
    });

    JPanel jp = null;
    JButton jb = null;
    frame.getContentPane().add("South", jp = new JPanel());
    jp.setLayout(new FlowLayout());
/*
    jp.add(jb = new JButton("Submit"));
    jb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        submitText();
      }
    });
*/

    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        e.getWindow().dispose();
      }
    });
    frame.pack();
    frame.setSize(500, 600);
    frame.setVisible(true);
  }

  void send(NetworkEntry ne) {

    if(sendDialog != null)
    {
      sendDialog.dispose();
      sendDialog = null;
    }
    sendDialog = new SendDialog(frame, "Send from  ", ne);
    sendDialog.setVisible(true);
  }
  void setOnline(NetworkEntry ne) {
    ne.setStatus((byte)1);
  }
  void setBusy(NetworkEntry ne) {
    ne.setStatus((byte)2);
  }
  void setOffline(NetworkEntry ne) {
    ne.setStatus((byte)3);
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

  public void addMessagingNetworkListener(MessagingNetworkListener l)
  {
    System.out.println("addMessagingNetworkListener");
    networkListeners.add(l);
  }
  public void removeMessagingNetworkListener(MessagingNetworkListener l) throws MessagingNetworkException
  {
    System.out.println("removeMessagingNetworkListener");
    networkListeners.remove(l);
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

  public void logout(String srcLoginId, int endUserReason) throws MessagingNetworkException
  {
    logout(srcLoginId);
  }

  public void setClientStatus(String srcLoginId, int status, int endUserReason) throws MessagingNetworkException
  {
    setClientStatus(srcLoginId, status);
  }

  public void logout(String srcLoginId) throws MessagingNetworkException
  {
    NetworkEntry ne = entries.get(srcLoginId);
    ne.setLoggedIn(false);
    showMessage(srcLoginId + " logout");
  }

  public void addToContactList(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {
    showMessage(srcLoginId + " addToContactList " + dstLoginId);
    entries.get(srcLoginId).getContacts().add(entries.get(dstLoginId));
  }
  public void removeFromContactList(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {
    showMessage(srcLoginId + " removeFromContactList " + dstLoginId);
    entries.get(srcLoginId).getContacts().remove(entries.get(dstLoginId));
  }

  public void setClientStatus(String srcLoginId, int status) throws MessagingNetworkException
  {
    System.out.println("setClientStatus " + srcLoginId + " in " + status);
    entries.get(srcLoginId).setStatus((byte)status);
  }
  public int getClientStatus(String srcLoginId) throws MessagingNetworkException
  {
    System.out.println("getClientStatus " + srcLoginId + " is " + entries.get(srcLoginId).getStatus());
    return entries.get(srcLoginId).getStatus();
  }

  public int getStatus(String srcLoginId, String dstLoginId) throws MessagingNetworkException {
    System.out.println("getStatus " + srcLoginId + " is " + entries.get(dstLoginId).getStatus());
    return entries.get(dstLoginId).getStatus();
  }

  public void sendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {
    showMessage(srcLoginId + " send message " + text + " to " + dstLoginId);
    messageSend(srcLoginId, dstLoginId, text);
  }

  void messageSend(String from, String to, String text) {
    if(from == null || to == null || from.length() == 0 || to.length() == 0)
      return;
    NetworkEntry nef = entries.get(from);
    NetworkEntry net = entries.get(to);

    if(net.getLoggedIn())
      for(Iterator nli = networkListeners.iterator(); nli.hasNext(); )
        ((MessagingNetworkListener)nli.next()).messageReceived(getNetworkId(), from, to, text);

//    (new PopupDialog(frame, "Message sent from "+from+" to "+to,text)).show();
  }

  void showMessage(String s)
  {
    JFrame frame = new JFrame(s);
    JLabel jLabel = new JLabel(s);
    jLabel.setPreferredSize(new Dimension(120, 30));
    frame.getContentPane().add(jLabel, BorderLayout.CENTER);

    frame.pack();
    frame.setVisible(true);
  }

  class EntriesTableCellRenderer implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      if(value instanceof NetworkEntry)
        return (NetworkEntry)value;
      else
        return null;
    }
  }

  class EntriesTableModel implements TableModel {
    Hashtable entries = new Hashtable();
    java.util.List entriesList = new ArrayList();
    java.util.List tls = new ArrayList();

    public void update(NetworkEntry ne) {
      int index = entriesList.indexOf(ne);
      if(index < 0)
        return;
      for(Iterator i = tls.iterator(); i.hasNext(); )
        ((TableModelListener)i.next()).tableChanged(new TableModelEvent(this, index));
    }
    public Collection values() { return entries.values(); }
    public Set keySet() { return entries.keySet(); }

    public NetworkEntry get(String id) {
      NetworkEntry ne = (NetworkEntry)entries.get(id);
      if(ne == null)
        return put(id, (byte)3, false);
      else
        return ne;
    }
    public NetworkEntry get(int index) { return (NetworkEntry)entriesList.get(index); }

    public NetworkEntry put(String id, byte st, boolean loggedIn) {
      NetworkEntry ne = new NetworkEntry(id, st, loggedIn);
      Object o = entries.get(id);

      if(o != null) {
        int index = entriesList.indexOf(o);
        entriesList.set(index, ne);
        entries.put(id, ne);
        for(Iterator i = tls.iterator(); i.hasNext(); )
          ((TableModelListener)i.next()).tableChanged(new TableModelEvent(this, index));
      } else {
        entriesList.add(ne);
        entries.put(id, ne);
        for(Iterator i = tls.iterator(); i.hasNext(); )
          ((TableModelListener)i.next()).tableChanged(new TableModelEvent(this, entries.size() - 1, entries.size() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
      }
      return ne;
    }

    public void addTableModelListener(TableModelListener l) { tls.add(l); }
    public void removeTableModelListener(TableModelListener l) { tls.remove(l); }

    public int getColumnCount() { return 2; }
    public Class getColumnClass(int columnIndex) {
      switch(columnIndex) {
        case 0: return String.class;
        case 1: return NetworkEntry.class;
        default: return null;
      }
    }
    public String getColumnName(int columnIndex) {
      switch(columnIndex) {
        case 0: return "Index";
        case 1: return "Login";
        default: return null;
      }
    }

    public int getRowCount() { return entries.size(); }

    public Object getValueAt(int rowIndex, int columnIndex) {
      if(columnIndex == 0)
        return ""+rowIndex;
      else if(columnIndex == 1)
        return entriesList.get(rowIndex);
      else
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) { }
  }

  class NetworkEntry extends JLabel {
    private String loginId;
    private byte status;
    private boolean loggedIn;
    private java.util.List contacts = new ArrayList();

    NetworkEntry(String id, byte st, boolean loggedIn) {
      super();
      setLoginId(id);
      setStatus(st);
      setLoggedIn(loggedIn);
      addMouseListener(new MouseAdapter() {
        public void mouseReleased(MouseEvent e) {
          if(e.isPopupTrigger())
            entryPopup.show(e.getComponent(), e.getX(), e.getY());
        }
      });
    }

    String getLoginId() { return loginId; }
    private void setLoginId(final String id) {
      loginId = id;
//      try {
//        SwingUtilities.invokeAndWait(new Runnable() {
//          public void run() {
//            setText(id);
//          }
//        });
//      } catch(Exception e) {
//        e.printStackTrace();
//      }
      setText(id);
      entries.update(this);
    }

    java.util.List getContacts() { return contacts; }

    void receivedMessage(String srcLoginId, String dstLoginId, String text)
    {
      for(Iterator nli = networkListeners.iterator(); nli.hasNext(); )
      {
        ((MessagingNetworkListener)nli.next()).messageReceived(getNetworkId(), srcLoginId, dstLoginId, text);
        System.out.println("messageRecieved(getNetworkId() = "+getNetworkId()+", getLoginId() = "+srcLoginId+", dstLoginId = "+dstLoginId+", text = "+text+");");
      }
    }

    boolean getLoggedIn() { return loggedIn; }
    void setLoggedIn(boolean li) { loggedIn = li; }

    byte getStatus() { return status; }
    void setStatus(byte st) {
      status = st;
      try {
        switch(status) {
          case 1: //SwingUtilities.invokeAndWait(new Runnable() {
                  //  public void run() {
                      setForeground(Color.green);
                  //  }
                  //});
                  break;
          case 2: //SwingUtilities.invokeAndWait(new Runnable() {
                  //  public void run() {
                      setForeground(Color.black);
                  //  }
                  //});
                  break;
          case 3: //SwingUtilities.invokeAndWait(new Runnable() {
                  //  public void run() {
                      setForeground(Color.red);
                  //  }
                  //});
                  break;
          default://SwingUtilities.invokeAndWait(new Runnable() {
                  //  public void run() {
                      setForeground(Color.yellow);
                  //  }
                  //});
                  break;
        }
      } catch(Exception e) {
        e.printStackTrace();
      }

      for(Iterator i = entries.values().iterator(); i.hasNext(); )
      {
        NetworkEntry ne = (NetworkEntry)i.next();

//        if(ne.getContacts().contains(this) && ne.getLoggedIn())
//        if(!ne.getLoginId().equals(getLoginId()))
          for(Iterator nli = networkListeners.iterator(); nli.hasNext(); ) {
            ((MessagingNetworkListener)nli.next()).statusChanged(getNetworkId(), ne.getLoginId(), getLoginId(), status, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
            System.out.println("statusChanged(getNetworkId() = "+getNetworkId()+", ne.getLoginId() = "+ne.getLoginId()+", getLoginId() = "+getLoginId()+", status = "+status+");");
          }
      }
      entries.update(this);
    }
  }

  class SendDialog extends JDialog
  {
    JTextArea textArea;
/*
    String getSelectedLogin(int number)
    {
      int count = 0;
      for(Iterator i = entries.keySet().iterator(); i.hasNext(); )
      {
        if(count==number)
        {
          NetworkEntry networkEntry = (NetworkEntry)i.next();
          return networkEntry.getLoginId();
        }
        count++;
      }
      return "";
    }
*/

    SendDialog(JFrame owner, String title, final NetworkEntry from) {
      super(owner, title+from.getLoginId());

      JPanel jp;
      final JComboBox jcb = new JComboBox();

      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          e.getWindow().dispose();
        }
      });

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add("Center", textArea = new JTextArea());

      getContentPane().add("North", jp = new JPanel());
      jp.setLayout(new FlowLayout());
      jp.add(new JLabel("to"));
      jp.add(jcb);
      for(Iterator i = entries.keySet().iterator(); i.hasNext(); )
        jcb.addItem(i.next());

      getContentPane().add("South", jp = new JPanel());
      jp.setLayout(new FlowLayout());
      JButton jb;
      jp.add(jb = new JButton("Ok"));
      jb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try
          {
            from.receivedMessage(from.getLoginId(), (String)jcb.getSelectedItem(), textArea.getText());
          }
          catch(Exception ex)
          {
          }
        }
      });
      jp.add(jb = new JButton("Cancel"));
      jb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dispose();
        }
      });
    }
  }
  public UserDetails getUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public void sendContacts(String srcLoginId, String dstLoginId, String[] nicks, String[] loginIds) throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  /**
    If null is returned, then no users found.
    @see UserSearchResults
  */
  public UserSearchResults searchUsers(
    String srcLoginId,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern) throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public boolean isAuthorizationRequired(String srcLoginId, String dstLoginId)
      throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public void authorizationRequest(String srcLoginId, String dstLoginId, String reason)
      throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public void authorizationResponse(String srcLogin, String dstLogin, boolean grant)
      throws MessagingNetworkException
  {
    throw new MessagingNetworkException("TODO: not implemented", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
  }
  public void init()
  {
  if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("TODO: empty init();");
  }
  public void deinit()
  {
  if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("TODO: empty deinit();");
  }
  public long startLogin(String srcLoginId, String password, String[] contactList, int status) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startSendMessage(String srcLoginId, String dstLoginId, String text) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
  public long startGetUserDetails(String srcLoginId, String dstLoginId) throws MessagingNetworkException
  {throw new RuntimeException("TODO: not implemented.");}
}
