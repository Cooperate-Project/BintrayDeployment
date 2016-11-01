package de.cooperateproject.maven.bintray.deploy.api;

public class UploadPathUtils {

	public static String create(String path, String pkg, String version) {
		String pathName = path;
		if (pathName.startsWith("/")) {
			pathName = "/" + pkg + "/" + version + pathName;
		} else {
			pathName = pkg + "/" + version + "/" + pathName;
		}
		return pathName;
	}
	
}
