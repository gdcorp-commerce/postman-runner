package co.poynt.postman.test;

import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import co.poynt.postman.PostmanReader;
import co.poynt.postman.PostmanRequestRunner;
import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;

import org.mozilla.javascript.*;

public class TestPostman {

	@Test
	public void testLoadFile() throws Exception {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		String fileName = "PoyntRegression.postman_collection";
		PostmanReader reader = new PostmanReader();

		PostmanCollection c = reader.readCollectionFromFile(fileName);
		c.init();
		// System.out.println(om.writeValueAsString(c));

		String envFile = "PoyntRegression.postman_environment";
		PostmanEnvironment e = reader.readEnvironmentFromFile(envFile);
		e.init();
		// System.out.println(om.writeValueAsString(e));

		PostmanVariables var = new PostmanVariables(e);
		PostmanRequestRunner runner = new PostmanRequestRunner(var);
		
		//For each folder, execute each request in order
		for (PostmanFolder f : c.folders) {
			if (f.name.equals("Terminal use-cases")) {
				for (String rId : f.order){
					runner.run(c.requestLookup.get(rId));
				}
			}
		}
	}

	@Test
	public void testRhino() throws Exception {
		Context cx = Context.enter();
		try {
			Scriptable scope = cx.initStandardObjects();

			// Add a global variable "out" that is a JavaScript reflection
			// of System.out
			Object jsOut = Context.javaToJS(System.out, scope);
			ScriptableObject.putProperty(scope, "out", jsOut);
			
			//Testing native object
			NativeObject nobj = new NativeObject();
			nobj.defineProperty("foo", "bar", NativeObject.READONLY);
			ScriptableObject.putProperty(scope, "mynativeobj", nobj);
			
			String s = "function f(x){return x+1} out.println(\"Hellow from js\");mynativeobj[\"func\"]=f(7);\"alldone\"";
			Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
			
			Object x = scope.get("mynativeobj", scope);
			if (x == Scriptable.NOT_FOUND) {
			    System.out.println("mynativeobj is not defined.");
			} else {
			    for (Map.Entry e : nobj.entrySet()) {
			    	System.out.println(e.getKey() + ":" + e.getValue());
			    }
			}
			System.err.println(Context.toString(result));
		} finally {
			Context.exit();
		}
	}
}
