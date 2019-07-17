//
//   Copyright 2018  SenX S.A.S.
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

import io.warp10.continuum.gts.GTSDecoder;
import io.warp10.continuum.gts.GTSEncoder;
import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.GTSStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extract the values from the parameter GTS instances and push them onto the stack.
 * 
 * Only the ticks with actual values are returned
 */
public class VALUES extends GTSStackFunction {
  
  public VALUES(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    if (!(stack.peek() instanceof GTSEncoder)) {
      return super.apply(stack);
    }

    GTSEncoder encoder = (GTSEncoder) stack.pop();
    
    int n = (int) encoder.getCount();

    List<Object> values = new ArrayList<Object>(n);
    
    GTSDecoder decoder = encoder.getDecoder(true);
    
    while(decoder.next()) {
      values.add(decoder.getBinaryValue());
    }
    
    stack.push(values);
    
    return stack;
  }
  
  
  @Override
  protected Map<String, Object> retrieveParameters(WarpScriptStack stack) throws WarpScriptException {
    return null;
  }

  @Override
  protected Object gtsOp(Map<String, Object> params, GeoTimeSerie gts) throws WarpScriptException {
    int nvalues = GTSHelper.nvalues(gts);

    List<Object> values = new ArrayList<Object>(nvalues);

    for (int i = 0; i < nvalues; i++) {
      values.add(GTSHelper.valueAtIndex(gts, i));
    }
    return values;
  }
}
