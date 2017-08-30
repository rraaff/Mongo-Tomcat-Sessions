package com.dawsonsystems.session;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SensitiveConf {

	private Properties properties;
	
	private SensitiveConf() {
		
	}
	
	public static SensitiveConf load(String path) throws FileNotFoundException, IOException {
		SensitiveConf conf = new SensitiveConf();
		conf.setProperties(loadProperties(path));
		return conf;
	}

	private static Properties loadProperties(String path) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		try (InputStream in = new FileInputStream(path)) {
			prop.load(in);
		}
        return prop;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String getProperty(String userNameKey) {
		return getProperties().getProperty(userNameKey);
	}
	
	
}
