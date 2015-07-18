package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.IOException;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@NotThreadSafe(/* Threadsafe for maven execution with multiple instances, but not a threadsafe instance */)
public abstract class AInvesdwinMojo extends AbstractMojo {

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
	

	@Override
	public final void execute() throws MojoExecutionException,
			MojoFailureException {
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

	protected abstract void internalExecute() throws MojoExecutionException, MojoFailureException;

	protected boolean writeFileIfDifferent(File file,
			String newContent) throws IOException {
		final boolean write;
		if (file.exists()) {
			String existingContent = FileUtils
					.readFileToString(file);
			write = !existingContent.equals(newContent);
		} else {
			write = true;
		}
		if (write) {
			FileUtils.writeStringToFile(file, newContent);
			buildContext.refresh(file);
			return true;
		} else {
			return false;
		}
	}
	
}
