package co.poynt.postman.V1;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostmanHttpResponseV1 {
	private static final Logger logger = LoggerFactory.getLogger(PostmanHttpResponseV1.class);
	public int code;
	public String body;
	public Map<String, String> headers = new HashMap<>();

	public PostmanHttpResponseV1(int code, String body) {
		this.code = code;
		this.body = body;
	}

	public PostmanHttpResponseV1(HttpResponse response) {
		this.code = response.getStatusLine().getStatusCode();

		if (code > 399) {
			logger.warn("HTTP Response code: " + code);
		}
		if (code > 499) {
			logger.error("Failed to make POSTMAN request call.");
		}

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			try (InputStream resIs = entity.getContent()) {
				byte[] rawResponse = IOUtils.toByteArray(resIs);
				this.body = new String(rawResponse);
			} catch (IOException e) {
				throw new HaltTestFolderExceptionV1();
			}
		}

		for (Header h : response.getAllHeaders()) {
			this.headers.put(h.getName(), h.getName());
		}
	}
}
