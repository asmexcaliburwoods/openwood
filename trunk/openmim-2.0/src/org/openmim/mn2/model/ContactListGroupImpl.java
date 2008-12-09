package org.openmim.mn2.model;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

public class ContactListGroupImpl implements ContactListGroup {
    private List<ContactListItem> items=new ArrayList<ContactListItem>();
    private String displayName;
    private List<ContactListGroupListener> listeners=new LinkedList<ContactListGroupListener>();

    public List<ContactListItem> getItems() {
        return items;
    }

    public void addItem(ContactListItem item) {
        items.add(item);
        for (ContactListGroupListener listener : listeners) {
            listener.itemAdded(this,item);
        }
    }

    public void removeItem(int i) {
        ContactListItem item=items.remove(i);
        for (ContactListGroupListener listener : listeners) {
            listener.itemRemoved(this,i,item);
        }
    }

    public void addContactListGroupListener(ContactListGroupListener contactListGroupListener) {
        listeners.add(contactListGroupListener);
    }

    public void removeContactListGroupListener(ContactListGroupListener contactListGroupListener) {
        listeners.remove(contactListGroupListener);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setItems(List<ContactListItem> items) {
        this.items = items;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
