package co.poynt.postman;

import picocli.CommandLine;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionProvider implements CommandLine.IVersionProvider {
	private static final String MANIFEST_BUILD_TIMESTAMP = "BuildTimestamp";
	private static final String MANIFEST_TOOLS_VERSION = "ToolsVersion";
	private static final String MANIFEST_ARTIFACT_ID = "ArtifactId";

	private String buildInfo;
	private String artifactId;

	@Override
	public String[] getVersion() {

		try {
			String className = getClass().getSimpleName() + ".class";
			String classPath = getClass().getResource(className).toString();
			if (!classPath.startsWith("jar")) {
				return new String[0];
			}

			URL url = new URL(classPath);
			JarURLConnection jarConnection = (JarURLConnection) url.openConnection();

			Manifest manifest = jarConnection.getManifest();
			Attributes attributes = manifest.getMainAttributes();

			buildInfo = attributes.getValue(MANIFEST_TOOLS_VERSION);
			String timestamp = attributes.getValue(MANIFEST_BUILD_TIMESTAMP);
			if (timestamp != null && !timestamp.isEmpty()) {
				buildInfo += "-" + timestamp;
			}
			artifactId = attributes.getValue(MANIFEST_ARTIFACT_ID);
			return new String[] {artifactId, buildInfo};
		} catch (IOException e) {
			System.err.println("Failed to load manifest version info.");
			throw new IllegalStateException("Failed to load manifest version info.");
		}

	}
}