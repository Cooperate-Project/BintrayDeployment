package de.cooperateproject.maven.bintray.deploy.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfrog.bintray.client.api.handle.VersionHandle;

public class UploadMapCalculator extends DirectoryWalker<Pair<String, InputStream>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadMapCalculator.class);
	private final File directory;
	private final VersionHandle version;

	public UploadMapCalculator(File directory, VersionHandle version) {
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
	protected void handleFile(File file, int depth, Collection<Pair<String, InputStream>> results)
			throws FileNotFoundException {
		String pathName = directory.toURI().relativize(file.toURI()).toString();

		if (!skipPathConstruction(file)) {
			pathName = UploadPathUtils.create(pathName, version.pkg().name(), version.name());
		}

		results.add(Pair.of(pathName, (InputStream) new FileInputStream(file)));
		LOGGER.debug("File \"{}\" will be uploaded to \"{}\"", file.getPath(), pathName);
	}

	private static boolean skipPathConstruction(File file) {
		final Set<String> names = new HashSet<String>(Arrays.asList("content.jar", "artifacts.jar",
				"compositeContents.jar", "compositeArtifacts.jar", "p2.index"));
		return names.contains(file.getName());
	}

}
