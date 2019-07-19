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
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Ignore;
import org.junit.Test;
import io.warp10.standalone.Warp;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class WarpTest {

  //change with your absolute path.
  private static String WARP10_HOME = "/home/jc/Projects/2019/warp10-platform/warp10/archive/warp10-2.0.2-116-gcd20745a";

  //@Ignore
  @Test
  public void testWarp() throws Exception {

    // loging
    System.setProperty("log4j.configuration", new File(WARP10_HOME + "/etc/log4j.properties").toURI().toString());
    System.setProperty("sensision.server.port", "0");

    // load extensions
    File[] jars = (new File(WARP10_HOME + "/lib")).listFiles((FileFilter) new WildcardFileFilter("*.jar"));
    URLClassLoader cl = (URLClassLoader) WarpScriptLib.class.getClassLoader();
    Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
    m.setAccessible(true);

    for (int i = 0; i < jars.length; i++) {
      URL url = jars[i].toURL();
      m.invoke(cl, url);
      System.out.println("Loading " + url.toString());
    }

    // start Warp 10
    Warp.main(new String[]{WARP10_HOME + "/etc/conf-standalone.conf"});
  }
}