package co.poynt.postman.js;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.poynt.postman.PostmanHttpResponse;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import co.poynt.postman.model.PostmanEnvValue;
import co.poynt.postman.model.PostmanEnvironment;

public class PostmanJsVariables {
	// ============================================================
	// The members of this class are equivalent to the POSTMAN
	// global variables available inside a POSTMAN test script.
	// As defined here http://www.getpostman.com/docs/jetpacks_writing_tests
	public Object responseBody;
	public NativeArray responseHeaders;
	public Object responseTime;
	public NativeObject responseCode;
	public Object iteration;
	public Object postman;
	public NativeObject environment;
	public NativeObject tests;
	public NativeObject preRequestScript;
	// ============================================================

	private Context ctx;
	private Scriptable scope;
	private PostmanEnvironment env;

	public PostmanJsVariables(Context ctx, Scriptable scope, PostmanEnvironment env) {
		this.ctx = ctx;
		this.scope = scope;
		this.env = env;
	}

	public void prepare(PostmanHttpResponse httpResponse) {
		this.prepareJsVariables(httpResponse);
		this.injectJsVariablesToScope();
	}

	private void prepareJsVariables(PostmanHttpResponse httpResponse) {

		this.responseCode = new NativeObject();
		if (httpResponse != null) {
			List<Object> headerList = new ArrayList<Object>(httpResponse.headers.size());
			for (Map.Entry<String, String> h : httpResponse.headers.entrySet()) {
				NativeObject hobj = new NativeObject();
				hobj.put("key", hobj, h.getKey());
				hobj.put("value", hobj, h.getValue());
				headerList.add(hobj);
			}
			this.responseHeaders = new NativeArray(headerList.toArray());
			this.responseBody = Context.javaToJS(httpResponse.body, scope);

			this.responseCode.put("code", responseCode, httpResponse.code);
		} else {
			this.responseHeaders = new NativeArray(new Object[] {});
			this.responseBody = Context.javaToJS("", scope);

			this.responseCode.put("code", responseCode, 0);
		}

		// TODO: fix me
		this.responseTime = Context.javaToJS(0.0, scope);

		// TODO: fix me
		this.iteration = Context.javaToJS(0, scope);

		// The postman js var is only used to setEnvironmentVariables()
		this.postman = Context.javaToJS(this.env, scope);

		this.environment = new NativeObject();
		Set<Map.Entry<String, PostmanEnvValue>> map = this.env.lookup.entrySet();
		for (Map.Entry<String, PostmanEnvValue> e : map) {
			this.environment.put(e.getKey(), environment, e.getValue().value);
		}

		this.tests = new NativeObject();
		this.preRequestScript = new NativeObject();
	}

	private void injectJsVariablesToScope() {
		ScriptableObject.putProperty(scope, "responseBody", responseBody);
		ScriptableObject.putProperty(scope, "responseHeaders", responseHeaders);
		ScriptableObject.putProperty(scope, "responseTime", responseTime);
		ScriptableObject.putProperty(scope, "responseCode", responseCode);
		ScriptableObject.putProperty(scope, "iteration", iteration);
		ScriptableObject.putProperty(scope, "postman", postman);
		ScriptableObject.putProperty(scope, "environment", environment);
		ScriptableObject.putProperty(scope, "tests", tests);
		ScriptableObject.putProperty(scope, "preRequestScript", preRequestScript);
	}

	public void extractEnvironmentVariables() {

	}
}
