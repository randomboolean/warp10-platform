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

package io.warp10.script.processing.image;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.processing.ProcessingUtil;

import java.util.ArrayList;
import java.util.List;

import processing.core.PGraphics;
import processing.core.PImage;

/**
 * Call loadPixels then pixels
 */
public class Ppixels extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public Ppixels(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    if (stack.peek() instanceof PGraphics) {
      List<Object> params = ProcessingUtil.parseParams(stack, 0);
      
      PGraphics pg = (PGraphics) params.get(0);
      
      pg.parent.loadPixels();
      
      List<Long> pixels = new ArrayList<Long>(pg.pixels.length);
      
      for (int pixel: pg.pixels) {
        pixels.add((long) pixel);
      }
      
      stack.push(pg);
      stack.push(pixels);      
    } else if (stack.peek() instanceof PImage) {
      PImage pimage = (PImage) stack.pop();
      pimage.loadPixels();
      List<Long> pixels = new ArrayList<Long>(pimage.pixels.length);
      for (int pixel: pimage.pixels) {
        pixels.add((long) pixel);
      }
      stack.push(pixels);
    } else {
      throw new WarpScriptException(getName() + " can only be applied to PGraphics or PImage instances.");
    }
    
    return stack;
  }
}
