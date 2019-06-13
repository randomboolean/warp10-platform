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

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts a value from a map, list, or byte array given a key.
 */
public class GET extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public GET(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object key = stack.pop();
    
    Object coll = stack.pop();

    if (!(coll instanceof Map) && !(coll instanceof List) && !(coll instanceof byte[])) {
      throw new WarpScriptException(getName() + " operates on a map, list or byte array.");
    }
    
    Object value = null;
    
    if (coll instanceof Map) {
      value = ((Map) coll).get(key);

    } else if (key instanceof Number) {
      int idx = ((Number) key).intValue();

      if (coll instanceof List) {
        int size = ((List) coll).size();

        idx = computeAndCheckIndex(this, idx, size);

        value = ((List) coll).get(idx);

      } else {
        int size = ((byte[]) coll).length;

        idx = computeAndCheckIndex(this, idx, size);

        value = (long) (((byte[]) coll)[idx] & 0xFFL);
      }

    } else if (coll instanceof byte[]) {
      throw new WarpScriptException(getName() + " expects the key to be an long when operating on a byte array.");

    } else if (!(key instanceof List)) {
      throw new WarpScriptException(getName() + " expects the key to be an long or a List of long when operating on a List.");

    } else {
      for (Object o: (List) key) {
        if (!(o instanceof Number)) {
          throw new WarpScriptException(getName() + " expects the key to be an long or a List of long when operating on a List.");
        }
      }

      value = recNestedGet((List) coll, (List<Long>) key);
    }
    
    stack.push(value);

    return stack;
  }

  public static int computeAndCheckIndex(NamedWarpScriptFunction func, int index, int size) throws WarpScriptException {
    if (index < 0) {
      index += size;
    } else if (index >= size) {
      throw new WarpScriptException(func.getName() + " index out of bound, " + index + " >= " + size);
    }
    if (index < 0) {
      throw new WarpScriptException(func.getName() + " index out of bound, " + (index - size) + " < -" + size);
    }

    return index;
  }

  private Object recNestedGet(List nestedList, List<Long> indexList) throws WarpScriptException {
    List<Long> copyIndices = new ArrayList<>(indexList);

    int idx = computeAndCheckIndex(this, copyIndices.remove(0).intValue(), nestedList.size());

    if (0 == copyIndices.size()) {
      return nestedList.get(idx);

    } else {
      if (nestedList.get(idx) instanceof List) {
        return recNestedGet((List) nestedList.get(idx), copyIndices);

      } else {
        throw new WarpScriptException(getName() + " tried to get an element at a nested path that does not exist in the input list.");
      }
    }
  }
}
