package co.poynt.postman.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostmanCollection {
	public PostmanInfo info;
	public List<PostmanFolder> item;

	public Map<String, PostmanFolder> folderLookup = new HashMap<>();

	public void init() {
		for (PostmanFolder f : item) {
			folderLookup.put(f.name, f);
		}
	}
}
