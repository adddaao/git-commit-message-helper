package com.fulinlin.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author shuzijun
 */
public class PropertiesUtils {

    private final static String baseName = "i18n/info";
    private final static ResourceBundle rb1 = ResourceBundle.getBundle(baseName);

    public static String getInfo(String key, String... params) {
        return new MessageFormat(rb1.getString(key)).format(params);

    }


}