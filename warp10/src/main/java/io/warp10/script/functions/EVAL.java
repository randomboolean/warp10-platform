//
//   Copyright 2018-2020  SenX S.A.S.
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

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStack.Macro;

public class EVAL extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public EVAL(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object o = stack.pop();
    
    if (o instanceof String) {
      // Execute single statement which may span multiple lines
      stack.execMulti((String) o);
    } else if (o instanceof Macro) {
      stack.exec((Macro) o);
    } else if (o instanceof WarpScriptStackFunction) {
      ((WarpScriptStackFunction) o).apply(stack);
    } else {
      throw new WarpScriptException(getName() + " expects a Macro, a function of a String.");
    }
    return stack;
  }
}
