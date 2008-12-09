package org.openmim.test;

import java.util.*;

import org.openmim.*;
import org.openmim.mn.MessagingNetwork;
import org.openmim.mn.MessagingNetworkListener;
import org.openmim.mn.MessagingNetworkAdapter;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.joe.*;
import org.w3c.dom.*;
import org.apache.xerces.parsers.*;


public final class MessagingNetworkScriptClient
{
//  { log("instance initializer 1."); }

  private Hashtable meetingId2meeting = new Hashtable();
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(MessagingNetworkScriptClient.class.getName());

//  { log("instance initializer 2."); }

  private final DOMParser parser = new DOMParser();

//  { log("instance initializer 3."); }

  private MessagingNetwork messagingNetworkImpl;
  private Hashtable userId2user;
  class User
  {
    final String nick;
    final String loginId;
    final String password;
    User(String loginId, String nick, String password)
    {
      this.nick = nick;
      this.loginId = loginId;
      this.password = password;
    }
  }
  public class Context
  {
    MessagingNetworkListener listener;
    final User user;
    private long startTime = 0;
    private long endTime = 0;
    private int opCount = 0;
    
    Context(User user)
    {
      this.user = user;
    }
    
    public String getSrcLoginId()
    {
      return user.loginId;
    }
    
    public String getPassword()
    {
      return user.password;
    }
    
    final synchronized void markStartTime()
    {
      Context.this.startTime = System.currentTimeMillis();
      Context.this.endTime = 0;
    }
    
    final synchronized void markEndTime()
    {
      Context.this.endTime = System.currentTimeMillis();
      report(true);
    }
    
    final synchronized void increaseOperationCount()
    {
      ++opCount;
      if ((opCount & 63) == 1) report(false);
    }
    
    final private void report(boolean finalReport)
    {
      System.out.println((finalReport ? "FINAL REPORT ": "")+getSrcLoginId()+", ops/sec: "+getOpsPerSecond());
    }
    
    final private double getOpsPerSecond()
    {
      return ((double) 1000 * opCount) /
          (
            (Context.this.endTime == 0 ? System.currentTimeMillis() : Context.this.endTime) 
            - Context.this.startTime
          );
    }
  }

