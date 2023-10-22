package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @phase compile
 * @goal compile
 * @threadSafe true
 */
@NotThreadSafe(/*
				 * Threadsafe for maven execution with multiple instances, but not a threadsafe
				 * instance
				 */)
public class CompileMojo extends AInvesdwinMojo {

	private static final String[] ADDITIONAL_PROJECT_BUILD_COMMANDS_FOR_JAVA_PROJECTS = {
			"net.sf.eclipsecs.core.CheckstyleBuilder", "edu.umd.cs.findbugs.plugin.eclipse.findbugsBuilder" };
	private static final String[] ADDITIONAL_PROJECT_NATURES_FOR_JAVA_PROJECTS = {
			"net.sf.eclipsecs.core.CheckstyleNature", "edu.umd.cs.findbugs.plugin.eclipse.findbugsNature" };
	private static final String[] REMOVE_PROJECT_NATURES = { "org.eclipse.pde.PluginNature",
			"com.github.spotbugs.plugin.eclipse.findbugsNature", "org.eclipse.jem.workbench.JavaEMFNature",
			"org.eclipse.wst.common.project.facet.core.nature" };
	private static final String[] REMOVE_PROJECT_BUILD_COMMANDS = {
			"com.github.spotbugs.plugin.eclipse.findbugsBuilder", "org.eclipse.wst.common.project.facet.core.builder",
			"org.eclipse.wst.validation.validationbuilder" };

	protected void internalExecute() throws MojoExecutionException, MojoFailureException {
		if (isUseInvesdwinEclipseSettings()) {
			File projectFile = new File(getProject().getBasedir(), ".project");
			if (!projectFile.exists()) {
				return;
			}
			try {
				final String existingContent = FileUtils.readFileToString(projectFile, Charset.defaultCharset());

				String newContent = existingContent;

				if (checkJavaSourceFolderExists()) {
					newContent = addProjectNaturesAndBuildersForJavaProjects(newContent);
				} else {
					newContent = removeProjectNaturesAndBuildersForJavaProjects(newContent);
				}
				newContent = removeUnwantedProjectNaturesAndBuilders(newContent);

				if (!existingContent.equals(newContent)) {
					FileUtils.writeStringToFile(projectFile, newContent, Charset.defaultCharset());
					getBuildContext().refresh(projectFile);
				}

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private String removeUnwantedProjectNaturesAndBuilders(String content) {
		String newContent = content;
		for (String nature : REMOVE_PROJECT_NATURES) {
			newContent = newContent.replaceAll("(?s)<nature>\\s*" + nature + "</nature>", "");
		}
		for (String command : REMOVE_PROJECT_BUILD_COMMANDS) {
			newContent = newContent.replaceAll("(?s)<buildCommand>\\s*<name>" + command
					+ "</name>\\s*<arguments>\\s*</arguments>\\s*</buildCommand>", "");
		}
		return newContent;
	}

	private String removeProjectNaturesAndBuildersForJavaProjects(String content) {
		String newContent = content;

		for (String command : ADDITIONAL_PROJECT_BUILD_COMMANDS_FOR_JAVA_PROJECTS) {
			newContent = newContent.replaceAll("(?s)<buildCommand>\\s*<name>" + command
					+ "</name>\\s*<arguments>\\s*</arguments>\\s*</buildCommand>", "");
		}

		for (String nature : ADDITIONAL_PROJECT_NATURES_FOR_JAVA_PROJECTS) {
			newContent = newContent.replaceAll("(?s)<nature>\\s*" + nature + "</nature>", "");
		}

		return newContent;
	}

	private String addProjectNaturesAndBuildersForJavaProjects(String content) throws IOException {
		String newContent = content;

		// put new builders before m2e builder to prevent m2e warning about
		// .project being out of sync
		String buildSpecTag = "<name>org.eclipse.m2e.core.maven2Builder</name>";
		for (String buildCommand : ADDITIONAL_PROJECT_BUILD_COMMANDS_FOR_JAVA_PROJECTS) {
			if (!newContent.contains(buildCommand)) {
				newContent = newContent.replace(buildSpecTag,
						"\t" + newBuildCommandXml(buildCommand) + "\n\t\t" + buildSpecTag);
			}
		}

		String naturesTag = "</natures>";
		for (String nature : ADDITIONAL_PROJECT_NATURES_FOR_JAVA_PROJECTS) {
			if (!newContent.contains(nature)) {
				newContent = newContent.replace(naturesTag, "\t" + newNatureXml(nature) + "\n\t" + naturesTag);
			}
		}

		return newContent;
	}

	private String newBuildCommandXml(String buildCommand) {
		StringBuilder sb = new StringBuilder();
		sb.append("<name>");
		sb.append(buildCommand);
		sb.append("</name>");
		sb.append("\n\t\t\t<arguments>");
		sb.append("\n\t\t\t</arguments>");
		sb.append("\n\t\t</buildCommand>");
		sb.append("\n\t\t<buildCommand>");
		return sb.toString();
	}

	private String newNatureXml(String nature) {
		StringBuilder sb = new StringBuilder("<nature>");
		sb.append(nature);
		sb.append("</nature>");
		return sb.toString();
	}

	private boolean checkJavaSourceFolderExists() {
		return new File(getProject().getBasedir(), "src/main/java").exists();
	}

}
