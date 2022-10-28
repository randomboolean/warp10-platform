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

package io.warp10.script;


import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.continuum.gts.Slicer;

import java.util.Arrays;

/**
 * Function which takes as input multiple measurements, possibly
 * from different GTS instances and produces a single one.
 * 
 * This is the base interface for Mappers, Reducers, Bucketizers and Binary Ops
 *
 */
public interface WarpScriptAggregatorFunction extends WarpScriptAggregator {
  public Object apply(Object[] args) throws WarpScriptException;

  @Override
  default Object apply(Slicer slicer) {
    Object[] args = new Object[8];

    args[0] = slicer.getReferenceTick();
    args[1] = slicer.getNames().toArray();
    args[2] = slicer.getLabels().toArray();
    args[3] = slicer.getTicks().toArray();
    if (null != slicer.getLocations()) {
      args[4] = slicer.getLocations().toArray();
    } else {
      args[4] = new long[slicer.getSize()];
      Arrays.fill((long[]) args[4], GeoTimeSerie.NO_LOCATION);
    }
    if (null != slicer.getElevations()) {
      args[5] = slicer.getElevations().toArray();
    } else {
      args[5] = new long[slicer.getSize()];
      Arrays.fill((long[]) args[5], GeoTimeSerie.NO_ELEVATION);
    }
    args[6] = slicer.getValues().toArray();
    args[7] = slicer.getAdditionalArgs().toArray();

    return args;
  }
}
