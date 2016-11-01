package de.cooperateproject.maven.bintray.deploy.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.jfrog.bintray.client.api.BintrayCallException;
import com.jfrog.bintray.client.api.MultipleBintrayCallException;
import com.jfrog.bintray.client.api.details.VersionDetails;
import com.jfrog.bintray.client.api.handle.Bintray;
import com.jfrog.bintray.client.api.handle.PackageHandle;
import com.jfrog.bintray.client.api.handle.RepositoryHandle;
import com.jfrog.bintray.client.api.handle.SubjectHandle;
import com.jfrog.bintray.client.api.handle.VersionHandle;
import com.jfrog.bintray.client.impl.BintrayClient;

public class BintrayAPI implements AutoCloseable {

	private final Bintray api;
	private final PackageHandle pkg;

	public BintrayAPI(String username, String password, String subject, String repo, String pkg) throws BintrayCallException, InitializationException {
		super();
		this.api = createBintray(username, password);
		this.pkg = getPackage(api, subject, repo, pkg);
	}
	
	public void uploadAndPublish(File fileToPublish, String versionName, boolean createVersionIfMissing,
			boolean replaceIfExisting) throws DeploymentException, IOException {
		
		VersionHandle version = pkg.version(versionName);
		if (!version.exists() && !createVersionIfMissing) {
			throw new DeploymentException(String.format("The version \"%s\" does not exist.", versionName));
		}
		
		if (version.exists() && !replaceIfExisting) {
			throw new DeploymentException(String.format("The version \"%s\" already exists.", versionName));
		}

		if (version.exists()) {
			version.delete();
		}
		
		VersionDetails versionDetails = new VersionDetails(versionName);
		version = pkg.createVersion(versionDetails);
		
		try {
			uploadAndPublish(fileToPublish, version);
			version.publish();
		} catch (IOException e) {
			// cleanup version
			throw new DeploymentException("An error occurred during the upload.", e);
		}
	}
	
	private void uploadAndPublish(File fileToPublish, final VersionHandle version) throws IOException {
		if (fileToPublish.isDirectory()) {
			uploadAndPublishDirectory(fileToPublish, version);
		} else {
			uploadAndPublishSingleFile(fileToPublish, fileToPublish.getName(), version);
		}
	}
	
	private void uploadAndPublishDirectory(File file, final VersionHandle version) throws IOException {
		UploadMapCalculator calculator = new UploadMapCalculator(file, version.name());
		Map<String, InputStream> uploadMap = calculator.getUploadMap();
		try {
			version.upload(uploadMap);
		} catch (MultipleBintrayCallException e) {
			throw new IOException(e);
		} finally {
			UploadMapCalculator.close(uploadMap);
		}
		
	}
	
	private void uploadAndPublishSingleFile(File file, String path, VersionHandle version) throws FileNotFoundException, BintrayCallException {
		InputStream is = new FileInputStream(file);
		try {
			String pathName = UploadPathUtils.create(path, version.name());
			version.upload(pathName, is);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
	
	private static Bintray createBintray(String username, String password) {
		return BintrayClient.create(username, password);
	}
	
	private static PackageHandle getPackage(Bintray api, String subjectName, String repoName, String pkgName) throws BintrayCallException, InitializationException {
		SubjectHandle subject = api.subject(subjectName);		
		RepositoryHandle repository = subject.repository(repoName);
		if (!repository.exists()) {
			throw new InitializationException(String.format("A combination of subject \"%s\" and repository \"%s\" does not exist.", subjectName, repoName));
		}
		PackageHandle pkg = repository.pkg(pkgName);
		if (!pkg.exists()) {
			throw new InitializationException(String.format("The package \"%s\" does not exist.", pkgName));
		}
		return pkg;
	}

	public void close() {
		api.close();
	}

}