  public MessagingNetworkScriptClient()
  {
  }

private void addContactListItem(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String dst = getLoginId(getAttr(node, "dst-login-id"));
  messagingNetworkImpl.addToContactList(ctx.getSrcLoginId(), dst);
}

private void getUserInfo(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String dst = getLoginId(getAttr(node, "dst-login-id"));
  UserDetails d = messagingNetworkImpl.getUserDetails(ctx.getSrcLoginId(), dst);
  log("User_ info received for "+dst+": nick="+d.getNick()+", name="+d.getRealName()+", email="+d.getEmail());
}

private void sendRandomContact(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String dst = getLoginId(getAttr(node, "dst-login-id"));
  messagingNetworkImpl.sendContacts(ctx.getSrcLoginId(), dst, new String[] {"random nick "+(int) (100 * Math.random())}, new String[] {String.valueOf((100000 + (int) (10000000 * Math.random())))});
}

private static int expression(String expression) throws ExpectException
{
  final String errprefix = "expression \"" + expression + "\"";
  if ((expression) == null) Lang.EXPECT_NOT_NULL(expression, "expression");
  StringTokenizer st = new StringTokenizer(expression);
  //gettok
  if (st.hasMoreTokens()) Lang.EXPECT(st.hasMoreTokens(), errprefix + " must not be empty, but it is.");
  String tok = st.nextToken();
  //gottok
  if (tok.startsWith("random"))
  {
    //gettok
    tok = tok.substring("random".length());
    if (tok.length() == 0)
    {
      if (st.hasMoreTokens()) Lang.EXPECT(st.hasMoreTokens(), errprefix + " must have a \"(\" after \"random\", but it does not.");
      tok = st.nextToken();
    }
    //gottok
    Lang.EXPECT(tok.startsWith("("), errprefix + " must have a \"(\" after \"random\", but it does not.");
    //gettok
    tok = tok.substring("(".length());
    if (tok.length() == 0)
    {
      if (st.hasMoreTokens()) Lang.EXPECT(st.hasMoreTokens(), errprefix + " must have a non-negative integer after \"random(\", but it does not.");
      tok = st.nextToken();
    }
    //gottok
    String rangeStart_s = NumberParseUtil.extractAllDigitsPrefix(tok);
    if (rangeStart_s.length() > 0) Lang.EXPECT(rangeStart_s.length() > 0, errprefix + " must have a non-negative integer after \"random(\", but it does not.");
    int rangeStart = NumberParseUtil.parseInt(rangeStart_s, "rangeStart_s in " + errprefix);
    Lang.EXPECT_NON_NEGATIVE(rangeStart, "rangeStart"); //to avoid int overflow
    //gettok
    tok = tok.substring(rangeStart_s.length());
    if (tok.length() == 0)
    {
      if (st.hasMoreTokens()) Lang.EXPECT(st.hasMoreTokens(), errprefix + " must have a \",\", but it does not.");
      tok = st.nextToken();
    }
    //gottok
    Lang.EXPECT(tok.startsWith(","), errprefix + " must have a \",\", but it does not.");
    //gettok
    tok = tok.substring(",".length());
    if (tok.length() == 0)
    {
      if (st.hasMoreTokens()) Lang.EXPECT(st.hasMoreTokens(), errprefix + " must have a non-negative integer after \",\", but it does not.");
      tok = st.nextToken();
    }
    //gottok
    String rangeEnd_s = NumberParseUtil.extractAllDigitsPrefix(tok);
    if (rangeEnd_s.length() > 0) Lang.EXPECT(rangeEnd_s.length() > 0, errprefix + " must have a non-negative integer after \",\", but it does not.");
    int rangeEnd = NumberParseUtil.parseInt(rangeEnd_s, "rangeEnd_s in " + errprefix);
    Lang.EXPECT_NON_NEGATIVE(rangeEnd, "rangeEnd"); //to avoid int overflow
    //gettok
    tok = tok.substring(rangeEnd_s.length());
    if (tok.length() == 0)
    {
      if (st.hasMoreTokens()) Lang.EXPECT(st.hasMoreTokens(), errprefix + " must have a \")\", but it does not.");
      tok = st.nextToken();
    }
    //gottok
    Lang.EXPECT(tok.startsWith(")"), errprefix + " must have a \")\", but it does not.");
    //gettok
    tok = tok.substring(")".length()) + (st.hasMoreTokens() ? st.nextToken("").trim() : "");
    if (tok.length() == 0) Lang.EXPECT(tok.length() == 0, errprefix + " contains extra characters \"" + tok + "\" after the expression.");
    long rnd = Math.round(rangeStart + (rangeEnd - rangeStart) * Math.random());
    if (rnd < rangeStart)
      rnd = rangeStart;
    if (rnd > rangeEnd)
      rnd = rangeEnd;
    return (int) rnd;
  }
  else
  {
    int n = NumberParseUtil.parseInt(expression.trim(), "n");
    Lang.EXPECT_NON_NEGATIVE(n, "n");
    return n;
  }
  //"infinity" value is parsed in the loop(Node, Context)
}
private static String getAttr(Node node, String attrName)
{
  Node n2 = node.getAttributes().getNamedItem(attrName);
  if (n2 == null)
    return null;
  else
    return n2.getNodeValue();
}
private String getLoginId(String userId) throws ExpectException
{
  User u = getUser(userId);
  if (u != null)
    return u.loginId;
  else
    return Long.toString(expression(userId));
}
private User getUser(String userId) throws ExpectException
{
  return (User) userId2user.get(userId);
}
private static int integerNonNegative(String integer) throws ExpectException
{
  if ((integer) == null) Lang.EXPECT_NOT_NULL(integer, "integer");
  final String errprefix = "integer \"" + integer + "\"";
  integer = integer.trim();
  if (integer.length() > 0) Lang.EXPECT(integer.length() > 0, errprefix + " must be a non-negative integer, but it is not.");
  int integer_int = NumberParseUtil.parseInt(integer, "integerNonNegative");
  Lang.EXPECT_NON_NEGATIVE(integer_int, "integerNonNegative");
  return integer_int;
}
private void interpretBehavior(Node behavior, Context ctx) throws ExpectException, MessagingNetworkException
{
  loop(behavior.getChildNodes(), 1, true, ctx);
}
private void interpretScript(Document doc) throws ExpectException
{
  NodeList behaviorDefs = doc.getDocumentElement().getElementsByTagName("behavior");
  if (behaviorDefs.getLength() > 0) Lang.EXPECT(behaviorDefs.getLength() > 0, "list of <behavior> definitions cannot be empty, but it is.");
  final Hashtable id2behaviorDefNode = new Hashtable(behaviorDefs.getLength());
  for (int i = 0; i < behaviorDefs.getLength(); i++)
  {
    final Node behaviorDef = behaviorDefs.item(i);
    String behaviorId = behaviorDef.getAttributes().getNamedItem("id").getNodeValue();
    id2behaviorDefNode.put(behaviorId, behaviorDef);
  }
  NodeList userDefs = doc.getDocumentElement().getElementsByTagName("define-user");
  userId2user = new Hashtable(userDefs.getLength());
  for (int i = 0; i < userDefs.getLength(); i++)
  {
    final Node def = userDefs.item(i);
    String nick = def.getAttributes().getNamedItem("nick").getNodeValue();
    String loginId = "" + expression(def.getAttributes().getNamedItem("login-id").getNodeValue());
    Node passwdNode = def.getAttributes().getNamedItem("password");
    String password = (passwdNode == null ? null : passwdNode.getNodeValue());
    User u = new User(loginId, nick, password);
    userId2user.put(nick, u);
    userId2user.put(loginId, u);
  }

  //
  NodeList thrDefs = doc.getDocumentElement().getElementsByTagName("launch-thread");
  if (thrDefs.getLength() > 0) Lang.EXPECT(thrDefs.getLength() > 0, "list of <launch-thread> definitions cannot be empty, but it is.");
  for (int i = 0; i < thrDefs.getLength(); i++)
  {
    final Node def = thrDefs.item(i);
    String behaviorId = def.getAttributes().getNamedItem("behavior-id").getNodeValue();
    final Node behavior = (Node) id2behaviorDefNode.get(behaviorId);
    if (behavior != null) Lang.EXPECT(behavior != null, "behavior with id \"" + behaviorId + "\" doesn't exist.");
    String srcLoginId = def.getAttributes().getNamedItem("src-login-id").getNodeValue();
    final User user = getUser(srcLoginId);
    if (user != null) Lang.EXPECT(user != null, "user with loginid '" + srcLoginId + "' not found.");
    if (user.password != null) Lang.EXPECT(user.password != null, "password for user with loginid '" + srcLoginId + "' not specified, cannot login.");
    new Thread("[" + srcLoginId + "/" + behaviorId + "]")
    {
      public void run()
      {
        logt("thread started.");
        try
        {
          interpretBehavior(behavior, new Context(user));
        }
        catch (Throwable tr)
        {
          if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("unhandled exception, doing System.exit(1)", tr);
          System.exit(1);
        }
        logt("thread finished.");
      }
    }
    .start();
  }
}
private static void log(String s)
{
  if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("mns: " + s);
}
private void log(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String msg = node.getAttributes().getNamedItem("msg").getNodeValue();
  if (!msg.startsWith("###")) msg = "###\t"+msg;
  log(msg);
}
//<!ELEMENT login (contact-list)>
//<!ELEMENT contact-list (item)+>
//<!ELEMENT item EMPTY>
//<!ATTLIST item login-id CDATA #REQUIRED>
private void login(Node node, final Context ctx) throws ExpectException, MessagingNetworkException
{
  try
  {
    Node clist = ((Element) node).getElementsByTagName("contact-list").item(0);
    Vector v = null;
    if (clist != null)
    {
      NodeList items = ((Element) clist).getElementsByTagName("item");
      v = new Vector(items.getLength());
      for (int i = 0; i < items.getLength(); i++)
      {
        Node item = items.item(i);
        String id = item.getAttributes().getNamedItem("login-id").getNodeValue();
        v.add(getLoginId(id));
      }
    }
    messagingNetworkImpl.login(ctx.getSrcLoginId(), ctx.getPassword(), (v == null ? null : (String[]) v.toArray(new String[] {})), MessagingNetwork.STATUS_ONLINE);
    ctx.markStartTime();
  }
  catch (RuntimeException ee)
  {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ERROR: invalid <login> specification");
    throw ee;
  }
}
private void logout(Context ctx)
{
  try
  {
    ctx.markEndTime();
    messagingNetworkImpl.logout(ctx.getSrcLoginId());
  }
  catch (MessagingNetworkException ex)
  {
  if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", ex);
  }
}
private static void logt(String s)
{
  log(s);
  //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(/*"mns, " + Thread.currentThread().getName() + ": " + */s);
}
private void loop(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String timess = node.getAttributes().getNamedItem("times").getNodeValue();
  if ("infinity".equals(timess))
  {
    loopForever(node.getChildNodes(), ctx);
  }
  else
  {
    int times = expression(timess);
    loop(node.getChildNodes(), times, false, ctx);
  }
}
private void loop(NodeList operations, int times, boolean silent, Context ctx) throws ExpectException, MessagingNetworkException
{
  if ((times) < 0) Lang.ASSERT_NON_NEGATIVE(times, "times");
  if (times == 0)
  {
    logt("loop times == 0, loop skipped");
    return;
  }
  for (int i = 0; i < times; i++)
  {
    if (!silent)
      logt("loop, pass " + (1 + i) + " of " + times);
    sequence(operations, ctx);
    ThreadUtil.sleep(1);
  }
  if (!silent)
    logt("loop finished");
}
private void loopForever(NodeList operations, Context ctx) throws ExpectException, MessagingNetworkException
{
  logt("loop times=infinity, first pass");
  for (;;)
  {
    sequence(operations, ctx);
    ThreadUtil.sleep(1);
    logt("loop times=infinity, next pass");
  }
}
public void main() throws Exception
{
  System.err.println("mns started, check log4j log files to see the output.");
  if (!(  messagingNetworkImpl == null  )) throw new AssertException("By design, this method cannot be called twice.");
  String name = PropertyUtil.getResourceFilePathName(getClass());
  log("loading properties...");
  Properties prop = PropertyUtil.loadResourceProperties(getClass(), name);
  String mnImplClassName = PropertyUtil.getRequiredProperty(prop, name + " resource", "messaging.network.impl.class.name").trim();
  String scriptResourceNameOrUri = PropertyUtil.getRequiredProperty(prop, name + " resource", "script.resource.name.or.uri").trim();
  Lang.EXPECT(scriptResourceNameOrUri.endsWith(".mns.xml"), "script.resource.name.or.uri property must end with \".mns.xml\", but it doesn't.");
  log("locating script file at \"" + scriptResourceNameOrUri + "\"...");
  String uri;
  if (scriptResourceNameOrUri.indexOf(':') == -1)
  {
    java.net.URL url = getClass().getResource(scriptResourceNameOrUri);
    if (url != null) Lang.EXPECT(url != null, scriptResourceNameOrUri + " resource cannot be found.");
    uri = url.toString();
  }
  else
  {
    uri = scriptResourceNameOrUri;
  }
  log("parsing script...");
  Document doc = parseXML(uri);
  log("parsed ok.");
  log("instantiating a MessagingNetwork impl, class name: \"" + mnImplClassName + "\"...");

  messagingNetworkImpl = (MessagingNetwork) Class.forName(mnImplClassName).newInstance();

  messagingNetworkImpl.init();

  messagingNetworkImpl.addMessagingNetworkListener(//
  new MessagingNetworkAdapter()
  {
    public void messageReceived(byte mnId, String src, String dst, String text)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("INCOMING EVENT: messageReceived(" + mnId + ", " + StringUtil.toPrintableString(src) + ", " + StringUtil.toPrintableString(dst) + ", " + (text == null ? null : "\""+(text.length() < 80 ? text + "\"" : text.substring(0, 80)+"...\" (the rest not printed, message length="+text.length())) + ")");
    }

    public void contactsReceived(byte networkId, String from, String to, String[] contactsUins, String[] contactsNicks)
    {
      StringBuffer sb = new StringBuffer("INCOMING EVENT: contactsReceived from " + from + " to " + to + ", number of contacts="+contactsNicks.length+":\r\n");
      int i = 0;
      while (i < contactsNicks.length)
      {
        sb.append(
          "  nick="+StringUtil.toPrintableString(contactsNicks[i])+"\t\t"+
          "uin ="+StringUtil.toPrintableString(contactsUins[i])+"\r\n");
        i++;
      }
      String s = sb.toString();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info(s);
    }

    public void statusChanged(byte mnId, String src, String dst, int status, int reasonLogger, String reasonMessage, int endUserReasonCode)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("INCOMING EVENT: statusChanged(" + mnId + ", " + StringUtil.toPrintableString(src) + ", " + StringUtil.toPrintableString(dst) + ", " + org.openmim.icq2k.StatusUtil.translateStatusMimToString(status) + ", " + reasonMessage + ", " + StringUtil.toPrintableString(MessagingNetworkException.getEndUserReasonMessage(endUserReasonCode))+")");
    }
    public void authorizationResponse(byte networkId, String srcLoginId, String dstLoginId, boolean grant)
    {
      StringBuffer sb = new StringBuffer(
        "authorization response from " + srcLoginId +
        " to " + dstLoginId + ", granted: "+grant);
      String s = sb.toString();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("INCOMING EVENT: "+s);
    }

    public void authorizationRequest(byte networkId, String srcLoginId, String dstLoginId, String reason)
    {
      StringBuffer sb = new StringBuffer(
        "authorization request from " + srcLoginId +
        " to " + dstLoginId + ", reason"+
        (reason == null ? ": null" : " (" + reason.length() + " chars):\r\n\"" + reason + "\""));
      String s = sb.toString();
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("INCOMING EVENT: "+s);
    }
  });

  log("launching script interpreter...");
  interpretScript(doc);
  log("script interpreter launched.");
}
public static void main(String[] args)
{
  try
  {
    log("starting.");
    MessagingNetworkScriptClient c = new MessagingNetworkScriptClient();
    c.main();
  }
  catch (Throwable tr)
  {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception", tr);
    System.exit(1);
  }
}
//<!ELEMENT on-msg (reply)+>
//<!ELEMENT reply (#PCDATA)>
private void onMsg(final Node node, final Context ctx) throws ExpectException, MessagingNetworkException
{
  MessagingNetworkListener oldlistener = ctx.listener;
  ctx.listener = new MessagingNetworkAdapter()
  {
    public void contactsReceived(byte networkId, String from, String to, String[] contactsUins, String[] contactsNicks)
    {
    }
    public void authorizationResponse(byte networkId, String srcLoginId, String dstLoginId, boolean grant)
    {
    }
    public void authorizationRequest(byte networkId, String srcLoginId, String dstLoginId, String reason)
    {
    }
    public void messageReceived(byte netid, String src, String dst, String msgtext)
    {
      if (dst.equals(ctx.getSrcLoginId()))
      {
        try
        {
          NodeList items = ((Element) node).getElementsByTagName("reply");
          for (int i = 0; i < items.getLength(); i++)
          {
            Node item = items.item(i);
            String replyText = item.getChildNodes().item(0).getNodeValue().trim();
            messagingNetworkImpl.startSendMessage(dst, src, replyText);
            ctx.increaseOperationCount();
          }
        }
        catch (RuntimeException ee)
        {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("invalid <on-msg> specification", ee);
          System.exit(1);
        }
        catch (MessagingNetworkException me)
        {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception, logging out", me);
          logout(ctx);
        }
      }
    }
    public void statusChanged(byte netid, String src, String dst, int status, int reasonLogger, String reasonMessage, int endUserReasonCode)
    {
    }
  };
  messagingNetworkImpl.addMessagingNetworkListener(ctx.listener);
  if (oldlistener != null)
    messagingNetworkImpl.removeMessagingNetworkListener(oldlistener);
}
private Document parseXML(String uri) throws Exception
{
  parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
  parser.parse(uri);
  return parser.getDocument();
}
private void printMsgReceived()
{
}
private void printStatusChangeReceived()
{
}
private void removeContactListItem(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String dst = getLoginId(getAttr(node, "dst-login-id"));
  messagingNetworkImpl.removeFromContactList(ctx.getSrcLoginId(), dst);
}

