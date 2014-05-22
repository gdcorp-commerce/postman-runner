package co.poynt.postman.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostmanCollection {
	public String id;
	public String name;
	public String description;
	public List<PostmanFolder> folders;  //ordered
	public Long timestamp;
	public Boolean synced;
	public List<PostmanRequest> requests; //ordered
	
	public Map<String, PostmanRequest> requestLookup = new HashMap<String,PostmanRequest>();
	
	public void init() {
		for (PostmanRequest r : requests) {
			requestLookup.put(r.id, r);
		}
	}
}
