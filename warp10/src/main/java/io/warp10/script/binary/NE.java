//
//   Copyright 2019  SenX S.A.S.
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

package io.warp10.script.binary;

import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

/**
 * Checks the two operands on top of the stack for inequality
 */
public class NE extends ComparisonOperation {

  public NE(String name) {
    super(name, true, false); // 0.0 NaN != must return true
  }

  @Override
  public boolean operator(int op1, int op2) {
    return op1 != op2;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object op2 = stack.pop();
    Object op1 = stack.pop();

    if ((op1 instanceof Number && op2 instanceof Number)
        || (op1 instanceof GeoTimeSerie && op2 instanceof GeoTimeSerie)
        || (op1 instanceof GeoTimeSerie && (op2 instanceof Number || op2 instanceof String))
        || (op2 instanceof GeoTimeSerie && (op1 instanceof Number || op1 instanceof String))) {
      // both numbers, both GTSs or one GTS and one String or Number
      comparison(stack, op1, op2);
    } else {
      stack.push(!op1.equals(op2));
    }

    return stack;
  }
}
