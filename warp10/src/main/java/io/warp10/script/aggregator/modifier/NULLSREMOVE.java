//
//   Copyright 2023  SenX S.A.S.
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

package io.warp10.script.aggregator.modifier;

import io.warp10.continuum.gts.Aggregate;
import io.warp10.continuum.gts.COWAggregate;
import io.warp10.continuum.gts.COWTAggregate;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptLib;
import io.warp10.script.WarpScriptReducer;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.functions.SNAPSHOT;

import java.util.ArrayList;
import java.util.List;

public class NULLSREMOVE extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  public NULLSREMOVE(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object o = stack.pop();

    if (!(o instanceof WarpScriptReducer)) {
      throw new WarpScriptException(getName() + " expects a reducer");
    }

    stack.push(new RemoveNullsDecorator(getName(), (WarpScriptReducer) o));

    return stack;
  }

  private static final class RemoveNullsDecorator extends NamedWarpScriptFunction implements WarpScriptReducer, SNAPSHOT.Snapshotable {

    private final WarpScriptReducer aggregator;

    public RemoveNullsDecorator(String name, WarpScriptReducer aggregator) {
      super(name);
      this.aggregator = aggregator;
    }

    @Override
    public Object apply(Aggregate aggregate) throws WarpScriptException {
      return aggregator.apply(removeNulls(aggregate));
    }

    private Aggregate removeNulls(Aggregate aggregate) {
      if (null == aggregate.getValues()) {
        return aggregate;
      }

      // First case : COWList are backed by arrays of primitive objects that cannot contain any null value
      if (aggregate instanceof COWAggregate) {
        return aggregate;
      }

      // Second case : this is the usual case from REDUCE or APPLY framework
      if (aggregate instanceof COWTAggregate) {

        ((COWTAggregate) aggregate).removeNulls();
        return aggregate;
      }

      // Third case : this covers all remaining cases for full compatibility with the interface but should not happen for optimised aggregators
      List values = aggregate.getValues();
      List<Integer> skippedIndices = new ArrayList<Integer>(values.size() / 2);

      for (int i = 0; i < values.size(); i++) {
        if (null == values.get(i)) {
          skippedIndices.add(i);
        }
      }

      if (0 == skippedIndices.size()) {
        return aggregate;
      }

      List[] fields = aggregate.getLists();
      for (int i = 0; i < fields.length - 1; i++) {

        List field = fields[i];
        if (field.size() > 1 || i > 1) { // classnames and labels can be singletons so they are skipped in this case

          List newField = new ArrayList(field.size() - skippedIndices.size());

          for (int j = 0; j < field.size(); j++) {
            if (!skippedIndices.contains(j)) {
              newField.add(field.get(j));
            }
          }
        }
      }

      return aggregate;
    }

    @Override
    public String snapshot() {
      StringBuilder sb = new StringBuilder();
      sb.append(WarpScriptStack.MACRO_START);
      sb.append(" ");
      sb.append(aggregator.toString());
      sb.append(" ");
      sb.append(getName());
      sb.append(" ");
      sb.append(WarpScriptStack.MACRO_END);
      sb.append(" ");
      sb.append(WarpScriptLib.EVAL);
      return sb.toString();
    }
  }
}
