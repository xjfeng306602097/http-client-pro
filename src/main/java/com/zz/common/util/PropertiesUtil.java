package com.zz.common.util;

import java.util.Properties;

public class PropertiesUtil {

	private static final String CONFIG_FIle = "httpdefaultconf.properties";

	private static Properties properties = null;

	static {
		properties = PropertiesLoader.loadProperties(CONFIG_FIle);
	}

	public static String getValue(String key) {
		return properties.getProperty(key);
	}

}
