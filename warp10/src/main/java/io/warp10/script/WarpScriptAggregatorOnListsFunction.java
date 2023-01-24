//
//   Copyright 2022  SenX S.A.S.
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

package io.warp10.script;

/**
 * Function which takes as input multiple measurements, possibly
 * from different GTS instances and produces a single one.
 * 
 * This is the base interface for Mappers, Reducers, Bucketizers and Binary Ops
 *
 * WarpScriptAggregatorFunction apply() argument is an array of arrays.
 * WarpScriptAggregatorOnListsFunction applyOnSubLists() argument is similar except that data (ticks, locations, elevs, values) are either COWLists or null if empty.
 *
 */
public interface WarpScriptAggregatorOnListsFunction {
  public Object applyOnSubLists(Object[] subLists) throws WarpScriptException;
}
