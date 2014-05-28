package co.poynt.postman;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class PostmanErrorHandler extends DefaultResponseErrorHandler {
	private static final Logger logger = LoggerFactory.getLogger(PostmanErrorHandler.class);
	
	boolean haltOnError = false;
	
	public PostmanErrorHandler(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}
	
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (response != null) {
			logger.warn("HTTP Response code: " + response.getStatusCode().value());
			if (response.getStatusCode().series() == Series.SERVER_ERROR) {
				logger.error("Failed to make POSTMAN request call.");
				InputStream responseBody = response.getBody();
				if (responseBody != null) {
					StringWriter writer = new StringWriter();
					IOUtils.copy(responseBody, writer, "UTF-8");
					String body = writer.toString();
					logger.error("HTTP Response Body: " + body);
				}
			}
		}

		if (haltOnError) {
			throw new HaltTestFolderException();
		}
	}
}
