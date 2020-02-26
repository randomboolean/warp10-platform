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
import io.warp10.script.ext.inventory.InventoryWarpScriptExtension;
import io.warp10.script.ext.token.TokenWarpScriptExtension;
import org.junit.Ignore;
import org.junit.Test;
import io.warp10.standalone.Warp;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarpTest {

  private static String HOME = "/home/jenx/Softwares/warp10-local/";

  //@Ignore
  @Test
  public void testWarp() throws Exception {

    //
    // Config
    //

    String default_conf_folder = HOME + "etc/conf.d";
    List<String> conf = Files.walk(Paths.get(default_conf_folder)).map(x -> x.toString()).filter(f -> f.endsWith(".conf")).collect(Collectors.toList());

    //
    // Additional or overwriting configurations
    //

    String extraConfPath = HOME + "etc/conf.d/99-extra.conf";
    FileWriter fr = new FileWriter(new File(extraConfPath));
    fr.write("warp.timeunits = us");
    fr.close();
    conf.add(extraConfPath);

    //
    // Loging
    //

    //System.setProperty("log4j.configuration", new File(HOME + "etc/log4j.properties").toURI().toString());
    //System.setProperty("sensision.server.port", "0");

    //
    // Load jars from lib folder (plugins and extensions)
    //

    List<File> jars = new ArrayList<File>();
    for (File f: new File(HOME + "lib").listFiles()) {
      jars.add(f);
    }

    URLClassLoader cl = (URLClassLoader) WarpScriptLib.class.getClassLoader();
    Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
    m.setAccessible(true);

    for (int i = 0; i < jars.size(); i++) {
      URL url = jars.get(i).toURL();
      m.invoke(cl, url);
      System.out.println("Loading " + url.toString());
    }

    //
    // Start Warp 10
    //

    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Warp.main(conf.toArray(new String[conf.size()]));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    t.start();

    //
    // Built in extensions
    //

    WarpScriptLib.register(new InventoryWarpScriptExtension());
    //WarpScriptLib.register(new TokenWarpScriptExtension(Warp.getKeyStore())); null exception, must be loaded with config file

    //
    // Other extensions (add them as separate modules and put main as dep, then put these ext as dep of test)
    //

    //WarpScriptLib.register(new ArrowExtension());
    //WarpScriptLib.register(new ForecastExtension());

    //
    // Let Warp10 run
    //

    t.join();
  }

}