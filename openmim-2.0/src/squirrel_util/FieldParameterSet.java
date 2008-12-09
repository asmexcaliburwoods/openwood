package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   FieldParameterSet.java

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

// Referenced classes of package squirrel_util.util:
//      PropertyException, StringMap

public abstract class FieldParameterSet
{

public FieldParameterSet()
{
}
private static void assignValue(Object obj, Field field, StringMap stringmap, String s, boolean flag) throws PropertyException
{
  if (s == null)
  {
	if (flag)
	  throw new PropertyException(Util.getResourceString("Fatal error: Property with key") + " \"" + field.getName() + "\" " + Util.getResourceString("not specified."));
	else
	  return;
  }
  else
  {
	convertAndAssign(obj, field, stringmap, s);
	return;
  }
}
public static void assignValues(Object obj, StringMap stringmap) throws PropertyException
{
  inspect(obj.getClass(), obj, stringmap);
}
private static void convertAndAssign(Object obj, Field field, StringMap stringmap, String s) throws PropertyException
{
  try
  {
	Class class1 = field.getType();
	java.lang.reflect.Constructor constructor;
	try
	{
	  constructor = stringmap.getValueConstructor(class1);
	}
	catch (Exception exception1)
	{
	  throw new PropertyException(Util.getResourceString("Error getting conversion constructor for field") + " " + field.getName() + " " + Util.getResourceString("of class") + " " + class1.getName() + ": " + exception1);
	}
	Object obj1 = stringmap.newValueInstance(constructor, s);
	field.set(obj, obj1);
  }
  catch (InvocationTargetException invocationtargetexception)
  {
	Throwable throwable = invocationtargetexception.getTargetException();
	Logger.printException(throwable);
	throw new PropertyException(Util.getResourceString("Error while property assignment:") + " " + throwable);
  }
  catch (Exception exception)
  {
	throw new PropertyException(Util.getResourceString("Error while property assignment:") + " " + exception);
  }
}
private static void inspect(Class class1, Object obj, StringMap stringmap) throws PropertyException
{
  if (class1 == null)
	return;
  Field afield[] = class1.getFields();
  for (int i = 0; i < afield.length; i++)
  {
	Field field = afield[i];
	int j = field.getModifiers();
	if (!Modifier.isFinal(j) && !Modifier.isPrivate(j))
	{
	  String s = field.getName();
	  boolean flag = s.startsWith("REQPARAM_");
	  if (flag || s.startsWith("OPTPARAM_"))
		assignValue(obj, field, stringmap, stringmap.getValue(s), flag);
	}
  }
  inspect(class1.getSuperclass(), obj, stringmap);
}
}
