package de.invesdwin.maven.plugin.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;

import de.invesdwin.maven.plugin.util.internal.WebFragmentOrdering;

// @NotThreadSafe
public abstract class AWebFragmentConfigurationMerger {

	private static final String CONFIGURATION_OPEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee \n"
            + "                             https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd\"\n"
            + "         version=\"5.0\">";
    private static final String CONFIGURATION_CLOSE = "</web-app>";

    private void handleException(final Throwable cause, final Resource r) {
        try {
            throw new RuntimeException("At: " + r.getURI(), cause);
        } catch (final IOException e) {
        	throw new RuntimeException(cause);
        }
    }

    private List<String> extractComponent() {
        try {
            final List<WebFragmentResource> resources = findResources();
            logWebFragmentsBeingLoaded(resources);
            final List<String> components = new ArrayList<String>();
            for (final WebFragmentResource r : resources) {
                components.addAll(r.getWebFragmentXmlComponents());
            }
            return components;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void logWebFragmentsBeingLoaded(final List<WebFragmentResource> webFragments);

    public List<WebFragmentResource> findResources() throws IOException {
        final Iterable<? extends Resource> resources = getResources();
        final List<WebFragmentResource> webFragmentResources = new ArrayList<WebFragmentResource>();
        for (final Resource resource : resources) {
            try {
                webFragmentResources.add(new WebFragmentResource(resource));
            } catch (final Throwable e) {
                handleException(e, resource);
            }
        }
        final List<WebFragmentResource> orderedResources = new WebFragmentOrdering().order(webFragmentResources);
        return orderedResources;
    }

    protected abstract Iterable<? extends Resource> getResources() throws IOException;

    protected String mergeConfigs() {
        final StringBuilder merged = new StringBuilder(CONFIGURATION_OPEN);
        merged.append("\n");
        final List<String> componentContents = extractComponent();
        for (final String componentContent : componentContents) {
            merged.append("\t");
            merged.append(componentContent);
            merged.append("\n");
        }
        merged.append(CONFIGURATION_CLOSE);
        return merged.toString();
    }

}
