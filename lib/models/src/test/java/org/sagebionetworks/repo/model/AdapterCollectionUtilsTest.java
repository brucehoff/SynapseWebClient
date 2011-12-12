package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AdapterCollectionUtilsTest {
	
	@Test
	public void testStringArrayRoundTrip() throws JSONObjectAdapterException{
		String[] array = new String[] {"a", "b", "c"};
		JSONArrayAdapterImpl arrayAdapter = new JSONArrayAdapterImpl();
		AdapterCollectionUtils.writeToArray(arrayAdapter, array);
		List<String> back = AdapterCollectionUtils.readListOfStrings(arrayAdapter);
		assertNotNull(back);
		String[] backArray = back.toArray(new String[array.length]);
		assertTrue(Arrays.equals(array, backArray));
	}
	
	@Test
	public void testMapWithStringArray() throws JSONObjectAdapterException{
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("key array", new String[]{"one", "two", "three"});
		// Adapter
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl();
		AdapterCollectionUtils.writeToObject(adapter, row);
		Map<String, Object> back = AdapterCollectionUtils.readMapFromObject(adapter);
		assertNotNull(back);
		List<String> list = (List<String>) back.get("key array");
		assertEquals(3, list.size());
		assertEquals("one", list.get(0));
		
	}
	
	@Test
	public void testMapWithStringListy() throws JSONObjectAdapterException{
		Map<String, Object> row = new HashMap<String, Object>();
		List<String> list = new ArrayList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		row.put("key List", list);
		
		// Adapter
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl();
		AdapterCollectionUtils.writeToObject(adapter, row);
		
		Map<String, Object> back = AdapterCollectionUtils.readMapFromObject(adapter);
		assertNotNull(back);
		assertEquals(list, back.get("key List"));
		
	}

}