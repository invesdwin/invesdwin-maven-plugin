package de.invesdwin.maven.plugin.shade.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

public class TransformerResource implements Resource {

	private String resource;
	private final String content;

	public TransformerResource(String resource, InputStream inputStream) throws IOException {
		this.resource = resource;
		this.content = IOUtils.toString(inputStream);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(content.getBytes());
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI getURI() throws IOException {
		try {
			return new URI(resource);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public File getFile() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long contentLength() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long lastModified() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFilename() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDescription() {
		throw new UnsupportedOperationException();
	}

}
