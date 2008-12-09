package org.openmim.icq.util.java2;

/*
 * @(#)AbstractList.java        1.31 00/02/02
 *
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

class SubList extends AbstractList {
  private AbstractList l;
  private int offset;
  private int size;
  private int expectedModCount;

  SubList(AbstractList list, int fromIndex, int toIndex) {
		if (fromIndex < 0)
		  throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > list.size())
		  throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
		  throw new IllegalArgumentException("fromIndex(" + fromIndex +
						   ") > toIndex(" + toIndex + ")");
		l = list;
		offset = fromIndex;
		size = toIndex - fromIndex;
		expectedModCount = l.modCount;
		}
  public void add(int index, Object element) {
		if (index<0 || index>size)
		  throw new IndexOutOfBoundsException();
		checkForComodification();
		l.add(index+offset, element);
		expectedModCount = l.modCount;
		size++;
		modCount++;
		}
  public boolean addAll(int index, Collection c) {
		if (index<0 || index>size)
		  throw new IndexOutOfBoundsException(
		  "Index: "+index+", Size: "+size);
		int cSize = c.size();
		if (cSize==0)
		  return false;

		checkForComodification();
		l.addAll(offset+index, c);
		expectedModCount = l.modCount;
		size += cSize;
		modCount++;
		return true;
		}
  public boolean addAll(Collection c) {
		return addAll(size, c);
		}
  private void checkForComodification() {
		if (l.modCount != expectedModCount)
		  throw new ConcurrentModificationException();
		}
  public Object get(int index) {
		rangeCheck(index);
		checkForComodification();
		return l.get(index+offset);
		}
  public Iterator iterator() {
		return listIterator();
		}
  public ListIterator listIterator(final int index) {
		checkForComodification();
		if (index<0 || index>size)
		  throw new IndexOutOfBoundsException(
		  "Index: "+index+", Size: "+size);

		return new ListIterator() {
		  private ListIterator i = l.listIterator(index+offset);

		  public boolean hasNext() {
		  return nextIndex() < size;
		  }

		  public Object next() {
		  if (hasNext())
				return i.next();
		  else
				throw new NoSuchElementException();
		  }

		  public boolean hasPrevious() {
		  return previousIndex() >= 0;
		  }

		  public Object previous() {
		  if (hasPrevious())
				return i.previous();
		  else
				throw new NoSuchElementException();
		  }

		  public int nextIndex() {
		  return i.nextIndex() - offset;
		  }

		  public int previousIndex() {
		  return i.previousIndex() - offset;
		  }

		  public void remove() {
		  i.remove();
		  expectedModCount = l.modCount;
		  size--;
		  modCount++;
		  }

		  public void set(Object o) {
		  i.set(o);
		  }

		  public void add(Object o) {
		  i.add(o);
		  expectedModCount = l.modCount;
		  size++;
		  modCount++;
		  }
		};
		}
  private void rangeCheck(int index) {
		if (index<0 || index>=size)
		  throw new IndexOutOfBoundsException("Index: "+index+
						  ",Size: "+size);
		}
  public Object remove(int index) {
		rangeCheck(index);
		checkForComodification();
		Object result = l.remove(index+offset);
		expectedModCount = l.modCount;
		size--;
		modCount++;
		return result;
		}
  protected void removeRange(int fromIndex, int toIndex) {
		checkForComodification();
		l.removeRange(fromIndex+offset, toIndex+offset);
		expectedModCount = l.modCount;
		size -= (toIndex-fromIndex);
		modCount++;
		}
  public Object set(int index, Object element) {
		rangeCheck(index);
		checkForComodification();
		return l.set(index+offset, element);
		}
  public int size() {
		checkForComodification();
		return size;
		}
  public List subList(int fromIndex, int toIndex) {
		return new SubList(this, fromIndex, toIndex);
		}
}
