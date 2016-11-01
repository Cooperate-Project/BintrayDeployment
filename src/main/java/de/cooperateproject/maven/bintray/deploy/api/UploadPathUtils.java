package de.cooperateproject.maven.bintray.deploy.api;

public class UploadPathUtils {

	public static String create(String path, String version) {
		String pathName = path;
		if (pathName.startsWith("/")) {
			pathName = "/" + version + pathName;
		} else {
			pathName = version + "/" + pathName;
		}
		return pathName;
	}
	
}
