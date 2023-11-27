package de.invesdwin.maven.plugin.shade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.codehaus.plexus.util.IOUtil;

/**
 * A resource processor that appends content for a resource, separated by a
 * newline.
 */
public class RegexAppendingTransformer implements ResourceTransformer {
	String resource;
	private Pattern resourcePattern;

	Map<String, ByteArrayOutputStream> resource_data = new HashMap<String, ByteArrayOutputStream>();

	public boolean canTransformResource(String r) {
		if(resource == null) {
			return false;
		}
		if(resourcePattern == null) {
			resourcePattern = Pattern.compile(resource);
		}
		if (resourcePattern.matcher(r).matches()) {
			return true;
		}

		return false;
	}

	public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
		ByteArrayOutputStream data = resource_data.computeIfAbsent(resource, (key) -> new ByteArrayOutputStream());
		IOUtil.copy(is, data);
		data.write('\n');
	}

	public boolean hasTransformedResource() {
		return !resource_data.isEmpty();
	}

	public void modifyOutputStream(JarOutputStream jos) throws IOException {
		for (Entry<String, ByteArrayOutputStream> e : resource_data.entrySet()) {
			String resource = e.getKey();
			jos.putNextEntry(new JarEntry(resource));

			ByteArrayOutputStream data = e.getValue();
			IOUtil.copy(new ByteArrayInputStream(data.toByteArray()), jos);
			data.reset();
		}
	}
}
