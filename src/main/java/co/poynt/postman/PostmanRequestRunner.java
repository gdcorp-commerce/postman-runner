package co.poynt.postman;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import co.poynt.postman.js.PostmanJsVariables;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;

public class PostmanRequestRunner {
	private static final Logger logger = LoggerFactory
			.getLogger(PostmanRequestRunner.class);
	private PostmanVariables var;
	private boolean haltOnError = false;
	
	public PostmanRequestRunner(PostmanVariables var, boolean haltOnError) {
		this.var = var;
		this.haltOnError = haltOnError;
	}

	public boolean run(PostmanRequest request) {
		HttpHeaders headers = request.getHeaders(var);
		HttpEntity<String> entity = new HttpEntity<String>(request.getData(var), headers);
		ResponseEntity<String> httpResponse = null;
		PostmanErrorHandler errorHandler = new PostmanErrorHandler(haltOnError);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(errorHandler);
		String url = request.getUrl(var);
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			if (haltOnError)
				throw new HaltTestFolderException();
			else
				return false;
		}
		
		httpResponse = restTemplate.exchange(uri,
				HttpMethod.valueOf(request.method), entity, String.class);
		
		if (httpResponse.getStatusCode().series() != Series.SERVER_ERROR) {
			return this.evaluateTests(request, httpResponse);
		} else {
			return false;
		}
	}

	/**
	 * @param request
	 * @param httpResponse
	 * @return true if all tests pass, false otherwise
	 */
	public boolean evaluateTests(PostmanRequest request, ResponseEntity<String> httpResponse) {
		if ( request.tests == null || request.tests.isEmpty()) {
			return true;
		}
		Context cx = Context.enter();
		String testName = "---------------------> POSTMAN test: ";
		boolean isSuccessful = false;
		try {
			Scriptable scope = cx.initStandardObjects();
			PostmanJsVariables jsVar = new PostmanJsVariables(cx, scope, this.var.getEnv());
			jsVar.prepare(httpResponse);
			
			//Evaluate the test script
			cx.evaluateString(scope, request.tests, testName, 1, null);
			//The results are in the jsVar.tests variable
			
			//Extract any generated environment variables during the js run.
			jsVar.extractEnvironmentVariables();
			isSuccessful = true;
			for (Map.Entry e : jsVar.tests.entrySet()) {
				String strVal = e.getValue().toString();
				if ("false".equalsIgnoreCase(strVal)) {
					isSuccessful = false;
				}
				
				System.out.println(testName + ": " + e.getKey() + " - " + e.getValue());
			}			
		} finally {
			Context.exit();
		}
		return isSuccessful;
	}
}
