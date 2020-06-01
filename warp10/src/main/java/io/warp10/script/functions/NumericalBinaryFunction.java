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

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * Apply a double or long binary operator to two values.
 * If only the long operator is defined, all numbers are converted to long.
 * If only the double operator is defined, all numbers are converted to double.
 * If both long and double operators are defined, all numbers are converted to long until a Double or BigDecimal is found,
 * then they are all converted to double.
 *
 * The operator can also be applied to a list of values and a single operand, on top.
 * The result is a copy of the list with values being the result of the operator applied on the initial value and the single operand.
 *
 * If the function is given a list on top, its behavior depends on applyOnSingleList:
 * - if true the result is op(...op(op(op(v[0], v[1]), v[2]), v[3]), ... v[n])
 * - if false, the function expects a single value under the list and behave the same as described in the paragraph before, operands being switched.
 *
 * Whether applyOnSingleList should be set to true of false depends on the commutativity of the operator:
 * - max, sum, multiplication, for instance, do have meaning for a single list. Moreover, being commutative,
 *   the single operand can be put on top to apply the operator on a list and a single operand.
 * - copysign, power, nextafter, for instance, are not really useful when applied on a single list.
 */
public class NumericalBinaryFunction extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  final LongBinaryOperator opL;
  final DoubleBinaryOperator opD;
  final boolean applyInList;

  public NumericalBinaryFunction(String name, LongBinaryOperator longBinOp, DoubleBinaryOperator doubleBinOp, boolean applyOnSingleList) {
    super(name);
    opL = longBinOp;
    opD = doubleBinOp;
    applyInList = applyOnSingleList;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object op0 = stack.pop();

    if (op0 instanceof Number) {
      Object op1 = stack.pop();

      if (op1 instanceof Number) {
        // Simple case of 2 numeric operands.
        if (null != opD && (null == opL || op0 instanceof Double || op1 instanceof Double || op0 instanceof BigDecimal || op1 instanceof BigDecimal)) {
          stack.push(opD.applyAsDouble(((Number) op1).doubleValue(), ((Number) op0).doubleValue()));
        } else {
          stack.push(opL.applyAsLong(((Number) op1).longValue(), ((Number) op0).longValue()));
        }
      } else if (op1 instanceof List) {
        // A numeric operand on top and a list under: apply the operator on each value of the list and the single operand.
        List list = (List) op1;
        ArrayList<Object> result = new ArrayList<Object>(list.size());
        for (Object element: list) {
          if (!(element instanceof Number)) {
            throw new WarpScriptException(getName() + " can only operate on 2 numerical values or a list of numerical values.");
          }

          if (null != opD && (null == opL || op0 instanceof Double || element instanceof Double || op0 instanceof BigDecimal || element instanceof BigDecimal)) {
            result.add(opD.applyAsDouble(((Number) element).doubleValue(), ((Number) op0).doubleValue()));
          } else {
            result.add(opL.applyAsLong(((Number) element).longValue(), ((Number) op0).longValue()));
          }
        }
        stack.push(result);
      } else {
        throw new WarpScriptException(getName() + " can only operate on 2 numerical values or a list of numerical values.");
      }
    } else if (op0 instanceof List) {
      // A list on top, whether the function expects a single operand under or not depends on applyInList.
      if (applyInList) {
        // Apply operator only on the elements of the list op(...op(op(op(v[0], v[1]), v[2]), v[3]), ... v[n])
        Number result = null;

        for (Object element: (List) op0) {
          if (!(element instanceof Number)) {
            throw new WarpScriptException(getName() + " can only operate on 2 numerical values or a list of numerical values.");
          }

          if (null == result) {
            if (null != opD && (null == opL || element instanceof Double || element instanceof BigDecimal)) {
              result = ((Number) element).doubleValue();
            } else {
              result = ((Number) element).longValue();
            }
          } else {
            if (null != opD && (null == opL || result instanceof Double || element instanceof Double || element instanceof BigDecimal)) {
              result = opD.applyAsDouble(result.doubleValue(), ((Number) element).doubleValue());
            } else {
              result = opL.applyAsLong(result.longValue(), ((Number) element).longValue());
            }
          }
        }

        stack.push(result);
      } else {
        // Expect a single operand under the list and apply the operator on the single operand and each value of the list.
        Object op1 = stack.pop();

        if (!(op1 instanceof Number)) {
          throw new WarpScriptException(getName() + " can only operate on 2 numerical values or a list of numerical values.");
        }

        List list = (List) op0;
        ArrayList<Object> result = new ArrayList<Object>(list.size());
        for (Object element: list) {
          if (!(element instanceof Number)) {
            throw new WarpScriptException(getName() + " can only operate on 2 numerical values or a list of numerical values.");
          }

          if (null != opD && (null == opL || op1 instanceof Double || element instanceof Double || op1 instanceof BigDecimal || element instanceof BigDecimal)) {
            result.add(opD.applyAsDouble(((Number) op1).doubleValue(), ((Number) element).doubleValue()));
          } else {
            result.add(opL.applyAsLong(((Number) op1).longValue(), ((Number) element).longValue()));
          }
        }
        stack.push(result);
      }
    } else {
      throw new WarpScriptException(getName() + " can only operate on 2 numerical values or a list of numerical values.");
    }

    return stack;
  }
}
