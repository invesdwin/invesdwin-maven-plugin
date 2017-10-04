package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@NotThreadSafe(/*
				 * Threadsafe for maven execution with multiple instances, but not a threadsafe
				 * instance
				 */)
public abstract class AInvesdwinMojo extends AbstractMojo {

	public static final String DEFAULT_CODE_COMPLIANCE_LEVEL = "1.8";

	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;
	/**
	 * @component
	 */
	private BuildContext buildContext;
	/**
	 * @parameter default-value="false"
	 * @required
	 */
	private boolean useInvesdwinEclipseSettings;
	/**
	 * @parameter default-value="1.8"
	 * @required
	 */
	private String codeComplianceLevel = DEFAULT_CODE_COMPLIANCE_LEVEL;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		File pomFile = new File(getProject().getBasedir(), "pom.xml");
		if (buildContext.hasDelta(pomFile) || !buildContext.isIncremental()) {
			internalExecute();
		}
	}

	protected MavenProject getProject() {
		return project;
	}

	protected BuildContext getBuildContext() {
		return buildContext;
	}

	public boolean isUseInvesdwinEclipseSettings() {
		return useInvesdwinEclipseSettings;
	}

	public String getCodeComplianceLevel() {
		return codeComplianceLevel;
	}

	protected abstract void internalExecute() throws MojoExecutionException, MojoFailureException;

	protected boolean writeFileIfDifferent(File file, String newContent) throws IOException {
		boolean write;
		if (file.exists()) {
			try {
				String existingContent = FileUtils.readFileToString(file, Charset.defaultCharset());
				write = !existingContent.equals(newContent);
			} catch (IOException e) {
				write = true;
			}
		} else {
			write = true;
		}
		if (write) {
			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			buildContext.refresh(file);
			return true;
		} else {
			return false;
		}
	}

}
