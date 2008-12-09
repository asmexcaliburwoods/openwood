package org.openmim.icq.util.joe;

import java.io.*;

public class Log
{
  private final static java.text.DateFormat DATE_FORMAT = new java.text.SimpleDateFormat("dd MMM hh:mm:ss", java.util.Locale.US);
  private static final boolean DEBUG = true;
  private final PrintStream pw;

  public Log(String fileName)
  {
    if (DEBUG)
    {
      PrintStream pw_;
      try
      {
        final boolean fileAppend = true;
        final boolean autoflush = false;
        pw_ = new PrintStream(new FileOutputStream(fileName, fileAppend), autoflush);
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        pw_ = System.err;
      }
      pw = pw_;
    }
    else
    {
      pw = null;
    }
  }  
  
  private static final String formatCurrentDate()
  {
    return formatDate(new java.util.Date());
  }  
  
  private static final String formatDate(java.util.Date date)
  {
    return DATE_FORMAT.format(date);
  }  
  
  public final void log(String s)
  {
    if (DEBUG)
    {
      log(s, null);
    }
  }  
  
  public final void log(Throwable tr)
  {
    if (DEBUG)
    {
      log("", tr);
    }
  }  
  
  public final void log(String s, Throwable tr)
  {
    if (DEBUG)
    {
      synchronized (pw)
      {
        try
        {
          pw.println(formatCurrentDate() + " "+Thread.currentThread().getName()+" " + s);
          if (tr != null) tr.printStackTrace(pw);
        }
        catch (Exception ex)
        {
          try
          {
            System.err.println(formatCurrentDate() + " "+Thread.currentThread().getName()+" " + s);
            if (tr != null) tr.printStackTrace();
          }
          catch (Throwable tr1)
          {
          }
        }
        catch (Throwable tr2)
        {
        }
      }
    }
  }  

  public final void close()
  {
    if (DEBUG)
    {
      flush();
      try
      {
        pw.close();
      }
      catch (Throwable tr)
      {
      }
    }
  }

  public final void flush()
  {
    if (DEBUG)
    {
      try
      {
        pw.flush();
      }
      catch (Throwable tr)
      {
      }
    }
  }
}
