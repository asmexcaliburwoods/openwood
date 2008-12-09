package org.openmim.icq.util.java2;

/*
 * @(#)NoSuchElementException.java      1.18 00/02/02
 *
 * Copyright 1994-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

/**
 * Thrown by the <code>nextElement</code> method of an
 * <code>Enumeration</code> to indicate that there are no more
 * elements in the enumeration.
 *
 * @author  unascribed
 * @version 1.18, 02/02/00
 * @see     org.openmim.icq.util.java2.Enumeration
 * @see     org.openmim.icq.util.java2.Enumeration#nextElement()
 * @since   JDK1.0
 */
public
class NoSuchElementException extends RuntimeException {

  /**
		 * Constructs a <code>NoSuchElementException</code> with <tt>null</tt>
		 * as its error message string.
		 */
		public NoSuchElementException() {
		super();
		}
  /**
		 * Constructs a <code>NoSuchElementException</code>, saving a reference
		 * to the error message string <tt>s</tt> for later retrieval by the
		 * <tt>getMessage</tt> method.
		 *
		 * @param   s   the detail message.
		 */
		public NoSuchElementException(String s) {
		super(s);
		}
}