static int testMsgNumber = 1;
static final Object testMsgNumberLock = new Object();

//<!ELEMENT send-msg (#PCDATA|random|src-login-id)*>
//<!ATTLIST send-msg to CDATA #REQUIRED>
//<!ELEMENT random EMPTY>
//<!ELEMENT src-login-id EMPTY>
//
//<send-msg type="with-ascii-00-to-31-appended"></send-msg>
//<send-msg type="with-readable-stuff-appended" msg-size-chars="10000"></send-msg>
private void sendMsg(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  try
  {
    String to = getLoginId(getAttr(node, "to"));

    //<send-msg type="with-ascii-00-to-31-appended"></send-msg>
    //<send-msg type="with-readable-stuff-appended" msg-size-chars="10000"></send-msg>

    final int TYPE_NORMAL = 0;
    final int TYPE_CTRL = 1;
    final int TYPE_BIG = 2;

    String type = getAttr(node, "type");
    String msgSizeChars = getAttr(node, "msg-size-chars");
    int msgSizeChars_int = 0;

    int msgType = -1;

    if (type == null)
    {
      msgType = TYPE_NORMAL;
      if (msgSizeChars == null) Lang.EXPECT(msgSizeChars == null, "msgSizeChars must be null when type attr absent, but it is \""+msgSizeChars+"\".");
    }
    else
    if (type.equals("with-ascii-00-to-31-appended"))
    {
      msgType = TYPE_CTRL;
      if (msgSizeChars == null) Lang.EXPECT(msgSizeChars == null, "msgSizeChars must be null when type attr is \"with-ascii-00-to-31-appended\", but it is \""+msgSizeChars+"\".");
    }
    else
    if (type.equals("with-readable-stuff-appended"))
    {
      msgType = TYPE_BIG;
      msgSizeChars_int = NumberParseUtil.parseInt(msgSizeChars, "msgSizeChars attr");
    }
    else
    {
      Lang.EXPECT_FALSE("invalid type attr: "+StringUtil.toPrintableString(type));
    }

    Node child = node.getFirstChild();
    String msgtext = (child == null ? null : child.getNodeValue());
    if (msgtext == null)
      msgtext = "";
    else
      msgtext = msgtext.trim();

    int testMsgNum;
    synchronized (testMsgNumberLock)
    {
      testMsgNum = testMsgNumber++;
    }
    msgtext = "testmsg #" + testMsgNum + ": " + msgtext;

    final String CHUNK = "\r\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$";
    final int CHUNK_LEN = CHUNK.length();

    if (msgtext.length() < msgSizeChars_int)
    {
      if ((msgType) != (TYPE_BIG)) Lang.ASSERT_EQUAL(msgType, TYPE_BIG, "msgType", "TYPE_BIG");
      StringBuffer sb = new StringBuffer(msgSizeChars_int);
      sb.append(msgtext);

      int len = msgtext.length();

//      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("1 DBG len="+len+", msgtext.len()="+msgtext.length()+", msgSizeChars_int="+msgSizeChars_int+", sb.length()="+sb.length());

      while (msgSizeChars_int - len > CHUNK_LEN)
      {
        sb.append(CHUNK);
        len += CHUNK_LEN;
//      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("2 DBG len="+len+", msgtext.len()="+msgtext.length()+", msgSizeChars_int="+msgSizeChars_int+", sb.length()="+sb.length());
      }
//      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("3 DBG len="+len+", msgtext.len()="+msgtext.length()+", msgSizeChars_int="+msgSizeChars_int+", sb.length()="+sb.length());
      if (msgSizeChars_int > len)
        sb.append(CHUNK.substring(0, msgSizeChars_int - len));
//      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("4 DBG len="+len+", msgtext.len()="+msgtext.length()+", msgSizeChars_int="+msgSizeChars_int+", sb.length()="+sb.length());
      msgtext = sb.toString();
//      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("5 DBG len="+len+", msgtext.len()="+msgtext.length()+", msgSizeChars_int="+msgSizeChars_int);
      if ((msgtext.length()) != (msgSizeChars_int)) Lang.ASSERT_EQUAL(msgtext.length(), msgSizeChars_int, "msgtext.length()", "msgSizeChars_int");
    }
    else
    if (msgType == TYPE_CTRL)
    {
      StringBuffer sb = new StringBuffer(msgtext.length()+2+32);
      sb.append(msgtext).append("\r\n");
      for (char i = 0; i < 31; ++i)
      {
        sb.append(i);
      }
      msgtext = sb.toString();
    }

    log("msgtext.length()==" + msgtext.length());
    log("msgtext.substring(0, 80)==\"" + msgtext.substring(0, Math.min(msgtext.length(), 80)) + "\"");
    messagingNetworkImpl.startSendMessage(ctx.getSrcLoginId(), to, msgtext);
    ctx.increaseOperationCount();
  }
  catch (RuntimeException ee)
  {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ERROR: invalid <send-msg> specification");
    throw ee;
  }
}

