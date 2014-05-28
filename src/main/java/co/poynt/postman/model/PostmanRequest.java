package co.poynt.postman.model;

import org.springframework.http.HttpHeaders;

public class PostmanRequest {
	public String id;
	public String headers; //String of \n separated headers with : separate name:value.
	public String url;
	public String method;
	public Object data; //Either String of escaped-JSON or [] empty array (for GET)
	public String dataMode; //not used
	public String name;
	public String description;
	public String descriptionFormat;
	public Long time;
	public Integer version;
	public Object responses;
	public String tests;
	public Boolean synced;
	
	public String getData(PostmanVariables var) {
		if (this.data instanceof String) {
			String result = (String) this.data;
			return var.replace(result);
		} else { //empty array
			return "";
		}
	}
	
	public String getUrl(PostmanVariables var) {
		return var.replace(this.url);
	}
	
	public HttpHeaders getHeaders(PostmanVariables var) {
		HttpHeaders result = new HttpHeaders();
		if (this.headers == null || this.headers.isEmpty()) {
			return result;
		}
		String h = var.replace(headers);
		String[] splitHeads = h.split("\n");
		for (String hp : splitHeads) {
			String[] pair = hp.split(":");
			String key = pair[0].trim();
			String val = pair[1].trim();
			result.set(key, val);
		}
		return result;
	}
}
