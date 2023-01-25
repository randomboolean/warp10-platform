package io.warp10;

import io.warp10.continuum.gts.GTSEncoder;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.crypto.OrderPreservingBase64;
import io.warp10.script.MemoryWarpScriptStack;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;

public class SomePRtests {

  @Test
  public void testOPB64() throws Exception {

    GTSEncoder encoder = new GTSEncoder();
    encoder.setName("test");
    encoder.addValue(0, GeoTimeSerie.NO_LOCATION, GeoTimeSerie.NO_ELEVATION, "first value");
    encoder.addValue(0, GeoTimeSerie.NO_LOCATION, GeoTimeSerie.NO_ELEVATION, 42);
    encoder.addValue(5000000, 123, GeoTimeSerie.NO_ELEVATION, "first value");
    encoder.addValue(15000000, GeoTimeSerie.NO_LOCATION, 1000, "first vahhlue");
    encoder.addValue(333333333, 5, 100, 456.456);
    for (int i = 0; i < 2000000; i++) {
      encoder.addValue(333333333, 5, 100, 456.456 + i);
    }

    StringWriter sw  = new StringWriter();

    byte[] encoded = encoder.getBytes();
    System.out.println(encoded.length);

    System.out.println("New encoding");
    long a = System.currentTimeMillis();
    OrderPreservingBase64.encodeToWriter(sw, encoded, 0, encoded.length, new byte[100000]);
    long b = System.currentTimeMillis();
    sw.write('\r');
    sw.write('\n');
    sw.flush();
    String s1 = sw.toString();

    StringWriter sw2 = new StringWriter();

    System.out.println("Old encoding");
    long c = System.currentTimeMillis();
    OrderPreservingBase64.encodeToWriter(encoder.getBytes(), sw2);
    long d = System.currentTimeMillis();
    sw2.write('\r');
    sw2.write('\n');
    sw2.flush();
    String s2 = sw2.toString();

    System.out.println("s1 length: " + s1.length());
    System.out.println("s2 length: " + s2.length());
    System.out.println("Equal ? " + s1.equals(s2));

    System.out.println("Elapsed b-a:");
    System.out.println(b - a);
    System.out.println("Elapsed d-c:");
    System.out.println(d - c);
    System.out.println("Elapsed ratio:");
    System.out.println((d - c) / (double) (b - a));
  }

  @Test
  public void parse0Xnumber() throws Exception {

    StringBuilder props = new StringBuilder();

    props.append("warp.timeunits=us");
    WarpConfig.safeSetProperties(new StringReader(props.toString()));

    MemoryWarpScriptStack stack = new MemoryWarpScriptStack(null, null);
    stack.maxLimits();

    String op = "010";
    Long l = Long.decode(op.toString().trim());

    System.out.println(l);

    /*TOLONG TOLONG = new TOLONG("TOLONG");
    stack.push("08");
    TOLONG.apply(stack);
    System.out.println(stack.pop());*/


  }
}
