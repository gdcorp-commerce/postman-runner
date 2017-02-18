package co.poynt.postman;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
	public static final String REQUEST_ID_HEADER = "Poynt-Request-Id";

	private static final Logger logger = LoggerFactory.getLogger(PostmanRequestRunner.class);
	private PostmanVariables var;
	private boolean haltOnError = false;

	private static HttpComponentsClientHttpRequestFactory httpClientRequestFactory;

	static {
		httpClientRequestFactory = new HttpComponentsClientHttpRequestFactory();

		RequestConfig config = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(5000)
				.setConnectionRequestTimeout(60000).setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();

		httpClientRequestFactory.setHttpClient(HttpClientBuilder.create().setDefaultRequestConfig(config).build());
	}

	public PostmanRequestRunner(PostmanVariables var, boolean haltOnError) {
		this.var = var;
		this.haltOnError = haltOnError;
	}

	private RestTemplate setupRestTemplate(PostmanRequest request) {
		RestTemplate restTemplate = new RestTemplate(httpClientRequestFactory);
		if (request.dataMode.equals("urlencoded")) {
			List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
			converters.add(new FormHttpMessageConverter());
			StringHttpMessageConverter stringConv = new StringHttpMessageConverter();
			stringConv.setWriteAcceptCharset(false);
			converters.add(stringConv);
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
		String requestId = headers.getFirst(REQUEST_ID_HEADER);
		if (requestId == null) {
			requestId = UUID.randomUUID().toString();
			headers.set(REQUEST_ID_HEADER, requestId);
		}
		System.out.println("===============> requestId:" + requestId);

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
		httpResponse = restTemplate.exchange(uri, HttpMethod.valueOf(request.method), entity, String.class);
		System.out.println(" [" + (System.currentTimeMillis() - startMillis) + "ms]");

		// NOTE: there are certain negative test cases that expect 5xx series
		// response code.
		return this.evaluateTests(request, httpResponse, runResult);
	}

	/**
	 * @param request
	 * @param httpResponse
	 * @return true if all tests pass, false otherwise
	 */
	public boolean evaluateTests(PostmanRequest request, ResponseEntity<String> httpResponse,
			PostmanRunResult runResult) {
		if (request.tests == null || request.tests.isEmpty()) {
			return true;
		}
		Context cx = Context.enter();
		String testName = "---------------------> POSTMAN test";
		boolean isSuccessful = false;
		try {
			Scriptable scope = cx.initStandardObjects();
			PostmanJsVariables jsVar = new PostmanJsVariables(cx, scope, this.var.getEnv());
			jsVar.prepare(httpResponse);

			// Evaluate the test script
			cx.evaluateString(scope, request.tests, testName, 1, null);
			// The results are in the jsVar.tests variable

			// Extract any generated environment variables during the js run.
			jsVar.extractEnvironmentVariables();
			isSuccessful = true;
			boolean hasFailure = false;
			for (Map.Entry e : jsVar.tests.entrySet()) {
				runResult.totalTest++;

				String strVal = e.getValue().toString();
				if ("false".equalsIgnoreCase(strVal)) {
					hasFailure = true;
					runResult.failedTest++;
					runResult.failedTestName.add(request.name + "." + e.getKey().toString());
					isSuccessful = false;
				}

				System.out.println(testName + ": " + e.getKey() + " - " + e.getValue());
			}
			if (hasFailure) {
				System.out.println("=====THERE ARE TEST FAILURES=====");
				System.out.println("========TEST========");
				System.out.println(request.tests);
				System.out.println("========TEST========");
				System.out.println("========RESPONSE========");
				System.out.println(httpResponse.getStatusCode());
				System.out.println(httpResponse.getBody());
				System.out.println("========RESPONSE========");
				System.out.println("=====THERE ARE TEST FAILURES=====");
			}
		} catch (Throwable t) {
			isSuccessful = false;
			System.out.println("=====FAILED TO EVALUATE TEST AGAINST SERVER RESPONSE======");
			System.out.println("========TEST========");
			System.out.println(request.tests);
			System.out.println("========TEST========");
			System.out.println("========RESPONSE========");
			System.out.println(httpResponse.getStatusCode());
			System.out.println(httpResponse.getBody());
			System.out.println("========RESPONSE========");
			System.out.println("=====FAILED TO EVALUATE TEST AGAINST SERVER RESPONSE======");
		} finally {
			Context.exit();
		}
		return isSuccessful;
	}

	public boolean runPrerequestScript(PostmanRequest request, PostmanRunResult runResult) {
		if (request.preRequestScript == null || request.preRequestScript.isEmpty()) {
			return true;
		}
		Context cx = Context.enter();
		String testName = "---------------------> POSTMAN test: ";
		boolean isSuccessful = false;
		try {
			Scriptable scope = cx.initStandardObjects();
			PostmanJsVariables jsVar = new PostmanJsVariables(cx, scope, this.var.getEnv());
			// jsVar.prepare(httpResponse);
			jsVar.prepare(null);

			// Evaluate the test script
			cx.evaluateString(scope, request.preRequestScript, testName, 1, null);
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
