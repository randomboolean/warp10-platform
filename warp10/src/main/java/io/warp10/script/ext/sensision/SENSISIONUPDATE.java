//
//   Copyright 2018-2022  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.ext.sensision;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.sensision.Sensision;
import io.warp10.warp.sdk.Capabilities;

import java.util.List;
import java.util.Map;

/**
 * Updates a sensision metric
 */
public class SENSISIONUPDATE extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  public SENSISIONUPDATE(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {

    if (null == Capabilities.get(stack, SensisionWarpScriptExtension.WRITE_CAPABILITY)) {
      throw new WarpScriptException(getName() + " missing capability '" + SensisionWarpScriptExtension.WRITE_CAPABILITY +"'");
    }

    Object top = stack.pop();

    if (!(top instanceof List)) {
      throw new WarpScriptException(getName() + " expects a list on top of the stack.");
    }

    List<Object> args = (List<Object>) top;

    String cls = args.get(0).toString();
    Map<String,String> labels = (Map<String,String>) args.get(1);
    Number delta = (Number) args.get(2);

    Long ttl = null;

    if (args.size() > 3) {
      ttl = ((Number) args.get(3)).longValue();
    }

    if (null == ttl) {
      Sensision.update(cls, labels, delta);
    } else {
      Sensision.update(cls, labels, ttl, delta);
    }

    return stack;
  }

}
