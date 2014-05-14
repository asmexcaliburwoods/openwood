package org.openmim.messaging_network2.model;

import java.util.List;

public interface ContactListGroup extends ContactListItem {
//    final String PROPERTY_ITEMS = "items";

    List<ContactListItem> getItems();

    void addItem(ContactListItem item);

    void removeItem(int i);

    void addContactListGroupListener(ContactListGroupListener contactListGroupListener);

    void removeContactListGroupListener(ContactListGroupListener contactListGroupListener);

//    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);
//
//    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);
//
    public static interface ContactListGroupListener {
        void itemAdded(ContactListGroup source, ContactListItem item);

        void itemRemoved(ContactListGroup source, int i, ContactListItem item);
    }
}
