package de.cooperateproject.maven.bintray.deploy;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import com.jfrog.bintray.client.api.BintrayCallException;

import de.cooperateproject.maven.bintray.deploy.api.BintrayAPI;
import de.cooperateproject.maven.bintray.deploy.api.DeploymentException;
import de.cooperateproject.maven.bintray.deploy.api.InitializationException;

@Mojo(name = "deploy")
public class Deployment extends AbstractMojo {

	// internal fields

	@Component(role = SettingsDecrypter.class)
	private DefaultSettingsDecrypter settingsDecrypter;

	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	private Settings settings;

	// public settable fields

	@Parameter(property = "bintray.deploy.serverId", required = true)
	private String serverId;

	@Parameter(property = "bintray.deploy.subject", required = false, defaultValue = "")
	private String subjectName;

	@Parameter(property = "bintray.deploy.repository", required = true)
	private String repositoryName;

	@Parameter(property = "bintray.deploy.package", required = true)
	private String packageName;

	@Parameter(property = "bintray.deploy.version", required = false, defaultValue = "${project.version}")
	private String version;

	@Parameter(property = "bintray.deploy.file", required = true)
	private File fileToPublish;

	@Parameter(property = "bintray.deploy.snapshotVersionReplacement", required = false, defaultValue = "latest")
	private String snapshotVersionReplacement;

	@Parameter(property = "bintray.deploy.createVersionIfMissing", required = false, defaultValue = "true")
	private boolean createVersionIfMissing;

	@Parameter(property = "bintray.deploy.replaceIfExisting", required = false, defaultValue = "false")
	private boolean replaceIfExisting;
	
	@Parameter(property = "bintray.deploy.replaceIfSnapshot", required = false, defaultValue = "false")
	private boolean replaceIfSnapshot;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Server chosenServer = getServer(serverId);
		
		if (StringUtils.isEmpty(subjectName)) {
			subjectName = chosenServer.getUsername();
		}
		
		boolean replaceExisting = replaceIfExisting;
		String versionName = version;
		if (versionName.endsWith("-SNAPSHOT")) {
			versionName = snapshotVersionReplacement;
			replaceExisting = replaceIfSnapshot;
		}

		try (BintrayAPI bintrayAPI = new BintrayAPI(chosenServer.getUsername(), chosenServer.getPassword(), subjectName,
				repositoryName, packageName)) {
			bintrayAPI.uploadAndPublish(fileToPublish, versionName, createVersionIfMissing, replaceExisting);
		} catch (BintrayCallException e) {
			throw new MojoExecutionException("An error in communicating via Bintray API occurred.", e);
		} catch (InitializationException e) {
			throw new MojoExecutionException("The given bintray configuration is not correct.", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Unknown file access exception occurred.", e);
		} catch (DeploymentException e) {
			throw new MojoExecutionException("An error during the deployment of the files occurred.", e);
		}

	}

	private Server getServer(String serverId) throws MojoExecutionException {
		Server chosenServer = settings.getServer(serverId);
		if (chosenServer == null) {
			throw new MojoExecutionException("You must provide a valid server id that points to a bintray repository.");
		}
		DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(chosenServer);
		SettingsDecryptionResult result = settingsDecrypter.decrypt(request);
		return result.getServer();
	}

}
