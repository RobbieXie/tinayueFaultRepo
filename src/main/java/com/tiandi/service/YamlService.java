package com.tiandi.service;

import com.tiandi.mongo.testcase.TestCase;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/29.
 */
public class YamlService {
    public static void dump(Representer repr, Object ob, String filename) throws IOException {
        Yaml yaml = new Yaml(repr);
        yaml.dump(ob, new FileWriter(filename));
    }

    public static Object load(Constructor constructor, String fileName) throws FileNotFoundException {
        Yaml yaml = new Yaml(constructor);
        InputStream input = new FileInputStream(new File(fileName));
        return yaml.load(input);
    }
}
