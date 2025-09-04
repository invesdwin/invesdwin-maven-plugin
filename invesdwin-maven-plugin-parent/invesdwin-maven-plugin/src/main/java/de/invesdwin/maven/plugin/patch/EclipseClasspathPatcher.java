package de.invesdwin.maven.plugin.patch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.invesdwin.maven.plugin.AInvesdwinMojo;

public class EclipseClasspathPatcher {

	private static final String TARGET_GENERATEDSOURCES_APT = "target/generated-sources/apt";
	private static final String TARGET_GENERATEDTESTSOURCES_APT = "target/generated-test-sources/apt";
	private AInvesdwinMojo parent;

	public EclipseClasspathPatcher(AInvesdwinMojo parent) {
		this.parent = parent;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!parent.isUseInvesdwinEclipseSettings()) {
			return;
		}
		File classpathFile = new File(parent.getProject().getBasedir(), ".classpath");
		if (!classpathFile.exists()) {
			return;
		}
		try {
			final String existingContent = FileUtils.readFileToString(classpathFile, Charset.defaultCharset());

			String newContent = existingContent;

			if (checkJavaSourceFolderExists()) {
				newContent = addMissingClasspathEntries(newContent);
			}

			if (!existingContent.equals(newContent)) {
				FileUtils.writeStringToFile(classpathFile, newContent, Charset.defaultCharset());
				parent.getBuildContext().refresh(classpathFile);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private String addMissingClasspathEntries(String content) throws IOException {
		String newContent = content;
		String classpathTag = "</classpath>";
		if(!content.contains(TARGET_GENERATEDSOURCES_APT) && checkFolderExists(TARGET_GENERATEDSOURCES_APT)) {
			newContent = newContent.replace(classpathTag,  newGeneratedSourcesClasspathEntryXml() + "\n" + classpathTag);
		}
		if(!content.contains(TARGET_GENERATEDTESTSOURCES_APT) && checkFolderExists(TARGET_GENERATEDTESTSOURCES_APT)) {
			newContent = newContent.replace(classpathTag, newGeneratedTestSourcesClasspathEntryXml() + "\n" + classpathTag);
		}
		return newContent;
	}
	
	private String newGeneratedSourcesClasspathEntryXml() {
		//	<classpathentry kind="src" path="target/generated-sources/apt">
		//		<attributes>
		//			<attribute name="optional" value="true"/>
		//			<attribute name="maven.pomderived" value="true"/>
		//			<attribute name="ignore_optional_problems" value="true"/>
		//			<attribute name="m2e-apt" value="true"/>
		//		</attributes>
		//	</classpathentry>
		StringBuilder sb = new StringBuilder();
		sb.append("\t<classpathentry kind=\"src\" path=\"");
		sb.append(TARGET_GENERATEDSOURCES_APT);
		sb.append("\">");
		sb.append("\n\t\t<attributes>");
		sb.append("\n\t\t\t<attribute name=\"optional\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"maven.pomderived\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"ignore_optional_problems\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"m2e-apt\" value=\"true\"/>");
		sb.append("\n\t\t</attributes>");
		sb.append("\n\t</classpathentry>");
		return sb.toString();
	}

	private String newGeneratedTestSourcesClasspathEntryXml() {
		//	<classpathentry kind="src" output="target/test-classes" path="target/generated-test-sources/apt">
		//		<attributes>
		//			<attribute name="optional" value="true"/>
		//			<attribute name="maven.pomderived" value="true"/>
		//			<attribute name="ignore_optional_problems" value="true"/>
		//			<attribute name="m2e-apt" value="true"/>
		//			<attribute name="test" value="true"/>
		//		</attributes>
		//	</classpathentry>
		StringBuilder sb = new StringBuilder();
		sb.append("\t<classpathentry kind=\"src\" output=\"target/test-classes\" path=\"");
		sb.append(TARGET_GENERATEDTESTSOURCES_APT);
		sb.append("\">");
		sb.append("\n\t\t<attributes>");
		sb.append("\n\t\t\t<attribute name=\"optional\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"maven.pomderived\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"ignore_optional_problems\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"m2e-apt\" value=\"true\"/>");
		sb.append("\n\t\t\t<attribute name=\"test\" value=\"true\"/>");
		sb.append("\n\t\t</attributes>");
		sb.append("\n\t</classpathentry>");
		return sb.toString();
	}

	private String newNatureXml(String nature) {
		StringBuilder sb = new StringBuilder("<nature>");
		sb.append(nature);
		sb.append("</nature>");
		return sb.toString();
	}

	private boolean checkJavaSourceFolderExists() {
		return checkFolderExists("src/main/java");
	}
	
	private boolean checkFolderExists(String path) {
		return new File(parent.getProject().getBasedir(), path).exists();
	}

}
