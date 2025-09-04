package de.invesdwin.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.invesdwin.maven.plugin.patch.EclipseClasspathPatcher;
import de.invesdwin.maven.plugin.patch.EclipseProjectPatcher;

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


	protected void internalExecute() throws MojoExecutionException, MojoFailureException {
		new EclipseProjectPatcher(this).execute();
		new EclipseClasspathPatcher(this).execute();
	}

}
