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

import io.warp10.arrow.ArrowExtension;
import io.warp10.script.WarpScriptLib;
import io.warp10.script.ext.inventory.InventoryWarpScriptExtension;
import org.junit.Test;
import io.warp10.standalone.Warp;

public class WarpTest {

  //change with your absolute path.
  private static String CONF = "/home/jc/Projects/2019/warp10-platform/conf-standalone.conf";

  //@Ignore
  @Test
  public void testWarp() throws Exception {

    //
    // Loging
    //

    //System.setProperty("log4j.configuration", new File("/home/jc/Projects/2019/warp10-platform/etc/log4j.properties").toURI().toString());
    //System.setProperty("sensision.server.port", "0");

    System.out.println("conf file:");
    System.out.println(CONF);

    //
    // Start Warp 10
    //

    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Warp.main(new String[]{CONF});
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    t.start();

    //
    // Extensions
    //

    WarpScriptLib.register(new ArrowExtension());
    WarpScriptLib.register(new InventoryWarpScriptExtension());

    //
    // Let Warp10 run
    //

    t.join();
  }

}