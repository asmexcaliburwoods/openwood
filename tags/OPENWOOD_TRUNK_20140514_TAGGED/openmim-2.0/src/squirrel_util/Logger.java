package squirrel_util;

import org.openmim.exception_handler.ExceptionUtil;

import java.io.IOException;

/**
 * Insert the type's description here.
 * Creation date: (01.12.00 14:21:47)
 * @author:
 */
public class Logger
{
	public final static java.text.DateFormat DATE_FORMAT = new java.text.SimpleDateFormat("dd MMM hh:mm:ss");
	public static boolean DEBUG = true;
private Logger()
{
}
public static String formatCurrentDate()
{
  return DATE_FORMAT.format(new java.util.Date());
}
public static String formatStackTrace(Throwable tr)
{
  java.io.StringWriter stackTrace = new java.io.StringWriter();
  java.io.PrintWriter pw = new java.io.PrintWriter(stackTrace);
  tr.printStackTrace(pw);
  pw.flush();
  pw.close();
  stackTrace.flush();
    try {
        stackTrace.close();
    } catch (IOException e) {
        ExceptionUtil.handleException(e);
    }
    return stackTrace.toString().replace('/', '.');
}
public static void log(String s)
{
	if (DEBUG)
		System.err.println(Logger.formatCurrentDate() + ": " + s);
}
public static void printException(Throwable tr)
{
  printException(tr, System.err);
}
public static void printException(Throwable tr, java.io.PrintStream p)
{
  synchronized (p)
  {
	p.print(formatCurrentDate() + ": Logger.printException(");
	if (tr == null)
	{
	  p.println("null)");
	  return;
	}
	p.println(tr.getClass().getName() + ")");
	if (tr instanceof java.util.MissingResourceException)
	{
	  java.util.MissingResourceException mre = (java.util.MissingResourceException) tr;
	  p.println("Missing resource\nResource key:\t" + StringUtil.toPrintableString(mre.getKey()) + "\nClass: " + mre.getClassName());
	}

	//
	p.print(Logger.formatStackTrace(tr));
	p.print(Logger.formatStackTrace(new Exception("Where is this called from?")));

	//
	//if (tr instanceof java.sql.SQLException)
	//{
	////java.sql.SQLException se = (java.sql.SQLException) tr;

	////p.println(" Nested exception : "Missing resource\nResource key:\t" + squirrel_util.StringUtil.toPrintableString(mre.getKey()) + "\nClass: " + mre.getClassName());

	//}
  }
}
}
