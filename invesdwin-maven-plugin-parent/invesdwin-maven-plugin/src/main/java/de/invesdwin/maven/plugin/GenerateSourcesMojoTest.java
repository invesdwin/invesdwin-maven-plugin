package de.invesdwin.maven.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import com.sun.tools.xjc.XJCFacade;

public class GenerateSourcesMojoTest {
	
	public static void main(String[] args) throws Exception {
		
		callXjcFacade(Arrays.asList("--help"));
	}
	
	private static void callXjcFacade(List<String> args) throws Exception {
		final String javaExecutable = System.getProperty("java.home") + File.separator+"bin"+File.separator+"java";
		final URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		StringBuilder classpath = new StringBuilder();
		for (final URL url : classLoader.getURLs()) {
//			if (url.toString().toUpperCase().contains("XJC") || url.toString().toUpperCase().contains("JAXB")) {
			if (classpath.length() > 0) {
				classpath.append(File.pathSeparator);
			}
			classpath.append(new File(url.getFile()).getAbsolutePath());
//			}
		}
		List<String> command = new ArrayList<String>();
		command.add(javaExecutable);
		command.add("-classpath");
		command.add(classpath.toString());
		command.add(XJCFacade.class.getName());
		command.addAll(args);
		new ProcessExecutor().command(command).destroyOnExit().exitValueNormal()
				.redirectOutput(System.out)
				.redirectError(System.err).execute();
	}

}
