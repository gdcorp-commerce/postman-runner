package co.poynt.postman;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class CmdBase {
	static protected ObjectMapper om = new ObjectMapper();
	static com.mashape.unirest.http.ObjectMapper uniOm;

	static {
		om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		om.setDateFormat(new ISO8601DateFormat());
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		uniOm = new com.mashape.unirest.http.ObjectMapper() {
			@Override
			public <T> T readValue(String s, Class<T> aClass) {
				try {
					return om.readValue(s, aClass);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			public String writeValue(Object o) {
				try {
					return om.writeValueAsString(o);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
	}
}
