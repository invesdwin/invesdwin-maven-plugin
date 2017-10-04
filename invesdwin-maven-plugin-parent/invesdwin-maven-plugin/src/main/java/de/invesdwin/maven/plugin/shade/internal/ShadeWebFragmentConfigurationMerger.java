package de.invesdwin.maven.plugin.shade.internal;

import java.io.IOException;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.springframework.core.io.Resource;

import de.invesdwin.maven.plugin.util.AWebFragmentConfigurationMerger;
import de.invesdwin.maven.plugin.util.WebFragmentResource;

@NotThreadSafe
public class ShadeWebFragmentConfigurationMerger extends AWebFragmentConfigurationMerger {

	private List<? extends Resource> resources;

	public ShadeWebFragmentConfigurationMerger(List<? extends Resource> resources) {
		this.resources = resources;
	}

	@Override
	protected void logWebFragmentsBeingLoaded(final List<WebFragmentResource> webFragments) {
	}

	public String getMergedContent() throws IOException {
		final String merged = mergeConfigs();
		return merged;
	}

	@Override
	protected Iterable<? extends Resource> getResources() throws IOException {
		return resources;
	}

}
