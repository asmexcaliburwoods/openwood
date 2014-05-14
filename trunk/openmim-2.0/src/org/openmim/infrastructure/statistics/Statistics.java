package org.openmim.infrastructure.statistics;

import java.util.*;

import org.openmim.icq.utils.AutoConfig;

public class Statistics {
    private final static boolean STATISTICS_ENABLED = true;
    private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Statistics.class.getName());

    public static long REQPARAM_REPORT_PERIOD_SECONDS;
    public static boolean REQPARAM_REPORT_ALL_EVENTS;

    static {
        AutoConfig.fetchFromClassLocalResourceProperties(Statistics.class, true, false);
    }

    public static class SimpleCounter extends Statistics.Statistic {
        private int counter = 0;

        protected SimpleCounter(String nameSpace, String statisticName) throws RuntimeException {
            super(nameSpace, statisticName);
        }

        public final void inc() {
            increase();
        }

        public final void dec() {
            decrease();
        }

        public final void decrease() {
            if (STATISTICS_ENABLED && CAT.isInfoEnabled()) {
                synchronized (Statistics.SimpleCounter.this) {
                    int newvalue = --counter;
                    report(Integer.toString(newvalue));
                }
            }
        }

        public final void increase() {
            if (STATISTICS_ENABLED && CAT.isInfoEnabled()) {
                synchronized (Statistics.SimpleCounter.this) {
                    int newvalue = ++counter;
                    report(Integer.toString(newvalue));
                }
            }
        }
    }

    protected abstract static class Statistic {
        public final String statisticName;
        public final String nameSpace;
        private long lastReportTimeMillis = 0;

        protected Statistic(String nameSpace, String statisticName) throws RuntimeException {
            validateString(statisticName);
            validateString(nameSpace);
            this.statisticName = statisticName;
            this.nameSpace = nameSpace;
        }

        protected final void report(String s) {
            if (!REQPARAM_REPORT_ALL_EVENTS) {
                synchronized (CAT) {
                    long now = System.currentTimeMillis();
                    if (lastReportTimeMillis + 1000 * REQPARAM_REPORT_PERIOD_SECONDS >= now)
                        return;
                    lastReportTimeMillis = now;
                }
            }
            CAT.info(statisticName + "," + s + "," + nameSpace);
        }
    }

    protected final static void validateString(String statisticName) throws RuntimeException {
        if (statisticName == null
                || !(statisticName.trim().equals(statisticName))
                || statisticName.indexOf('\'') >= 0
                || statisticName.indexOf('\"') >= 0
                || statisticName.indexOf('`') >= 0
                || statisticName.indexOf(',') >= 0)
            throw new RuntimeException("invalid statistic string: " +
                    (statisticName == null ? null : "\"" + statisticName + "\""));
    }

    private final static HashMap statisticName2statistic = new HashMap();
    protected final static Object statisticMapLock = new Object();

    protected final static Statistic getInstance(String nameSpace, String statisticName) throws RuntimeException {
        validateString(nameSpace);
        validateString(statisticName);
        return (Statistic) statisticName2statistic.get(statisticName + "," + nameSpace);
    }

    protected final static void addInstance(Statistic s) {
        statisticName2statistic.put(s.statisticName + "," + s.nameSpace, s);
    }

    public static Statistics.SimpleCounter getSimpleCounterInstance(String nameSpace, String statisticName) throws RuntimeException {
        synchronized (statisticMapLock) {
            Statistic s = getInstance(nameSpace, statisticName);
            if (s == null) {
                s = new Statistics.SimpleCounter(nameSpace, statisticName);
                addInstance(s);
            }
            return (Statistics.SimpleCounter) s;
        }
    }
}