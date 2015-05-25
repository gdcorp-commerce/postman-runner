package co.poynt.postman;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import co.poynt.postman.js.PostmanJsVariables;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;

public class PostmanRequestRunner {
	private static final Logger logger = LoggerFactory
			.getLogger(PostmanRequestRunner.class);
	private PostmanVariables var;
	private boolean haltOnError = false;
	
	private static HttpComponentsClientHttpRequestFactory httpClientRequestFactory = new HttpComponentsClientHttpRequestFactory();
	
	public PostmanRequestRunner(PostmanVariables var, boolean haltOnError) {
		this.var = var;
		this.haltOnError = haltOnError;
	}

	private RestTemplate setupRestTemplate(PostmanRequest request) {
		RestTemplate restTemplate = new RestTemplate(httpClientRequestFactory);
		if (request.dataMode.equals("urlencoded")) {
			List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
			converters.add(new FormHttpMessageConverter());
			converters.add(new StringHttpMessageConverter());
			restTemplate.setMessageConverters(converters);
		}
		return restTemplate;
	}
	
	public boolean run(PostmanRequest request, PostmanRunResult runResult) {

		runPrerequestScript(request, runResult);

		HttpHeaders headers = request.getHeaders(var);
		if (request.dataMode.equals("urlencoded")) {
			headers.set("Content-Type", "application/x-www-form-urlencoded");
		}
		HttpEntity<String> entity = new HttpEntity<String>(request.getData(var), headers);
		ResponseEntity<String> httpResponse = null;
		PostmanErrorHandler errorHandler = new PostmanErrorHandler(haltOnError);
		RestTemplate restTemplate = setupRestTemplate(request);
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
		
		long startMillis = System.currentTimeMillis();
		httpResponse = restTemplate.exchange(uri,
				HttpMethod.valueOf(request.method), entity, String.class);
		System.out.println(" [" + (System.currentTimeMillis() - startMillis)
				+ "ms]");
		
		if (httpResponse.getStatusCode().series() != Series.SERVER_ERROR) {
			return this.evaluateTests(request, httpResponse, runResult);
		} else {
			return false;
		}
	}

	/**
	 * @param request
	 * @param httpResponse
	 * @return true if all tests pass, false otherwise
	 */
	public boolean evaluateTests(PostmanRequest request, ResponseEntity<String> httpResponse, PostmanRunResult runResult) {
		if ( request.tests == null || request.tests.isEmpty()) {
			return true;
		}
		Context cx = Context.enter();
		String testName = "---------------------> POSTMAN test";
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
				runResult.totalTest++;
				
				String strVal = e.getValue().toString();
				if ("false".equalsIgnoreCase(strVal)) {
					runResult.failedTest++;
					runResult.failedTestName.add(request.name + "." + e.getKey().toString());
					isSuccessful = false;
				}
				
				System.out.println(testName + ": " + e.getKey() + " - "
						+ e.getValue());
			}			
		} finally {
			Context.exit();
		}
		return isSuccessful;
	}

	public boolean runPrerequestScript(PostmanRequest request,
			PostmanRunResult runResult) {
		if (request.preRequestScript == null
				|| request.preRequestScript.isEmpty()) {
			return true;
		}
		Context cx = Context.enter();
		String testName = "---------------------> POSTMAN test: ";
		boolean isSuccessful = false;
		try {
			Scriptable scope = cx.initStandardObjects();
			PostmanJsVariables jsVar = new PostmanJsVariables(cx, scope,
					this.var.getEnv());
			// jsVar.prepare(httpResponse);
			jsVar.prepare(null);

			// Evaluate the test script
			cx.evaluateString(scope, request.preRequestScript, testName, 1,
					null);
			// The results are in the jsVar.tests ???? variable

			// Extract any generated environment variables during the js run.
			jsVar.extractEnvironmentVariables();
			isSuccessful = true;
		} finally {
			Context.exit();
		}
		return isSuccessful;
	}
}
