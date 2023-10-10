package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @phase initialize
 * @goal initialize
 * @threadSafe true
 */
@NotThreadSafe(/*
				 * Threadsafe for maven execution with multiple instances, but not a threadsafe
				 * instance
				 */)
public class InitializeMojo extends AInvesdwinMojo {

	private static final String SETTINGS_SCM_IGNORE = ".settings/scm_ignore";
	private static final String INVESDWIN_ECLIPSE_SETTINGS = "invesdwin-eclipse-settings";
	private static final List<String> CODE_COMPLIANCE_SETTINGS = Arrays.asList("org.eclipse.jdt.core.compiler.source=",
			"org.eclipse.jdt.core.compiler.codegen.targetPlatform=", "org.eclipse.jdt.core.compiler.compliance=");
	private String springBeansConfigs;

	protected void internalExecute() throws MojoExecutionException, MojoFailureException {
		extractConfigFiles();
		if (isUseInvesdwinEclipseSettings()) {
			if (new File(getProject().getBasedir(), SETTINGS_SCM_IGNORE).exists()) {
				updateSvnProperties();
				updateGitProperties();
			}
			createDefaultFolders();
			deleteFactoryPath();
		}
	}
	
	private void deleteFactoryPath() {
		File parent = getProject().getBasedir();
		File factorypath = new File(parent, ".factorypath");
		if (factorypath.exists()) {
			factorypath.delete();
		}
	}

	private boolean detectScmFolder(String folderName) {
		File parent = getProject().getBasedir();
		while (parent != null) {
			if (new File(parent, folderName).exists()) {
				return true;
			} else {
				parent = parent.getParentFile();
			}
		}
		getLog().debug(".svn folder not found, skipping svn properties");
		return false;
	}

	private void extractConfigFiles() {
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource[] resources = resolver.getResources("classpath*:/" + INVESDWIN_ECLIPSE_SETTINGS + "/**");

			for (Resource resource : resources) {
				File resourceFile = resourceToFile(resource);
				if ((resourceFile == null || !resourceFile.isDirectory()) && resource.contentLength() > 0) {
					String uri = resource.getURI().toString();
					// only xjb file should be extracted if no eclipse settings
					// are used
					if (!isUseInvesdwinEclipseSettings() && !uri.endsWith("invesdwin.xjb")) {
						continue;
					}
					String path = uri.substring(
							uri.indexOf(INVESDWIN_ECLIPSE_SETTINGS) + INVESDWIN_ECLIPSE_SETTINGS.length() + 1);
					File toPath = new File(getProject().getBasedir(), path);
					InputStream in = resource.getInputStream();
					String newContent = IOUtils.toString(in, Charset.defaultCharset());
					in.close();
					newContent = newContent.replace("[PROJECTNAME]", getProject().getName());
					newContent = newContent.replace("[BASEDIR]", getProject().getBasedir().getAbsolutePath());
					newContent = newContent.replace("[SPRINGBEANS_CONFIGS]", getSpringBeansConfigs());
					for (String codeComplianceSetting : CODE_COMPLIANCE_SETTINGS) {
						newContent = newContent.replace(codeComplianceSetting + DEFAULT_CODE_COMPLIANCE_LEVEL,
								codeComplianceSetting + getCodeComplianceLevel());
					}

					if (writeFileIfDifferent(toPath, newContent)) {
						getLog().debug("Extracted [" + path + "] to [" + toPath + "]");
					} else {
						getLog().debug("Skipping [" + path + "] since [" + toPath + "] is already up to date");
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected File resourceToFile(Resource resource) {
		try {
			return resource.getFile();
		} catch (Throwable t) {
			return null;
		}
	}

	private String getSpringBeansConfigs() throws IOException {
		if (springBeansConfigs == null) {
			File[] dirs = { new File(getProject().getBasedir(), "src/main/resources/META-INF"),
					new File(getProject().getBasedir(), "src/test/resources/META-INF") };
			StringBuilder sb = new StringBuilder();
			for (File dir : dirs) {
				if (dir.exists()) {
					for (File file : dir.listFiles()) {
						if (file.getName().endsWith(".xml")) {
							String content = FileUtils.readFileToString(file, Charset.defaultCharset());
							if (content.contains("<beans")) {
								String path = file.getAbsolutePath()
										.replace(getProject().getBasedir().getAbsolutePath() + "/", "");
								sb.append("\n\t\t<config>" + path + "</config>");
							}
						}
					}
				}
			}
			springBeansConfigs = sb.toString();
		}
		return springBeansConfigs;
	}

	private void updateSvnProperties() {
		if (!detectScmFolder(".svn")) {
			return;
		}

		Throwable svnCommandException = null;
		try {
			String command = "svn propset svn:ignore --file "
					+ new File(getProject().getBasedir(), SETTINGS_SCM_IGNORE).getAbsolutePath() + " "
					+ getProject().getBasedir().getAbsolutePath();

			int returnCode = CommandLineUtils.executeCommandLine(new Commandline(command), new StreamConsumer() {
				@Override
				public void consumeLine(String line) {
					getLog().debug(line);
				}
			}, new StreamConsumer() {
				@Override
				public void consumeLine(String line) {
					getLog().error(line);
				}
			});
			if (returnCode != 0) {
				svnCommandException = new IllegalStateException("[" + command + "] failed with return code ["
						+ returnCode + "]. Maybe the svn command line tools are not installed or are missing in PATH?");
			}
		} catch (CommandLineException e) {
			svnCommandException = e;
		}
		if (svnCommandException != null) {
			getLog().debug("Failed updating svn:ignore", svnCommandException);
		}
	}

	private void updateGitProperties() {
		if (!detectScmFolder(".git")) {
			return;
		}

		try {
			String newContent = FileUtils.readFileToString(new File(getProject().getBasedir(), SETTINGS_SCM_IGNORE),
					Charset.defaultCharset()).replaceAll("[\n\r]+", "\n");

			// filter only in current directory
			// see:
			// http://git.661346.n2.nabble.com/gitignore-how-to-ignore-only-in-current-directory-td5091741.html
			String newAdjContent = "";
			for (String line : newContent.split("\n")) {
				if (!StringUtils.isBlank(line) && !line.startsWith("/")) {
					newAdjContent += "/" + line + "\n";
				}
			}

			writeFileIfDifferent(new File(getProject().getBasedir(), ".gitignore"), newAdjContent);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void createDefaultFolders() {
		if (checkSourceFoldersMustExist()) {
			try {
				for (String mandatoryDir : new String[] { "src/main/java", "src/test/java" }) {
					FileUtils.forceMkdir(new File(getProject().getBasedir(), mandatoryDir));
				}
				for (String optionalDir : new String[] { "src/main/resources", "src/test/resources" }) {
					File dir = new File(getProject().getBasedir(), optionalDir);
					if (dir.exists() && dir.list().length == 0) {
						dir.delete();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private boolean checkSourceFoldersMustExist() {
		if(new File(getProject().getBasedir(), "src").exists()) {
			return true;
		}
		final String packaging = getProject().getPackaging();
		if("jar".equalsIgnoreCase(packaging)) {
			return true;
		}
		return false;
	}

}
