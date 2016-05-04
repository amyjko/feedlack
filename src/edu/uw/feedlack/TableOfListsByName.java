package edu.uw.feedlack;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class TableOfListsByName<KeyType, EntryType> {
	
	private final Map<KeyType, Collection<EntryType>> table = new Hashtable<KeyType, Collection<EntryType>>();
	
	public Collection<EntryType> get(KeyType name) { return table.get(name); }
	public void put(KeyType name, EntryType object) {
		
		Collection<EntryType> matches;
		if(table.containsKey(name)) {
			matches = table.get(name);
		}
		else {
			matches = new Vector<EntryType>(3);			
			table.put(name, matches);				
		}
		matches.add(object);
		
	}
	
	public Collection<EntryType> getAll() {
		
		Collection<EntryType> all = new Vector<EntryType>();
		for(Collection<EntryType> these : table.values())
			all.addAll(these);
		return all;
		
	}
	
}