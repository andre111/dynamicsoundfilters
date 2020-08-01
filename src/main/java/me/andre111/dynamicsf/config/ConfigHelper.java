package me.andre111.dynamicsf.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import me.andre111.dynamicsf.DynamicSoundFilters;

public class ConfigHelper {
	public static <K, V> Map<K, V> parseToMap(List<String> entries, Function<String, K> keyParser, Function<String, V> valueParser) {
		Map<K, V> map = new HashMap<>();
		
		if(entries != null) {
			for(String entry : entries) {
				try {
					if(entry == null) throw new RuntimeException();
					String[] split = entry.split(";");
					if(split.length != 2) throw new RuntimeException();
					
					K key = keyParser.apply(split[0]);
					V value = valueParser.apply(split[1]);
					
					map.put(key, value);
				} catch(RuntimeException e) {
					DynamicSoundFilters.getLogger().error("Ignoring broken config entry: "+entry, e);
				}
			}
		}
		
		return map;
	}
}
