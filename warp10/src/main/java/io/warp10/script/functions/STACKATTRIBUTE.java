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

package io.warp10.script.functions;

import java.util.concurrent.atomic.AtomicReference;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

/**
 * Extract a stack attribute and push them on top of the stack, unless it is an AtomicReference
 * in which case the actual attribute will not be exposed
 */
public class STACKATTRIBUTE extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  public STACKATTRIBUTE(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {

    Object key = stack.pop();

    if (!(key instanceof String)) {
      throw new WarpScriptException(getName() + " expects a string as the attribute key.");
    }

    Object attr = stack.getAttribute(key.toString());

    if (attr instanceof AtomicReference) {
      stack.push(null);
    } else {
      stack.push(attr);
    }

    return stack;
  }
}