private void sequence(NodeList statements, Context ctx) throws ExpectException, MessagingNetworkException
{
  for (int i = 0; i < statements.getLength(); i++)
  {
    Node node = statements.item(i);
    if (node.getNodeType() != node.ELEMENT_NODE)
      continue;
    statement(node, ctx);
  }
}
//<!ELEMENT set-status EMPTY>
//<!ATTLIST set-status status (online|offline|busy) #REQUIRED>
private void setStatus(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String dst = getAttr(node, "status");
  int st = -1;
  if ("online".equals(dst))
    st = MessagingNetwork.STATUS_ONLINE;
  else
    if ("busy".equals(dst))
      st = MessagingNetwork.STATUS_BUSY;
    else
    if ("random-online-busy".equals(dst))
      st = (Math.random() > 0.5 ? MessagingNetwork.STATUS_ONLINE : MessagingNetwork.STATUS_BUSY);
    else
      if ("offline".equals(dst))
      {
        logout(ctx);
        return;
      }
      else
        Lang.EXPECT_FALSE("invalid status value: '" + dst + "'");
  messagingNetworkImpl.setClientStatus(ctx.getSrcLoginId(), st);
}
private void sleep(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  String secondsAttr = getAttr(node, "seconds");
  String minutesAttr = getAttr(node, "minutes");
  if (secondsAttr != null || minutesAttr != null) Lang.EXPECT(secondsAttr != null || minutesAttr != null, "seconds and minutes cannot be both missing in <sleep>");
  if (secondsAttr == null)
    secondsAttr = "0";
  if (minutesAttr == null)
    minutesAttr = "0";
  int sec = expression(secondsAttr);
  long min = expression(minutesAttr);
  long sleepTime = min * 60 + sec;
  if ((sleepTime) < 0) Lang.ASSERT_NON_NEGATIVE(sleepTime, "<sleep> time");
  if (sleepTime == 0)
    logt("sleeping " + sec + " seconds (sleep ignored)");
  else
  {
    logt("sleeping " + (min == 0 ? "" : min + " minutes ") + (sec == 0 ? "" : sec + " seconds"));
    ThreadUtil.sleep(sleepTime * 1000);
  }
}
private void statement(Node node, Context ctx) throws ExpectException, MessagingNetworkException
{
  //<!ELEMENT behavior (loop|sleep|send-msg|add-contact-list-item|remove-contact-list-item|
  //  login|logout|set-status|on-msg)+>
  //<!ELEMENT loop     (loop|sleep|send-msg|add-contact-list-item|remove-contact-list-item|
  //  login|logout|set-status|on-msg)+>

  String tag = node.getNodeName();
  //statements that don't print "op: statement"
  if ("log".equals(tag))
    log(node, ctx);
  else
  if ("sleep".equals(tag))
    sleep(node, ctx);
  else
  if ("thread-meeting".equals(tag))
    threadMeeting(node, ctx);
  else
  if ("try".equals(tag))
    try_(node, ctx);
  else
  if ("switch".equals(tag))
    switch_(node, ctx);
  else
  {
    //statements that print "op: statement"
    logt("op: " + tag);

    if ("loop".equals(tag))
      loop(node, ctx);
    else
    if ("login".equals(tag))
      login(node, ctx);
    else
    if ("logout".equals(tag))
      logout(ctx);
    else
    if ("send-msg".equals(tag))
      sendMsg(node, ctx);
    else
    if ("set-status".equals(tag))
      setStatus(node, ctx);
    else
    if ("add-contact-list-item".equals(tag))
      addContactListItem(node, ctx);
    else
    if ("remove-contact-list-item".equals(tag))
      removeContactListItem(node, ctx);
    else
    if ("get-user-info".equals(tag))
      getUserInfo(node, ctx);
    else
    if ("send-random-contact".equals(tag))
      sendRandomContact(node, ctx);
    else
    if ("on-msg".equals(tag))
      onMsg(node, ctx);
    else
      Lang.EXPECT_FALSE("invalid operation: \"" + tag + "\"");
  }
  ThreadUtil.sleep(1);
}
//<thread-meeting barrier-participants-count="2" meeting-id="meeting1"/>
private void threadMeeting(Node node, final Context ctx) throws ExpectException, MessagingNetworkException
{
  try
  {
    int barrierParticipantsCount = integerNonNegative(getAttr(node, "barrier-participants-count"));
    if ((barrierParticipantsCount) <= 0) Lang.EXPECT_POSITIVE(barrierParticipantsCount, "barrierParticipantsCount");
    String meetingId = getAttr(node, "meeting-id").trim();
    if (StringUtil.isNullOrEmpty(meetingId)) Lang.EXPECT_NOT_NULL_NOR_EMPTY(meetingId, "meetingId");
    jsync.Barrier meeting;
    synchronized (meetingId2meeting)
    {
      meeting = (jsync.Barrier) meetingId2meeting.get(meetingId);
      if (meeting == null)
      {
        meeting = new jsync.Barrier(barrierParticipantsCount);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("created meeting " + meetingId + ", waiting for " + (barrierParticipantsCount - 1) + " other threads to join");
        meetingId2meeting.put(meetingId, meeting);
      }
      else
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("joined meeting " + meetingId + ", waiting for other threads to join");
      }
    }
    meeting.reach();
    synchronized (meetingId2meeting)
    {
      if (meetingId2meeting.remove(meetingId) != null)
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("meeting " + meetingId + " finished, disposed.");
    }
  }
  catch (RuntimeException ee)
  {
    if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error in/or/while <thread-meeting>");
    throw ee;
  }
}
  //<try>
  //<catch/>
  //</try>
  private void try_(Node node, final Context ctx) throws ExpectException, MessagingNetworkException
  {
  NodeList statements = node.getChildNodes();
  if ((statements) == null) Lang.EXPECT_NOT_NULL(statements, "statements");
  int i = 0;
  boolean catch_finally_encountered = false;
  node = null;
  try
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("<try>");
    for (; i < statements.getLength(); i++)
    {
      node = statements.item(i);
      if (node.getNodeType() != node.ELEMENT_NODE)
        continue;
      if ("catch".equals(node.getNodeName()))
      {
        catch_finally_encountered = true;
        break;
      }
      statement(node, ctx);
    }
    if (!catch_finally_encountered)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("<try> without <catch/>");
    }
    return;
  }
  catch (Exception ex)
  {
    for (; i < statements.getLength(); i++)
    {
      node = statements.item(i);
      if (node.getNodeType() != node.ELEMENT_NODE)
        continue;
      if ("catch".equals(node.getNodeName()))
      {
        catch_finally_encountered = true;
        i++;
        break;
      }
    }
    if (!catch_finally_encountered)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("<try> without <catch/>, rethrowing exception", ex);
      throw new RuntimeException(ex.getMessage());
    }
    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("<catch/> catched exception", ex);
    for (; i < statements.getLength(); i++)
    {
      node = statements.item(i);
      if (node.getNodeType() != node.ELEMENT_NODE)
        continue;
      statement(node, ctx);
    }
  }
  finally
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("</try>");
  }
  }

  /*
  <switch var="random(1, 10)">
    <case var="1"/>
    <case var="2"/>
    <case var="3"/>
    <break/>
    <case var="10"/>
    <break/>
  </switch>
  */
  private void switch_(Node node, final Context ctx) throws ExpectException, MessagingNetworkException
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("<switch");
      int var = expression(getAttr(node, "var"));
      NodeList statements = node.getChildNodes();
      if ((statements) == null) Lang.EXPECT_NOT_NULL(statements, "statements");
      int i = 0;
      node = null;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("  var=\""+var+"\">");
      boolean ignore = true;
      for (; i < statements.getLength(); i++)
      {
        node = statements.item(i);
        if (node.getNodeType() != node.ELEMENT_NODE)
          continue;
        if ("case".equals(node.getNodeName()))
        {
          int cvar = expression(getAttr(node, "var"));
          if (var == cvar)
          {
            ignore = false;
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("<case var=\""+cvar+"\"/>");
          }
          continue; //parse next statement
        }
        if (ignore)
          continue; //ignore the current break or statement
        else
        {
          if ("break".equals(node.getNodeName()))
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("<break/>");
            return;
          }
          statement(node, ctx);
        }
      }
      if (ignore)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("no matching <case/> found");
      }
    }
    finally
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("</switch>");
    }
  }
}
