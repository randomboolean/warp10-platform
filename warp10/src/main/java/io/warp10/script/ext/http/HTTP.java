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

package io.warp10.script.ext.http;

import io.warp10.WarpConfig;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WebAccessController;
import io.warp10.script.formatted.FormattedWarpScriptFunction;
import io.warp10.standalone.StandaloneWebCallService;
import io.warp10.warp.sdk.Capabilities;
import io.warp10.script.ext.http.HttpWarpScriptExtension;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Apply an HTTP method over an url
 */
public class HTTP extends FormattedWarpScriptFunction {


  private final Arguments args;
  private final Arguments output;

  protected Arguments getArguments() {
    return args;
  }
  protected Arguments getOutput() {
    return output;
  }

  private final ReentrantLock stackCountersLock = new ReentrantLock();
  private final WebAccessController webAccessController;

  public HTTP(String name) {
    super(name);

    String patternConf = WarpConfig.getProperty(HttpWarpScriptExtension.WARPSCRIPT_HTTP_HOST_PATTERNS);

    // If not defined, use already existing StandaloneWebCallService webAccessController which uses Configuration.WEBCALL_HOST_PATTERNS
    if (null == patternConf) {
      webAccessController = StandaloneWebCallService.getWebAccessController();
    } else {
      webAccessController = new WebAccessController(patternConf);
    }

    getDocstring().append("Apply an HTTP method over an url and fetch response.");

    args = new ArgumentsBuilder()
      //todo
      .build();

    output = new ArgumentsBuilder()
      //todo
      .build();
  }

