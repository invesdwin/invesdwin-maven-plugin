package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

/**
 * @phase generate-sources
 * @goal generate-sources
 * @threadSafe true
 */
@NotThreadSafe(/* Threadsafe for maven execution with multiple instances, but not a threadsafe instance */)
public class GenerateSourcesMojo extends AInvesdwinMojo {

	@Override
	protected void internalExecute() throws MojoExecutionException,
			MojoFailureException {
		File[] xsdDirs = {
				new File(getProject().getBasedir(),
						"src/main/resources/META-INF/xsd"),
				new File(getProject().getBasedir(),
						"src/main/java/META-INF/xsd") };
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
				} catch(IOException e){
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void generateMergedJaxbContextPath(XPath xpath, File xsdFile)
			throws IOException, XPathExpressionException {
		String targetNamespace = (String) xpath.evaluate(
				"//*[local-name()='schema']/@targetNamespace", new InputSource(
						new FileInputStream(xsdFile)), XPathConstants.STRING);
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
		packageDeclaration = packageDeclaration.substring(0,
				packageDeclaration.length() - 1);
		packageFilePath = packageFilePath.substring(0,
				packageFilePath.length() - 1);

		String schemaPath = xsdFile.getAbsolutePath().replace("\\", "/");
		schemaPath = schemaPath.substring(schemaPath.indexOf("/META-INF/"));

		String className = "MergedJaxbContextPath_"
				+ xsdFile.getName().replace(".xsd", "").replace(".", "_");

		StringBuilder sb = new StringBuilder();
		sb.append("package " + packageDeclaration + ";\n");
		sb.append("\n");
		sb.append("import de.invesdwin.context.integration.IMergedJaxbContextPath;\n");
		sb.append("import javax.inject.Named;\n");
		sb.append("\n");
		sb.append("@Named(\"mergedJaxbContextPath_" + packageDeclaration
				+ "\")\n");
		sb.append("public class " + className
				+ " implements IMergedJaxbContextPath {\n");
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

		File genDir = new File(getProject().getBasedir().getAbsolutePath()
				+ "/target/generated-sources/invesdwin");
		FileUtils.forceMkdir(genDir);
		File genFile = new File(genDir, packageFilePath + "/" + className
				+ ".java");
		if (writeFileIfDifferent(genFile, newContent)) {
			getLog().debug("Generated [" + genFile + "]");
		} else {
			getLog().debug(
					"Skipping [" + genFile
							+ "] because file is already up to date");
		}
	}

}
