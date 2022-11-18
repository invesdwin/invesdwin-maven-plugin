package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.xml.sax.InputSource;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import com.sun.tools.xjc.XJCFacade;

/**
 * @phase generate-sources
 * @goal generate-sources
 * @threadSafe true
 */
@NotThreadSafe(/*
				 * Threadsafe for maven execution with multiple instances, but not a threadsafe
				 * instance
				 */)
public class GenerateSourcesMojo extends AInvesdwinMojo {

	@Override
	protected void internalExecute() throws MojoExecutionException, MojoFailureException {
		invokeXjc();
		generateMergedJaxbContextPaths();
	}

	private void invokeXjc() throws MojoExecutionException {
		final List<String> args = new ArrayList<String>();
		args.add("-extension");
		args.add("-d");
		final File genDir = getGenDir();
		args.add(genDir.getAbsolutePath());
		args.add("-b");
		args.add(new File(getProject().getBasedir(), ".settings/invesdwin.xjb").getAbsolutePath());

		boolean xsdFound = false;
		final File[] xsdDirs = getXsdDirs();
		for (final File xsdDir : xsdDirs) {
			final File[] xsds = xsdDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.endsWith(".xsd");
				}
			});
			if (xsds != null) {
				for (final File xsd : xsds) {
					args.add(xsd.getAbsolutePath());
					xsdFound = true;
				}
			}
		}

		if (xsdFound) {
			try {
				FileUtils.forceMkdir(genDir);
				callXjcFacade(args);
			} catch (final Throwable e) {
				throw new MojoExecutionException("XJC args: " + args.toString(), e);
			}
		}
	}

	private void callXjcFacade(final List<String> args) throws Exception {
		final String javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator
				+ "java";
		final URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		final StringBuilder classpath = new StringBuilder();
		for (final URL url : classLoader.getURLs()) {
			if (classpath.length() > 0) {
				classpath.append(File.pathSeparator);
			}
			classpath.append(new File(url.getFile()).getAbsolutePath());
		}
		final List<String> command = new ArrayList<String>();
		command.add(javaExecutable);
		command.add("-classpath");
		command.add(classpath.toString());
		command.add(XJCFacade.class.getName());
		command.addAll(args);
		new ProcessExecutor().command(command).destroyOnExit().exitValueNormal()
				.redirectOutput(Slf4jStream.of(GenerateSourcesMojo.class).asInfo())
				.redirectError(Slf4jStream.of(GenerateSourcesMojo.class).asWarn()).execute();
	}

	private void generateMergedJaxbContextPaths() {
		final File[] xsdDirs = getXsdDirs();
		for (final File xsdDir : xsdDirs) {
			if (xsdDir.exists()) {
				final XPath xpath = XPathFactory.newInstance().newXPath();
				try {
					for (final File xsdFile : xsdDir.listFiles()) {
						if (xsdFile.getName().endsWith(".xsd")) {
							generateMergedJaxbContextPath(xpath, xsdFile);
						}
					}
				} catch (final XPathExpressionException e) {
					throw new RuntimeException(e);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private File[] getXsdDirs() {
		final File[] xsdDirs = { new File(getProject().getBasedir(), "src/main/resources/META-INF/xsd"),
				new File(getProject().getBasedir(), "src/main/java/META-INF/xsd") };
		return xsdDirs;
	}

	private void generateMergedJaxbContextPath(final XPath xpath, final File xsdFile)
			throws IOException, XPathExpressionException {
		String targetNamespace = (String) xpath.evaluate("//*[local-name()='schema']/@targetNamespace",
				new InputSource(new FileInputStream(xsdFile)), XPathConstants.STRING);
		targetNamespace = targetNamespace.replace("http://www.", "");
		targetNamespace = targetNamespace.replace("http://", "");

		final List<String> packages = new ArrayList<String>();
		for (final String it : targetNamespace.split("/")) {
			final List<String> dotSplit = Arrays.asList(it.split("\\."));
			Collections.reverse(dotSplit);
			packages.addAll(dotSplit);
		}
		String packageDeclaration = "";
		String packageFilePath = "";
		for (final String p : packages) {
			packageDeclaration += p + ".";
			packageFilePath += p + "/";
		}
		packageDeclaration = packageDeclaration.substring(0, packageDeclaration.length() - 1);
		packageFilePath = packageFilePath.substring(0, packageFilePath.length() - 1);

		String schemaPath = xsdFile.getAbsolutePath().replace("\\", "/");
		schemaPath = schemaPath.substring(schemaPath.indexOf("/META-INF/"));

		final String className = "MergedJaxbContextPath_" + xsdFile.getName().replace(".xsd", "").replace(".", "_");

		final StringBuilder sb = new StringBuilder();
		sb.append("package " + packageDeclaration + ";\n");
		sb.append("\n");
		sb.append("import de.invesdwin.context.integration.marshaller.IMergedJaxbContextPath;\n");
		sb.append("import jakarta.inject.Named;\n");
		sb.append("\n");
		sb.append("@Named(\"mergedJaxbContextPath_" + packageDeclaration + "\")\n");
		sb.append("public class " + className + " implements IMergedJaxbContextPath {\n");
		sb.append("\n");
		sb.append("    @Override\n");
		sb.append("    public String getContextPath() {\n");
		sb.append("        return \"" + packageDeclaration + "\";\n");
		sb.append("    }\n");
		sb.append("\n");
		sb.append("    @Override\n");
		sb.append("    public String getSchemaPath() {\n");
		sb.append("        return \"" + schemaPath + "\";\n");
		sb.append("    }\n");
		sb.append("\n");
		sb.append("}\n");
		final String newContent = sb.toString();

		final File genDir = getGenDir();
		FileUtils.forceMkdir(genDir);
		final File genFile = new File(genDir, packageFilePath + "/" + className + ".java");
		if (writeFileIfDifferent(genFile, newContent)) {
			getLog().debug("Generated [" + genFile + "]");
		} else {
			getLog().debug("Skipping [" + genFile + "] because file is already up to date");
		}
	}

	private File getGenDir() {
		final File genDir = new File(
				getProject().getBasedir().getAbsolutePath() + "/target/generated-sources/invesdwin");
		return genDir;
	}

}
