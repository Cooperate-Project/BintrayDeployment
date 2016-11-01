package de.cooperateproject.maven.bintray.deploy.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

public class UploadMapCalculator extends DirectoryWalker<Pair<String, InputStream>> {

	private final File directory;
	private final String version;
	
	public UploadMapCalculator(File directory, String version) {
		this.directory = directory;
		this.version = version;
	}
	
	public Map<String, InputStream> getUploadMap() throws IOException {
		
		Collection<Pair<String, InputStream>> results = new ArrayList<Pair<String, InputStream>>();
		try {
			this.walk(directory, results);
		} catch (IOException e) {
			for (Pair<String, InputStream> p : results) {
				IOUtils.closeQuietly(p.getValue());
			}
			throw e;
		}
		
		Map<String, InputStream> uploadMap = new HashMap<String, InputStream>();
		for (Pair<String, InputStream> p : results) {
			uploadMap.put(p.getKey(), p.getValue());
		}
		return uploadMap;
	}
	
	public static void close(Map<String, InputStream> uploadMap) {
		for (InputStream s : uploadMap.values()) {
			IOUtils.closeQuietly(s);
		}
	}
	
	@Override
	protected void handleFile(File file, int depth, Collection<Pair<String, InputStream>> results) throws FileNotFoundException {
		String pathName = directory.toURI().relativize(file.toURI()).toString();
		pathName = UploadPathUtils.create(pathName, version);
		results.add(Pair.of(pathName, (InputStream)new FileInputStream(file)));
	}
	
}
