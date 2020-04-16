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

package io.warp10.script.mapper;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptAggregatorFunction;
import io.warp10.script.WarpScriptMapperFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.formatted.FormattedWarpScriptFunction;

import java.util.List;
import java.util.Map;

/**
 * Mapper which returns the absolute value of the value passed as parameter
 */
public class CHAINMAPPER extends FormattedWarpScriptFunction {

  public static final String LIST = "mappers";
  public static final String MAPPER = "mapper";

  private final Arguments args;
  private final Arguments output;
  protected Arguments getArguments() { return args; }

  protected Arguments getOutput() { return output; }

  public CHAINMAPPER(String name) {
    super(name);

    getDocstring().append("Return the shape of an input list if it could be a tensor (or multidimensional array), or raise an Exception.");

    args = new ArgumentsBuilder()
      .addListArgument(WarpScriptMapperFunction.class, LIST, "The input list of mappers to chain.")
      .build();

    output = new ArgumentsBuilder()
      .addListArgument(WarpScriptMapperFunction.class, MAPPER, "The composition mapper.")
      .build();
  }

  public abstract class CompositeMapper extends NamedWarpScriptFunction implements WarpScriptMapperFunction, WarpScriptAggregatorFunction {
    public CompositeMapper(String name) {
      super(name);
    }
  }

  @Override
  protected WarpScriptStack apply(Map<String, Object> formattedArgs, WarpScriptStack stack) throws WarpScriptException {

    List<NamedWarpScriptFunction> mappers = (List<NamedWarpScriptFunction>) formattedArgs.get(LIST);
    StringBuilder name = new StringBuilder();
    for (NamedWarpScriptFunction mapper: mappers) {
      if (0 != name.length()) {
        name.append(':');
      }
      name.append(mapper.getName());
    }

    WarpScriptMapperFunction mapper = new CompositeMapper(name.toString()) {
      
      @Override
      public Object apply(Object[] args) throws WarpScriptException {
        Object[] tmp = args;
        Object res = null;
        List<WarpScriptMapperFunction> mappers = (List<WarpScriptMapperFunction>) formattedArgs.get(LIST);

        for (WarpScriptMapperFunction mapper: mappers) {
          if (null != res) {
            if (res instanceof Map) {
              
              Map<String,Object[]> mapRes = (Map) res;
              tmp[1] = new String[mapRes.size()];
              tmp[3] = new Long[mapRes.size()];
              tmp[4] = new Long[mapRes.size()];
              tmp[5] = new Long[mapRes.size()];
              tmp[6] = new Object[mapRes.size()];

              int i = 0;
              for (Map.Entry<String,Object[]> entry : mapRes.entrySet()) {
                String key = entry.getKey();
                Object[] value = entry.getValue();

                ((String[]) tmp[1])[i] = key;
                ((Long[]) tmp[3])[i] = (long) value[0];
                ((Long[]) tmp[4])[i] = (long) value[1];
                ((Long[]) tmp[5])[i] = (long) value[2];
                ((Object[]) tmp[6])[i] = value[3];

                i++;
              }
              
            } else {

              Object[] singleRes;
              if (res instanceof List) {
                singleRes = ((List) res).toArray();
              } else {
                singleRes = (Object[]) res;
              }
              
              tmp[0] = singleRes[0];
              tmp[3] = new Long[]{(long) singleRes[0]};
              tmp[4] = new Long[]{(long) singleRes[1]};
              tmp[5] = new Long[]{(long) singleRes[2]};
              tmp[6] = new Object[]{singleRes[3]};
            }
          }

          res = mapper.apply(tmp);
        }

        return res;
      }
    };

    return stack;
  }
}
