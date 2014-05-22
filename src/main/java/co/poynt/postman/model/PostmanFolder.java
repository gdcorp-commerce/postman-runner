package co.poynt.postman.model;

import java.util.List;

public class PostmanFolder {
	public String id;
	public String name;
	public String description;
	public List<String> order; //An ordered list of the request ids
}
