package co.poynt.postman.modelV1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostmanCollectionV1 {
	public String id;
	public String name;
	public String description;
	public List<PostmanFolderV1> folders;  //ordered
	public Long timestamp;
	public Boolean synced;
	public List<PostmanRequestV1> requests; //ordered
	
	public Map<String, PostmanRequestV1> requestLookup = new HashMap<String,PostmanRequestV1>();
	public Map<String, PostmanFolderV1> folderLookup = new HashMap<String,PostmanFolderV1>();
	
	public void init() {
		for (PostmanRequestV1 r : requests) {
			requestLookup.put(r.id, r);
		}
		for (PostmanFolderV1 f : folders) {
			folderLookup.put(f.name, f);
		}
	}
}
