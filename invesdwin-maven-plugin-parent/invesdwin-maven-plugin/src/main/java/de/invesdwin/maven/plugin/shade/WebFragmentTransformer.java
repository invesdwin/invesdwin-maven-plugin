package de.invesdwin.maven.plugin.shade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.codehaus.plexus.util.IOUtil;

import de.invesdwin.maven.plugin.shade.internal.ShadeWebFragmentConfigurationMerger;
import de.invesdwin.maven.plugin.shade.internal.TransformerResource;

public class WebFragmentTransformer implements ResourceTransformer {

	private String resource;
	private List<TransformerResource> resources = new ArrayList<TransformerResource>();

	public boolean canTransformResource(String r) {
		if (resource != null && resource.equalsIgnoreCase(r)) {
			return true;
		}

		return false;
	}

	public void processResource(String resource, InputStream is,
			List<Relocator> relocators) throws IOException {
		resources.add(new TransformerResource(resource, is));
	}

	public boolean hasTransformedResource() {
		return resources.size() > 0;
	}

	public void modifyOutputStream(JarOutputStream jos) throws IOException {
		jos.putNextEntry(new JarEntry(resource));

		String content = new ShadeWebFragmentConfigurationMerger(resources)
				.getMergedContent();

		IOUtil.copy(new ByteArrayInputStream(content.getBytes()), jos);
		resources.clear();
	}

}
