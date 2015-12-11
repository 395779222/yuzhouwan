package property;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Copyright @ 2015 yuzhouwan.com
 * All right reserved.
 * Function：annotation.property
 *
 * @author asdf2014
 * @since 2015/12/12 0011
 */
public class Log4jProperty {

    private final static Logger _log = Logger.getLogger(Log4jProperty.class);

    private final static String LOG4J_PROPERTIES_PATH_POSTFIX = "log4j.properties";

    public static void main(String[] args) throws MalformedURLException {

        URL url = Log4jProperty.class.getResource("/");
        _log.info(url);

        String path = url.toString().concat(LOG4J_PROPERTIES_PATH_POSTFIX);
        _log.info(path);

        /**
         * Auto
         */
//        PropertyConfigurator.configure(new URL(path));
        _log.info(PropertyConfigurator.LOGGER_FACTORY_KEY);
    }

}
