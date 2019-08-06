// Copyright 2018 SenX
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package io.warp10;

import io.warp10.script.WarpScriptLib;
import ml.MachineLearningPackage;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Ignore;
import org.junit.Test;
import io.warp10.standalone.Warp;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WarpTest {

  //change with your absolute path.
  private static String PROJECT_FOLDER = "/home/jc/Projects/2019/";
  private static String WARP10_HOME = PROJECT_FOLDER + "warp10-platform/warp10/archive/warp10-2.1.0-106-g2d446747";
  private static String PY4J_JAR = PROJECT_FOLDER + "warp10-plugin-py4j/build/libs/warp10-plugin-py4j-41a0a63.jar";
  private static String TF_EXT_JAR = PROJECT_FOLDER + "warp10-ext-tensorflow/build/libs/warp10-ext-tensorflow-037c800.jar";

  //@Ignore
  @Test
  public void testWarp() throws Exception {

    //
    // Loging
    //

    System.setProperty("log4j.configuration", new File(WARP10_HOME + "/etc/log4j.properties").toURI().toString());
    System.setProperty("sensision.server.port", "0");

    //
    // Register project extensions
    //

    //WarpScriptLib.register(new MachineLearningPackage());

    //
    // Load jar extensions (needs to be specified in conf file)
    //

    List<File> jars = new ArrayList<File>();
    //jars.addAll(Arrays.asList((new File(WARP10_HOME + "/lib")).listFiles((FileFilter) new WildcardFileFilter("*.jar"))));
    jars.add(new File(PY4J_JAR));
    jars.add(new File(TF_EXT_JAR));

    URLClassLoader cl = (URLClassLoader) WarpScriptLib.class.getClassLoader();
    Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
    m.setAccessible(true);

    for (int i = 0; i < jars.size(); i++) {
      URL url = jars.get(i).toURL();
      m.invoke(cl, url);
      System.out.println("Loading " + url.toString());
    }

    // start Warp 10
    Warp.main(new String[]{PROJECT_FOLDER + "warp10-platform/conf-standalone.conf"});
  }
}