  @Override
  public WarpScriptStack apply(Map<String, Object> formattedArgs, WarpScriptStack stack) throws WarpScriptException {

    if (stack.getAttribute(WarpScriptStack.CAPABILITIES_ATTR) instanceof Capabilities) {
      Capabilities capabilities = (Capabilities) stack.getAttribute(WarpScriptStack.CAPABILITIES_ATTR);
      if(!(capabilities.containsKey(HttpWarpScriptExtension.HTTP_CAPABILITY))) {
        throw new WarpScriptException("Capability " + HttpWarpScriptExtension.HTTP_CAPABILITY + " is required by function " + getName());
      }
    }

    Object o = stack.pop();

    Map<Object, Object> properties = new HashMap<Object, Object>();
    if (o instanceof Map) {
      properties = (Map<Object, Object>) o;
      o = stack.pop();
    }

    if (!(o instanceof String) && !(o instanceof List)) {
      throw new WarpScriptException(getName() + " expects a URL or list thereof on top of the stack.");
    }

    List<URL> urls = new ArrayList<URL>();

    try {
      if (o instanceof String) {
        urls.add(new URL(o.toString()));
      } else {
        for (Object oo: (List) o) {
          urls.add(new URL(oo.toString()));
        }
      }
    } catch (MalformedURLException mue) {
      throw new WarpScriptException(getName() + " encountered an invalid URL.", mue);
    }

    //
    // Check URLs
    //

    for (URL url: urls) {
      if (!webAccessController.checkURL(url)) {
        throw new WarpScriptException(getName() + " encountered a forbidden URL '" + url + "'");
      }
    }

    //
    // Check that we do not exceed the limits
    //

    // Get the current counters in the stack and initialize them if not present.
    AtomicLong urlCount;
    AtomicLong downloadSize;

    try {
      stackCountersLock.lockInterruptibly();

      Object ufCount = stack.getAttribute(HttpWarpScriptExtension.ATTRIBUTE_HTTP_COUNT);
      Object ufSize = stack.getAttribute(HttpWarpScriptExtension.ATTRIBUTE_HTTP_SIZE);

      if (null == ufCount || null == ufSize) {
        urlCount = new AtomicLong();
        downloadSize = new AtomicLong();
        stack.setAttribute(HttpWarpScriptExtension.ATTRIBUTE_HTTP_COUNT, urlCount);
        stack.setAttribute(HttpWarpScriptExtension.ATTRIBUTE_HTTP_SIZE, downloadSize);
      } else {
        urlCount = (AtomicLong) ufCount;
        downloadSize = (AtomicLong) ufSize;
      }
    } catch (InterruptedException ie) {
      throw new WarpScriptException(getName() + " thread has been interrupted", ie);
    } finally {
      if (stackCountersLock.isHeldByCurrentThread()) {
        stackCountersLock.unlock();
      }
    }

    if (urlCount.get() + urls.size() > (long) HttpWarpScriptExtension.getLongAttribute(stack, HttpWarpScriptExtension.ATTRIBUTE_HTTP_LIMIT)) {
      throw new WarpScriptException(getName() + " is limited to " + HttpWarpScriptExtension.getLongAttribute(stack, HttpWarpScriptExtension.ATTRIBUTE_HTTP_LIMIT) + " calls.");
    }

    List<Object> results = new ArrayList<Object>();

    for (URL url: urls) {
      // Recheck the count here in case of concurrent runs
      if (urlCount.addAndGet(1) > (long) HttpWarpScriptExtension.getLongAttribute(stack, HttpWarpScriptExtension.ATTRIBUTE_HTTP_LIMIT)) {
        throw new WarpScriptException(getName() + " is limited to " + HttpWarpScriptExtension.getLongAttribute(stack, HttpWarpScriptExtension.ATTRIBUTE_HTTP_LIMIT) + " calls.");
      }

      HttpURLConnection conn = null;

      try {
        conn = (HttpURLConnection) url.openConnection();

        if (null != url.getUserInfo()) {
          String basicAuth = "Basic " + new String(Base64.encodeBase64String(url.getUserInfo().getBytes(StandardCharsets.UTF_8)));
          properties.put("Authorization", basicAuth);
        }

        for (Map.Entry<Object, Object> prop: properties.entrySet()) {
          conn.setRequestProperty(String.valueOf(prop.getKey()), String.valueOf(prop.getValue()));
        }

        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setRequestMethod("GET");

        byte[] buf = new byte[8192];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream in = null;
        // When there is an error (response code is 404 for instance), body is in the error stream.
        try {
          in = conn.getInputStream();
        } catch (IOException ioe) {
          in = conn.getErrorStream();
        }

        while (true) {
          int len = in.read(buf);

          if (len < 0) {
            break;
          }

          if (downloadSize.get() + baos.size() + len > (long) HttpWarpScriptExtension.getLongAttribute(stack, HttpWarpScriptExtension.ATTRIBUTE_HTTP_MAXSIZE)) {
            throw new WarpScriptException(getName() + " would exceed maximum size of content which can be retrieved via this function (" + HttpWarpScriptExtension.getLongAttribute(stack, HttpWarpScriptExtension.ATTRIBUTE_HTTP_MAXSIZE) + " bytes)");
          }

          baos.write(buf, 0, len);
        }

        downloadSize.addAndGet(baos.size());

        List<Object> res = new ArrayList<Object>();

        res.add(conn.getResponseCode());
        Map<String, List<String>> hdrs = conn.getHeaderFields();

        if (hdrs.containsKey(null)) {
          List<String> statusMsg = hdrs.get(null);
          if (statusMsg.size() > 0) {
            res.add(statusMsg.get(0));
          } else {
            res.add("");
          }
        } else {
          res.add("");
        }

        //
        // Make the headers map modifiable
        //

        hdrs = new HashMap<String, List<String>>(hdrs);
        hdrs.remove(null);

        res.add(hdrs);
        res.add(Base64.encodeBase64String(baos.toByteArray()));

        results.add(res);
      } catch (IOException ioe) {
        throw new WarpScriptException(getName() + " encountered an error while fetching '" + url + "'", ioe);
      } finally {
        if (null != conn) {
          conn.disconnect();
        }
      }
    }

    stack.push(results);

    return stack;
  }
}