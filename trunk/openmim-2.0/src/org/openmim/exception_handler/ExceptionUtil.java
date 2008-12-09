package org.openmim.exception_handler;

import java.io.IOException;

public class ExceptionUtil {
    public static void handleException(Throwable tr) {
        tr.printStackTrace();                
    }

    public static void handleException(String s, Throwable throwable) {
        System.err.println(s);
        throwable.printStackTrace();
    }
}
