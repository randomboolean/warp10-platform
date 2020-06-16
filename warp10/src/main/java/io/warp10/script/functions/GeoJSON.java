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

package io.warp10.script.functions;

import com.geoxp.GeoXPLib;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

import java.util.Map;

import org.wololo.jts2geojson.GeoJSONReader;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

/**
 * Converts a Geo JSON Text String into a GeoXP Shape suitable for geo filtering
 */
public class GeoJSON extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  private final boolean uniform;

  public GeoJSON(String name, boolean uniform) {
    super(name);
    this.uniform = uniform;
  }

  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object inside = stack.pop();
    Object pcterror = stack.pop();
    Object geoJson = stack.pop();

    if (!(geoJson instanceof String) || !(inside instanceof Boolean) || (!(pcterror instanceof Double) && !(pcterror instanceof Long))) {
      throw new WarpScriptException(getName() + " expects a GeoJSON string, an error percentage or resolution (even number between 2 and 30) and a boolean as the top 3 elements of the stack.");
    }

    // Check the resolution is even and in 2..30, if relevant
    if (pcterror instanceof Long) {
      long res = ((Number) pcterror).longValue();
      if (1 == (res % 2) || res > 30 || res < 2) {
        throw new WarpScriptException(getName() + " expects the resolution to be an even number between 2 and 30");
      }
    }

    //
    // Read Geo JSON
    //

    GeoJSONReader reader = new GeoJSONReader();
    Geometry geometry = null;

    try {
      geometry = reader.read((String) geoJson);
    } catch (UnsupportedOperationException uoe) {
      throw new WarpScriptException(uoe);
    }

    //
    // Apply buffer if defined
    //
    Map<Object,Object> buffer = (Map<Object,Object>) stack.getAttribute(GEOBUFFER.ATTR_GEOBUFFER);
    
    if (null != buffer) {
      // Clear the buffer
      stack.setAttribute(GEOBUFFER.ATTR_GEOBUFFER, null);
      // Apply the buffer operation
      BufferOp bop = new BufferOp(geometry, (BufferParameters) buffer.get(GEOBUFFER.KEY_PARAMS));
      geometry = bop.getResultGeometry(((Double) buffer.get(GEOBUFFER.KEY_DIST)).doubleValue());
    }

    //
    // Convert Geometry to a GeoXPShape
    //

    int maxcells = ((Number) stack.getAttribute(WarpScriptStack.ATTRIBUTE_MAX_GEOCELLS)).intValue();
    Object shape = null;

    if (!this.uniform) {
      if (pcterror instanceof Double) {
        shape = GeoXPLib.toGeoXPShape(geometry, ((Number) pcterror).doubleValue(), Boolean.TRUE.equals(inside), maxcells);
      } else {
        shape = GeoXPLib.toGeoXPShape(geometry, ((Number) pcterror).intValue(), Boolean.TRUE.equals(inside), maxcells);
      }
    } else {
      if (pcterror instanceof Double) {
        shape = GeoXPLib.toUniformGeoXPShape(geometry, ((Number) pcterror).doubleValue(), Boolean.TRUE.equals(inside), maxcells);
      } else {
        shape = GeoXPLib.toUniformGeoXPShape(geometry, ((Number) pcterror).intValue(), Boolean.TRUE.equals(inside), maxcells);
      }
    }

    if (null == shape) {
      throw new WarpScriptException("Maximum number of cells exceeded in a geographic shape (warpscript.maxgeocells=" + maxcells + ")");
    }

    stack.push(shape);
    return stack;
  }
}
