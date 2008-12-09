package org.openmim.icq.util.java2;

/*
 * @(#)UnsupportedOperationException.java       1.4 97/09/23
 *
 * Copyright 1993-1997 Sun Microsystems, Inc. 901 San Antonio Road,
 * Palo Alto, California, 94303, U.S.A.  All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * CopyrightVersion 1.2
 *
 */

/**
 * Thrown by an Object to indicate that it does not support the
 * requested operation.
 *
 * @author  Josh Bloch
 * @version 1.4 09/23/97
 * @since   JDK1.2
 */
public class UnsupportedOperationException extends RuntimeException
{

  /**
   * Constructs an UnsupportedOperationException with no detail message.
   */
  public UnsupportedOperationException()
  {
  }  
  /**
   * Constructs an UnsupportedOperationException with the specified
   * detail message.
   */
  public UnsupportedOperationException(String message)
  {
		super(message);
  }  
}
