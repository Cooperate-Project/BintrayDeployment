package de.cooperateproject.maven.bintray.deploy.api;

public class DeploymentException extends Exception {

	private static final long serialVersionUID = 4264832849135909722L;

	public DeploymentException(String message) {
		super(message);
	}

	public DeploymentException(String message, Throwable cause) {
		super(message, cause);
	}

}
