package co.poynt.postman;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import co.poynt.postman.js.PostmanJsVariables;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;

public class PostmanRequestRunner {
	private static final Logger logger = LoggerFactory
			.getLogger(PostmanRequestRunner.class);
	private PostmanVariables var;

	public PostmanRequestRunner(PostmanVariables var) {
		this.var = var;
	}

	public ResponseEntity<String> run(PostmanRequest request) {
		HttpHeaders headers = request.getHeaders(var);
		HttpEntity<String> entity = new HttpEntity<String>(request.getData(var), headers);
		ResponseEntity<String> response = null;
		RestTemplate restTemplate = new RestTemplate();
		try {
			String url = request.getUrl(var);
			URI uri = new URI(url);
			
			response = restTemplate.exchange(uri,
					HttpMethod.valueOf(request.method), entity, String.class);
			
			this.evaluateTests(request, response);
			
		} catch (Exception e) {
			logger.error("Failed to make POSTMAN request call.");
			if (response != null) {
				logger.error("HTTP code: " + response.getStatusCode().value());
				logger.error(e.getMessage());
			}
		}
		return response;
	}

	public void evaluateTests(PostmanRequest request, ResponseEntity<String> httpResponse) {
		Context cx = Context.enter();
		String testName = request.name + "-TEST";
		try {
			Scriptable scope = cx.initStandardObjects();
			PostmanJsVariables jsVar = new PostmanJsVariables(cx, scope, this.var.getEnv());
			jsVar.prepare(httpResponse);
			
			//Evaluate the test script
			Object result = cx.evaluateString(scope, request.tests, testName, 1, null);

			//Extract any generated environment variables during the js run.
			jsVar.extractEnvironmentVariables();
			
			for (Map.Entry e : jsVar.tests.entrySet()) {
				System.out.println(testName + ": " + e.getKey() + " - " + e.getValue());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			Context.exit();
		}

	}
}
