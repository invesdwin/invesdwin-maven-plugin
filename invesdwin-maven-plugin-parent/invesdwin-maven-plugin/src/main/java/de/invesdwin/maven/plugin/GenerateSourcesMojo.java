package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;
import org.xml.sax.InputSource;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import com.sun.codemodel.JCodeModel;
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
		invokeJsonschema2pojo();
		invokeXjc();
		generateMergedJaxbContextPaths();
	}

	private void invokeJsonschema2pojo() {
		// <plugin>
		// <groupId>org.jsonschema2pojo</groupId>
		// <artifactId>jsonschema2pojo-maven-plugin</artifactId>
		// <version>${version.jsonschema2pojo-maven-plugin}</version>
		// <configuration>
		// <sourceDirectory>${project.basedir}/src/main/java/META-INF/jsonschema</sourceDirectory>
		// <targetPackage>de.invesdwin.jsonschema</targetPackage>
		// <useCommonsLang3>true</useCommonsLang3>
		// <useDoubleNumbers>true</useDoubleNumbers>
		// <useLongIntegers>true</useLongIntegers>
		// <dateType>de.invesdwin.util.time.fdate.FDate</dateType>
		// <dateTimeType>de.invesdwin.util.time.fdate.FDate</dateTimeType>
		// <includeJsr303Annotations>true</includeJsr303Annotations>
		// <outputDirectory>${project.build.directory}/generated-sources/jsonschema</outputDirectory>
		// </configuration>
		// <executions>
		// <execution>
		// <goals>
		// <goal>generate</goal>
		// </goals>
		// </execution>
		// </executions>
		// </plugin>
		try {
			GenerationConfig config = new DefaultGenerationConfig() {
				@Override
				public boolean isGenerateBuilders() {
					return false;
				}

				@Override
				public boolean isIncludeJsr303Annotations() {
					return true;
				}

				@Override
				public boolean isIncludeJsr305Annotations() {
					return false;
				}

				@Override
				public boolean isUseJodaDates() {
					return true;
				}

				@Override
				public boolean isUseJodaLocalDates() {
					return true;
				}

				@Override
				public boolean isUseJodaLocalTimes() {
					return true;
				}

				@Override
				public Iterator<URL> getSource() {
					return getJsonschemaDirs().iterator();
				}

				@Override
				public File getTargetDirectory() {
					return getGenDir();
				}

				@Override
				public String getTargetPackage() {
					return "de.invesdwin.jsonschema";
				}

				@Override
				public boolean isUseBigDecimals() {
					return true;
				}

				@Override
				public boolean isUseBigIntegers() {
					return true;
				}

				@Override
				public boolean isRemoveOldOutput() {
					return false;
				}

				@Override
				public boolean isIncludeAdditionalProperties() {
					return false;
				}

				@Override
				public boolean isIncludeToString() {
					return false;
				}

				@Override
				public boolean isIncludeHashcodeAndEquals() {
					return false;
				}
				
				@Override
				public boolean isIncludeDynamicBuilders() {
					return false;
				}
				
			};

			Jsonschema2Pojo.generate(config);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private void invokeXjc() throws MojoExecutionException {
		final List<String> args = new ArrayList<String>();
		args.add("-extension");
		args.add("-d");
		File genDir = getGenDir();
		args.add(genDir.getAbsolutePath());
		args.add("-b");
		args.add(new File(getProject().getBasedir(), ".settings/invesdwin.xjb").getAbsolutePath());

		boolean xsdFound = false;
		final File[] xsdDirs = getXsdDirs();
		for (File xsdDir : xsdDirs) {
			File[] xsds = xsdDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xsd");
				}
			});
			if (xsds != null) {
				for (File xsd : xsds) {
					args.add(xsd.getAbsolutePath());
					xsdFound = true;
				}
			}
		}

		if (xsdFound) {
			try {
				FileUtils.forceMkdir(genDir);
				callXjcFacade(args);
			} catch (Throwable e) {
				throw new MojoExecutionException("XJC args: " + args.toString(), e);
			}
		}
	}

	private void callXjcFacade(List<String> args) throws Exception {
		final String javaExecutable = System.getProperty("java.home") + File.separator+"bin"+File.separator+"java";
		final URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		StringBuilder classpath = new StringBuilder();
		for (final URL url : classLoader.getURLs()) {
			if (classpath.length() > 0) {
				classpath.append(File.pathSeparator);
			}
			classpath.append(new File(url.getFile()).getAbsolutePath());
		}
		List<String> command = new ArrayList<String>();
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
		File[] xsdDirs = getXsdDirs();
		for (File xsdDir : xsdDirs) {
			if (xsdDir.exists()) {
				XPath xpath = XPathFactory.newInstance().newXPath();
				try {
					for (File xsdFile : xsdDir.listFiles()) {
						if (xsdFile.getName().endsWith(".xsd")) {
							generateMergedJaxbContextPath(xpath, xsdFile);
						}
					}
				} catch (XPathExpressionException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private File[] getXsdDirs() {
		File[] xsdDirs = { new File(getProject().getBasedir(), "src/main/resources/META-INF/xsd"),
				new File(getProject().getBasedir(), "src/main/java/META-INF/xsd") };
		return xsdDirs;
	}

	private List<URL> getJsonschemaDirs() {
		try {
			File[] jsonschemaDirs = { new File(getProject().getBasedir(), "src/main/resources/META-INF/jsonschema"),
					new File(getProject().getBasedir(), "src/main/java/META-INF/jsonschema") };
			List<URL> existingDirs = new ArrayList<URL>();
			for (File dir : jsonschemaDirs) {
				if (dir.exists()) {
					existingDirs.add(dir.toURI().toURL());
				}
			}
			return existingDirs;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateMergedJaxbContextPath(XPath xpath, File xsdFile) throws IOException, XPathExpressionException {
		String targetNamespace = (String) xpath.evaluate("//*[local-name()='schema']/@targetNamespace",
				new InputSource(new FileInputStream(xsdFile)), XPathConstants.STRING);
		targetNamespace = targetNamespace.replace("http://www.", "");
		targetNamespace = targetNamespace.replace("http://", "");

		List<String> packages = new ArrayList<String>();
		for (String it : targetNamespace.split("/")) {
			List<String> dotSplit = Arrays.asList(it.split("\\."));
			Collections.reverse(dotSplit);
			packages.addAll(dotSplit);
		}
		String packageDeclaration = "";
		String packageFilePath = "";
		for (String p : packages) {
			packageDeclaration += p + ".";
			packageFilePath += p + "/";
		}
		packageDeclaration = packageDeclaration.substring(0, packageDeclaration.length() - 1);
		packageFilePath = packageFilePath.substring(0, packageFilePath.length() - 1);

		String schemaPath = xsdFile.getAbsolutePath().replace("\\", "/");
		schemaPath = schemaPath.substring(schemaPath.indexOf("/META-INF/"));

		String className = "MergedJaxbContextPath_" + xsdFile.getName().replace(".xsd", "").replace(".", "_");

		StringBuilder sb = new StringBuilder();
		sb.append("package " + packageDeclaration + ";\n");
		sb.append("\n");
		sb.append("import de.invesdwin.context.integration.IMergedJaxbContextPath;\n");
		sb.append("import javax.inject.Named;\n");
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
		String newContent = sb.toString();

		File genDir = getGenDir();
		FileUtils.forceMkdir(genDir);
		File genFile = new File(genDir, packageFilePath + "/" + className + ".java");
		if (writeFileIfDifferent(genFile, newContent)) {
			getLog().debug("Generated [" + genFile + "]");
		} else {
			getLog().debug("Skipping [" + genFile + "] because file is already up to date");
		}
	}

	private File getGenDir() {
		File genDir = new File(getProject().getBasedir().getAbsolutePath() + "/target/generated-sources/invesdwin");
		return genDir;
	}

}
