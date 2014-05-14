package org.openmim.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openmim.icq.utils.AutoConfig;
import org.openmim.icq.utils.Lang;
import org.openmim.icq.utils.StringUtil;
import org.openmim.stuff.Defines;

public class EchoServer
{
	private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(EchoServer.class.getName());


	public static int REQPARAM_PORT_TO_LISTEN;

	static volatile int socketsCount = 0;

	static final Object statLock = new Object();
	static long startTime = 0;
	static long bytesSent = 0;

	public static void main(int portToListen)
	{
		try
		{
			if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("echoserver started");
			REQPARAM_PORT_TO_LISTEN = portToListen;
			new EchoServer().runEchoServer();
		}
		catch (Throwable tr)
		{
			if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("hl: unhandled exception", tr);
			System.exit(1);
		}
	}

	public static void main(String[] args)
	{
		try
		{
			if (args.length == 0)
			{
				AutoConfig.fetchFromClassLocalResourceProperties(EchoServer.class, true, false);
				main(REQPARAM_PORT_TO_LISTEN);
			}
			else
			{
				int p = -1;
				try
				{
					p = Integer.parseInt(args[0]);
				}
				catch (NumberFormatException exn)
				{
					if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("invalid port in hl.args[0]: "+StringUtil.quote(args[0]));
					System.exit(1);
				}
				main(p);
			}
		}
		catch (Throwable tr)
		{
			if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("hl: unhandled exception", tr);
			System.exit(1);
		}
	}

	private List echoConnections;

	private void runEchoServer()
	{
		echoConnections = new LinkedList();

		new Thread("echo accept") {
			public void run() {
				try {
					ServerSocket echo; 
					try	{
						if ((REQPARAM_PORT_TO_LISTEN) <= 0) Lang.ASSERT_POSITIVE(REQPARAM_PORT_TO_LISTEN, "REQPARAM_PORT_TO_LISTEN");

						if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("echo accept thread started.  Listening on port "+REQPARAM_PORT_TO_LISTEN);
						echo = new ServerSocket(REQPARAM_PORT_TO_LISTEN);
						++socketsCount;
						printSocketCount();
						for (;;)
						{
							Socket s = null;
							try
							{
								s = echo.accept();
								++socketsCount;
								printSocketCount();
								synchronized (echoConnections)
								{
									echoConnections.add(s);
								}
							}
							catch (IOException tr1)
							{
								if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ex ignored", tr1);
								if (s != null) try { s.close(); } catch (IOException ex2) {}
							}
						}
					}
					catch (Throwable tr)
					{
						if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("echo accept", tr);
					}
					if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("echo accept thread finished");
					System.exit(1);
				}catch(Throwable tr){
					tr.printStackTrace();
					System.err.println("thread "+Thread.currentThread().getName()+" exiting");
				}
			}
		}.start();

		new Thread("echo data copy")
		{
			public void run()
			{
				try
				{
					if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("echo data copy thread started");

					byte buf[] = new byte[1024];

					for (;;)
					{
						List ec;
						synchronized (echoConnections)
						{
							ec = new ArrayList(echoConnections.size());
							ec.addAll(echoConnections);
						}
						for (int i = 0; i < ec.size(); ++i)
						{
							Socket s = (Socket) ec.get(i);
							try
							{
								for(int j = 0; j < 5; ++j)
								{
									int av = s.getInputStream().available();
									if (av <= 0) break;
									int len = s.getInputStream().read(buf, 0, Math.min(av, buf.length));
									s.getOutputStream().write(buf, 0, len);
									if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("bytes echoed: "+len);
									synchronized (statLock)
									{
										long now = System.currentTimeMillis();
										if (startTime == 0) startTime = now;
										bytesSent+= len;
										long delta = now - startTime;
										if (delta >= 30)
										{
											double rate = (bytesSent) / (delta * (((double) 1.0)/500 * 1024*1024));
											if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("total avg traffic, MB per second: "+rate);
										}
									}

								}
							}
							catch (IOException exx)
							{
								if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error(""+s, exx);
								try
								{
									s.close();
								}
								catch (IOException exs)
								{
									if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(exs);
								}
								--socketsCount;
								echoConnections.remove(s);
								printSocketCount();
							}
						}
						ec = null;
						Thread.currentThread().yield();
					}
				}
				catch (Throwable tr)
				{
					if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("echo data copy thread", tr);
				}
				if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("echo data copy thread finished; doing System.exit(1)");
				System.exit(1);
			}
		}.start();
	}

	static void printSocketCount()
	{
		if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("count of sockets: "+socketsCount);
	}
}