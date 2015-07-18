package de.invesdwin.maven.plugin.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.core.io.Resource;

import de.invesdwin.maven.plugin.util.internal.WebFragmentOrderType;

// @NotThreadSafe
public class WebFragmentResource implements Resource {

	private final Resource delegate;
	private WebFragmentOrderType webFragmentOrderType = WebFragmentOrderType.None;
	private List<String> allWebFragmentNames = new ArrayList<String>();
	private final List<String> webFragmentBefores = new ArrayList<String>();
	private final List<String> webFragmentAfters = new ArrayList<String>();
	private final List<String> webFragmentXmlComponents = new ArrayList<String>();

	public WebFragmentResource(final Resource delegate)
			throws XMLStreamException, IOException, TransformerException {
		this.delegate = delegate;
		parseWebFragmentXml();
	}

	private void parseWebFragmentXml() throws XMLStreamException, IOException,
			TransformerException {
		final XMLInputFactory xif = XMLInputFactory.newInstance();
		final XMLStreamReader xsr = xif.createXMLStreamReader(getInputStream());
		xsr.nextTag(); // web-fragment tag skipped

		while (xsr.hasNext()
				&& xsr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			final String tagName = xsr.getLocalName();
			// webFragmentName
			if ("name".equalsIgnoreCase(tagName)) {
				final String element = xsr.getElementText();
				allWebFragmentNames.add(element);
				webFragmentXmlComponents.add("<name>" + element + "</name>");
			} else {
				final TransformerFactory tf = TransformerFactory.newInstance();
				final Transformer t = tf.newTransformer();
				final StringWriter res = new StringWriter();
				t.transform(new StAXSource(xsr), new StreamResult(res));
				final String element = org.apache.commons.lang3.StringUtils
						.substringAfter(res.toString(), "?>");
				if ("ordering".equalsIgnoreCase(tagName)) {
					// ordering
					parseOrdering(element);
				} else {
					// webFragmentXmlComponents
					webFragmentXmlComponents.add(element);
				}
			}
		}
		if (allWebFragmentNames.isEmpty()) {
			throw new IllegalStateException(
					"Name tag is missing. Please specify a name in the web-fragment.xml!");
		}
	}

	/**
	 * see https://blogs.oracle.com/swchan/entry/servlet_3_0_web_fragment
	 */
	private void parseOrdering(final String orderingTag)
			throws XMLStreamException, IOException {
		final XMLInputFactory xif = XMLInputFactory.newInstance();
		final XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(
				orderingTag));
		xsr.nextTag(); // ordering tag skipped

		Boolean before = null;
		while (xsr.hasNext()
				&& xsr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			final String tagName = xsr.getLocalName();
			if ("before".equalsIgnoreCase(tagName)) {
				before = true;
			} else if ("after".equalsIgnoreCase(tagName)) {
				before = false;
			} else if ("others".equalsIgnoreCase(tagName)) {
				if (before == null) {
					throw new IllegalStateException(
							"others tag is not nested inside before/after tag!");
				}
				// webFragmentOrderType
				if (BooleanUtils.isTrue(before)) {
					webFragmentOrderType = WebFragmentOrderType.Before;
				} else {
					webFragmentOrderType = WebFragmentOrderType.After;
				}
			} else if ("name".equalsIgnoreCase(tagName)) {
				if (before == null) {
					throw new IllegalStateException(
							"name tag is not nested inside before/after tag!");
				}
				final String element = xsr.getElementText();
				if (BooleanUtils.isTrue(before)) {
					// webFragmentBefores
					webFragmentBefores.add(element);
				} else {
					// webFragmentAfters
					webFragmentAfters.add(element);
				}

			}
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return delegate.getInputStream();
	}

	@Override
	public boolean exists() {
		return delegate.exists();
	}

	@Override
	public boolean isReadable() {
		return delegate.isReadable();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public URL getURL() throws IOException {
		return delegate.getURL();
	}

	@Override
	public URI getURI() throws IOException {
		return delegate.getURI();
	}

	@Override
	public File getFile() throws IOException {
		return delegate.getFile();
	}

	@Override
	public long contentLength() throws IOException {
		return delegate.contentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return delegate.lastModified();
	}

	@Override
	public Resource createRelative(final String relativePath)
			throws IOException {
		return delegate.createRelative(relativePath);
	}

	@Override
	public String getFilename() {
		return delegate.getFilename();
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public WebFragmentOrderType getWebFragmentOrderType() {
		return webFragmentOrderType;
	}

	public String getFirstWebFragmentName() {
		if (allWebFragmentNames.isEmpty()) {
			return null;
		} else {
			return allWebFragmentNames.get(0);
		}
	}

	public List<String> getAllWebFragmentNames() {
		return allWebFragmentNames;
	}

	public List<String> getWebFragmentBefores() {
		return webFragmentBefores;
	}

	public List<String> getWebFragmentAfters() {
		return webFragmentAfters;
	}

	public List<String> getWebFragmentXmlComponents() {
		return webFragmentXmlComponents;
	}

}
