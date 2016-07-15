package com.freeheap.akkawl.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigLoader {

    public static Properties loadConfig(String filename) throws IOException {
        Properties base = loadConfigFromFile(filename);
        String runMode = System.getenv("RUNMODE");
        if (runMode != null) {
            File file = new File(filename);
            String dirname = file.getParent();
            if (dirname == null) {
                dirname = "";
            } else {
                dirname = dirname + "/";
            }
            String basename = file.getName();
            try {
                Properties extra = loadConfigFromFile(dirname + "private/" + runMode + "/" + basename);
                for (String name : extra.stringPropertyNames()) {
                    base.setProperty(name, extra.getProperty(name));
                }
            } catch (IOException ex) {
                // Do nothing
            }
        }
        return base;
    }

    /**
     * Load configure file and remove empty key-property pairs
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static Properties loadConfigWithoutEmptyVals(String filename) throws IOException {
        Properties props = loadConfig(filename);
        // remove empty key that it's value is empty
        List<String> keyOfEmptyValues = new ArrayList<>();
        for (String s : props.stringPropertyNames()) {
            if (props.getProperty(s).isEmpty()) {
                keyOfEmptyValues.add(s);
            }
        }
        for (String s : keyOfEmptyValues) {
            props.remove(s);
        }
        return props;
    }

    private static Properties loadConfigFromFile(String filename) throws IOException {
        Properties prop = new Properties();
        File file = new File(filename);
        if (file.exists()) {
            prop.load(new FileReader(file));
        } else {
            InputStream stream = ConfigLoader.class.getClassLoader().getResourceAsStream(filename);
            if (stream != null) {
                prop.load(stream);
            }
        }
        return prop;
    }

}
