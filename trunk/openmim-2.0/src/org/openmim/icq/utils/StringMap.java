package org.openmim.icq.utils;

import java.lang.reflect.*;
import java.util.*;
/**
 * Insert the type's description here.
 * Creation date: (06.06.00 17:20:11)
 * @author: 
 */
public interface StringMap
{
String getValue(String key);
java.lang.reflect.Constructor getValueConstructor(Class targetClass)  throws NoSuchMethodException, SecurityException;
Object newValueInstance(java.lang.reflect.Constructor constructor, String value) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;
}
