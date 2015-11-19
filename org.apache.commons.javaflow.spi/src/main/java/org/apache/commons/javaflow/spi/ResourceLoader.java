package org.apache.commons.javaflow.spi;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceLoader {
	abstract public InputStream getResourceAsStream(String name) throws IOException; 
}
