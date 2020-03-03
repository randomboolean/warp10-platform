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

import io.warp10.continuum.gts.GTSEncoder;
import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.continuum.store.thrift.data.Metadata;
import io.warp10.script.ElementOrListStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

/**
 * For each encountered GTS, Encoder or list thereof, replace it with a selector which would select it
 */
public class TOSELECTOR extends ElementOrListStackFunction {

  private final ElementStackFunction toSelectorFunction = new ElementStackFunction() {
    @Override
    public Object applyOnElement(Object element) throws WarpScriptException {
      if (element instanceof GeoTimeSerie) {
        return GTSHelper.buildSelector((GeoTimeSerie) element, true);
      } else if (element instanceof GTSEncoder) {
        Metadata meta = new Metadata(((GTSEncoder) element).getMetadata());

        return GTSHelper.buildSelector(meta, true);
      } else {
        throw new WarpScriptException(getName() + " expects a GeoTimeSeries, a GTSEncoder or a list thereof on top of the stack.");
      }
    }
  };

  public TOSELECTOR(String name) {
    super(name);
  }

  @Override
  public ElementStackFunction generateFunction(WarpScriptStack stack) throws WarpScriptException {
    return toSelectorFunction;
  }
}
