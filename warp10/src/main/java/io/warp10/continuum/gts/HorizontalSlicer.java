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

package io.warp10.continuum.gts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HorizontalSlicer implements Slicer {

  private final GeoTimeSerie gts;
  private final int virtualSize;
  private final int startIdx;
  private final long referenceTick;
  private final Object[] additionalArgs;

  private List ticks, locations, elevations, values;

  public HorizontalSlicer(GeoTimeSerie gts, int startIdx, int virtualSize, long referenceTick) {
    this.gts = gts;
    this.startIdx = startIdx;
    this.virtualSize = virtualSize;
    this.referenceTick = referenceTick;
    this.additionalArgs = new Object[0];
  }

  public HorizontalSlicer(GeoTimeSerie gts, int startIdx, int virtualSize, long referenceTick, Object[] additionalArgs) {
    this.gts = gts;
    this.startIdx = startIdx;
    this.virtualSize = virtualSize;
    this.referenceTick = referenceTick;
    this.additionalArgs = additionalArgs;
  }

  public GeoTimeSerie getGTS() {
    return gts;
  }

  public int getStartIdx() {
    return startIdx;
  }

  @Override
  public int getSize() {
    return virtualSize;
  }

  @Override
  public long getReferenceTick() {
    return referenceTick;
  }

  @Override
  public List getNames() {
    List names = new ArrayList(1);
    names.add(gts.getName());
    return names;
  }

  @Override
  public List getLabels() {
    List labels = new ArrayList(1);
    labels.add(new HashMap<String, String>(gts.getLabels()));
    return labels;
  }

  @Override
  public List getTicks() {
    if (null == ticks) {
      ticks = new COWList(gts.ticks, startIdx, virtualSize);
    }
    return ticks;
  }

  @Override
  public List getLocations() {
    if (null == locations) {
      if (gts.hasLocations()) {
        locations = new COWList(gts.locations, startIdx, virtualSize);
      }
    }
    return locations;
  }

  @Override
  public List getElevations() {
    if (null == elevations) {
      if (gts.hasElevations()) {
        elevations = new COWList(gts.elevations, startIdx, virtualSize);
      }
    }
    return elevations;
  }

  @Override
  public List getValues() {
    if (null == values) {
      switch (gts.type) {
        case LONG:
          values = new COWList(gts.longValues, startIdx, virtualSize);
          break;
        case DOUBLE:
          values = new COWList(gts.doubleValues, startIdx, virtualSize);
          break;
        case STRING:
          values = new COWList(gts.stringValues, startIdx, virtualSize);
          break;
        case BOOLEAN:
          values = new COWList(gts.booleanValues, startIdx, virtualSize);
          break;
      }
    }
    return values;
  }

  @Override
  public List getAdditionalArgs() {
    return Arrays.asList(additionalArgs);
  }
}
