// Copyright 2019 SenX
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package io.warp10.script.functions.arrow;

import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.formatted.FormattedWarpScriptFunction;

import java.util.Map;

/**
 * Decode an object from an Arrow stream
 */
public class ARROWTO extends FormattedWarpScriptFunction {

  private final Arguments args;
  private static final String BYTES = "bytes";

  public Arguments getArguments() {
    return args;
  }

  public ARROWTO(String name) {
    super(name);

    args = new ArgumentsBuilder()
      .addArgument(byte[].class, BYTES, "Arrow stream to be decoded." )
      .build();

  }

  public WarpScriptStack apply(Map<String, Object> params, WarpScriptStack stack) throws WarpScriptException {

    // TODO

    return stack;
  }
}
