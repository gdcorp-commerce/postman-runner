package co.poynt.postman.model;

import co.poynt.postman.PostmanRequestRunner;

import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostmanRequest {
	public String method;
	public List<PostmanHeader> header;
	public PostmanBody body;
	public PostmanUrl url;
	public PostmanAuth auth;

	public String getData(PostmanVariables var) {
		if (body == null || body.mode == null)  {
			return "";
		} else {
			switch (body.mode) {
				case "raw":
					return var.replace(body.raw);
				case "urlencoded":
					return urlFormEncodeData(var, body.urlencoded);
				default:
					return "";
			}
		}
	}

	public String urlFormEncodeData(PostmanVariables var, List<PostmanUrlEncoded> formData) {
		String result = "";
		int i = 0;
		for (PostmanUrlEncoded encoded : formData) {
			result += encoded.key + "=" + URLEncoder.encode(var.replace(encoded.value));
			if (i < formData.size() - 1) {
				result += "&";
			}
		}
		return result;
	}

	public String getUrl(PostmanVariables var) {
		return var.replace(url.raw);
	}

	public Map<String, String> getHeaders(PostmanVariables var) {
		Map<String, String> result = new HashMap<>();
		if (header == null || header.isEmpty()) {
			return result;
		}
		for (PostmanHeader head : header) {
			if (head.key.toUpperCase().equals(PostmanRequestRunner.REQUEST_ID_HEADER)) {
				result.put(head.key.toUpperCase(), var.replace(head.value));
			} else {
				result.put(head.key, var.replace(head.value));
			}
		}
		return result;
	}
	
	public String getAuth() {
		if(this.auth!=null && this.auth.type!=null) {
			if(this.auth.type.equalsIgnoreCase("basic")) {
				final String[] credentials = this.credentials();
				if(credentials!=null && credentials[0]!=null && credentials[1]!=null) {
					final StringBuilder sb = new StringBuilder(credentials[0]);
					sb.append(":").append(credentials[1]);
					return "Basic " + Base64.getEncoder().encodeToString(sb.toString().getBytes());
				}
				return null;
			}
			throw new IllegalArgumentException("Auth type '"+ this.auth.type + "' not supported.");
		} else {
			return null;
		}
	}

	private String[] credentials() {
		String username=null;
		String password=null;
		if(this.auth.basic==null) {
			return null;
		}
		for (final PostmanHeader basic : this.auth.basic) {
			if(basic.key.equals("username")) {
				username=basic.value;
			}
			if(basic.key.equals("password")) {
				password=basic.value;
			}
		}
		return new String[] {username, password};
	}
}
