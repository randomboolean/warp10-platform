//
//   Copyright 2018-2020  SenX S.A.S.
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

package io.warp10.script.aggregator;

import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.continuum.store.Constants;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptBucketizerFunction;
import io.warp10.script.WarpScriptMapperFunction;
import io.warp10.script.WarpScriptReducerFunction;
import io.warp10.script.WarpScriptException;

/**
 * Compute the vertical speed from oldest tick to most recent, in m/s.
 * 
 * If one of those two ticks does not have an elevation, returned value is null.
 * Returned location and elevation are those of the tick being computed.
 */
public class VSpeed extends NamedWarpScriptFunction implements WarpScriptMapperFunction, WarpScriptBucketizerFunction, WarpScriptReducerFunction {
  
  public VSpeed(String name) {
    super(name);
  }
  
  @Override
  public Object apply(Object[] args) throws WarpScriptException {
    long tick = (long) args[0];
    long[] ticks = (long[]) args[3];
    long[] locations = (long[]) args[4];
    long[] elevations = (long[]) args[5];
    
    if (0 == ticks.length) {
      return new Object[] { Long.MAX_VALUE, GeoTimeSerie.NO_LOCATION, GeoTimeSerie.NO_ELEVATION, null };
    }

    int[] firstlast = GTSHelper.getFirstLastTicks(ticks);
    int firsttick = firstlast[0];
    int lasttick = firstlast[1];
    
    //
    // Determine location/elevation at 'tick' if known
    //
    
    long location = GeoTimeSerie.NO_LOCATION;
    long elevation = GeoTimeSerie.NO_ELEVATION;
    
    for (int i = 0; i < ticks.length; i++) {
      if (tick == ticks[i]) {
        location = locations[i];
        elevation = elevations[i];
        break;
      }
    }
    
    //
    // One of the ticks does not have an elevation
    //
    
    if (GeoTimeSerie.NO_ELEVATION == elevations[firsttick]
        || GeoTimeSerie.NO_ELEVATION == elevations[lasttick]) {
      return new Object[] { tick, location, elevation, null };
    }
    
    double vspeed = elevations[lasttick] - elevations[firsttick];
    
    if (0.0D == vspeed) {
      return new Object[] { tick, location, elevation, 0.0D };
    } else {
      return new Object[] { tick, location, elevation, vspeed / ((double) Constants.ELEVATION_UNITS_PER_M) / ((ticks[lasttick] - ticks[firsttick]) / ((double) Constants.TIME_UNITS_PER_S)) };
    }
  }
}
