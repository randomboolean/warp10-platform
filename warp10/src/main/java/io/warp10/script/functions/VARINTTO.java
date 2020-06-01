//
//   Copyright 2020  SenX S.A.S.
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.warp10.continuum.gts.Varint;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

/**
 * Converts a byte array containing varints into a list of LONGs
 */
public class VARINTTO extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public VARINTTO(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    Object top = stack.pop();
    
    long count = Long.MAX_VALUE;
    boolean countbased = false;
    if (top instanceof Long) {
      count = ((Long) top).longValue();
      countbased = true;
      top = stack.pop();
    }
    
    if (!(top instanceof byte[])) {
      throw new WarpScriptException(getName() + " operates on a byte array.");
    }
    
    byte[] data = (byte[]) top;

    ByteBuffer bb = ByteBuffer.wrap(data);
    
    List<Object> values = new ArrayList<Object>();

    try {
      while(bb.hasRemaining() && count > 0) {
        long value = Varint.decodeUnsignedLong(bb);
        values.add(value);
        count--;
      }
    } catch (IllegalArgumentException iae) {
      throw new WarpScriptException(getName() + " error while decoding values.", iae);
    }
    
    stack.push(values);
    
    //
    // If we were count based, output the number of bytes we consumed
    //
    if (countbased) {
      stack.push((long) (data.length - bb.remaining())); 
    }
    
    return stack;
  }
}
