package us.akcourts.querynotification;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Configuration {
	private static final String BUNDLE_NAME = "scheduler"; //$NON-NLS-1$
    private static ResourceBundle RESOURCE_BUNDLE = null;
	static {
		File file = new File("scheduler.properties");
		URL url = null;
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		URL[] urls = {url};
		ClassLoader loader = new URLClassLoader(urls);
//		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(), loader);
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(), loader);
    }


	
//	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Configuration() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
