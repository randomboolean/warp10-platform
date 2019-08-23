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

import com.geoxp.GeoXPLib;
import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.continuum.store.thrift.data.Metadata;
import io.warp10.script.WarpScriptException;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.util.Text;
import org.boon.json.JsonParser;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrowAdapterHelper {


  final static String TIMESTAMPS_KEY = "timestamps";
  final static String LONG_VALUES_KEY = "long_values";
  final static String DOUBLE_VALUES_KEY = "double_values";
  final static String BOOLEAN_VALUES_KEY = "boolean_values";
  final static String STRING_VALUES_KEY = "string_values";
  final static String LATITUDE_KEY = "latitudes";
  final static String LONGITUDE_KEY = "longitudes";
  final static String ELEVATION_KEY = "elevations";

  final static String BUCKETSPAN = "bucketspan";
  final static String BUCKETCOUNT = "bucketcount";
  final static String LASTBUCKET = "lastbucket";

  //
  // Fields of arrow schemas
  // Except for timestamp field, we make them nullable so we can use them for GTSEncoders
  //

  private static Field nonNullable(String key, ArrowType type) {
    return new Field(key, new FieldType(false, type, null), null);
  }

  final static Field TIMESTAMP_FIELD = nonNullable(TIMESTAMPS_KEY, new ArrowType.Int(64, true));
  final static Field LONG_VALUES_FIELD = Field.nullable(LONG_VALUES_KEY,new ArrowType.Int(64, true));
  final static Field DOUBLE_VALUES_FIELD = Field.nullable(DOUBLE_VALUES_KEY, new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE));
  final static Field BOOLEAN_VALUES_FIELD = Field.nullable(BOOLEAN_VALUES_KEY, new ArrowType.Bool());
  final static Field STRING_VALUES_FIELD = Field.nullable(STRING_VALUES_KEY, new ArrowType.Binary());
  final static Field LATITUDE_FIELD = Field.nullable(LATITUDE_KEY, new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE));
  final static Field LONGITUDE_FIELD = Field.nullable(LONGITUDE_KEY, new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE));
  final static Field ELEVATION_FIELD = Field.nullable(ELEVATION_KEY, new ArrowType.Int(64, true));

  //
  // Json converters for labels and attributes
  //

  final static JsonSerializer serializer = new JsonSerializerFactory().create();
  private static String toJson(Object o) {
    return serializer.serialize(o).toString();
  }

  final static JsonParser parser = new JsonParserFactory().create();
  private static Object jsonTo(String s) {
    return parser.parse(s);
  }

  /**
   * Creates an Arrow schema fitted to a GTS
   * @param gts
   * @return
   */
  public static Schema createArrowSchema(GeoTimeSerie gts) throws WarpScriptException {

    List<Field> fields = new ArrayList<>();
    Map<String, String> metadata = new HashMap<>();

    //
    // Feed schema's metadata
    //

    Metadata gtsMeta = gts.getMetadata();

    if (gtsMeta.isSetName()) {
      metadata.put(Metadata._Fields.NAME.getFieldName(), gtsMeta.getName());
    }

    if (gtsMeta.isSetLabels() && gtsMeta.getLabels().size() > 0) {
      metadata.put(Metadata._Fields.LABELS.getFieldName(), toJson(gtsMeta.getLabels()));
    }

    if(gtsMeta.isSetClassId()) {
      metadata.put(Metadata._Fields.CLASS_ID.getFieldName(), String.valueOf(gtsMeta.getClassId()));
    }

    if(gtsMeta.isSetLabelsId()) {
      metadata.put(Metadata._Fields.LABELS_ID.getFieldName(), String.valueOf(gtsMeta.getLabelsId()));
    }

    if (gtsMeta.isSetAttributes() && gtsMeta.getAttributes().size() > 0) {
      metadata.put(Metadata._Fields.ATTRIBUTES.getFieldName(), toJson(gtsMeta.getAttributes()));
    }

    if (gtsMeta.isSetSource()) {
      metadata.put(Metadata._Fields.SOURCE.getFieldName(), gtsMeta.getSource());
    }

    if(gtsMeta.isSetLastActivity()) {
      metadata.put(Metadata._Fields.LAST_ACTIVITY.getFieldName(), String.valueOf(gtsMeta.getLastActivity()));
    }

    //
    // Bucketize info
    //

    if (GTSHelper.isBucketized(gts)) {
      metadata.put(BUCKETSPAN, String.valueOf(GTSHelper.getBucketSpan(gts)));
      metadata.put(BUCKETCOUNT, String.valueOf(GTSHelper.getBucketCount(gts)));
      metadata.put(LASTBUCKET, String.valueOf(GTSHelper.getLastBucket(gts)));
    }

    //
    // Feed schema's fields
    //
    
    if (0 == gts.size()) {
      return new Schema(fields, metadata);
    }

    fields.add(TIMESTAMP_FIELD);
    
    if (gts.hasLocations()) {
      fields.add(LATITUDE_FIELD);
      fields.add(LONGITUDE_FIELD);
    }
    
    if (gts.hasElevations()) {
      fields.add(ELEVATION_FIELD);
    }

    GeoTimeSerie.TYPE type = gts.getType();
    switch(type) {
      case LONG: fields.add(LONG_VALUES_FIELD);
      break;

      case DOUBLE: fields.add(DOUBLE_VALUES_FIELD);
      break;

      case BOOLEAN: fields.add(BOOLEAN_VALUES_FIELD);
      break;

      case STRING: fields.add(STRING_VALUES_FIELD);
      break;

      case UNDEFINED: throw new WarpScriptException("Cannot create an Arrow schema for a GTS with data of undefined type.");
    }

    return new Schema(fields, metadata);
  }

  /**
   * Convert a GTS to an arrow stream
   */
  public static byte[] toArrowStream(GeoTimeSerie gts, int nTicksPerBatch) throws WarpScriptException {

    VectorSchemaRoot root = VectorSchemaRoot.create(createArrowSchema(gts), new RootAllocator(Integer.MAX_VALUE));

    //
    // Feed data to root
    //

    OutputStream out = new ByteArrayOutputStream();

    try (ArrowStreamWriter writer =  new ArrowStreamWriter(root, null, out)) {

      writer.start();
      root.setRowCount(nTicksPerBatch);

      for (int i = 0; i < gts.size(); i++) {

        ((BigIntVector) root.getVector(TIMESTAMPS_KEY)).setSafe(i % nTicksPerBatch, GTSHelper.tickAtIndex(gts, i));

        if (gts.hasLocations()) {
          double[] latlon = GeoXPLib.fromGeoXPPoint(GTSHelper.locationAtIndex(gts, i));
          ((Float4Vector) root.getVector(LATITUDE_KEY)).setSafe(i % nTicksPerBatch, (float) latlon[0]);
          ((Float4Vector) root.getVector(LONGITUDE_KEY)).setSafe(i % nTicksPerBatch, (float) latlon[1]);
        }

        if (gts.hasElevations()) {
          ((BigIntVector) root.getVector(ELEVATION_KEY)).setSafe(i % nTicksPerBatch, GTSHelper.elevationAtIndex(gts, i));
        }

        GeoTimeSerie.TYPE type = gts.getType();
        switch(type) {
          case LONG: ((BigIntVector) root.getVector(LONG_VALUES_KEY)).setSafe(i % nTicksPerBatch, (long) GTSHelper.valueAtIndex(gts, i));
          break;

          case DOUBLE: ((Float8Vector) root.getVector(DOUBLE_VALUES_KEY)).setSafe(i % nTicksPerBatch, (double) GTSHelper.valueAtIndex(gts, i));
          break;

          case BOOLEAN: ((BitVector) root.getVector(BOOLEAN_VALUES_KEY)).setSafe(i % nTicksPerBatch, (boolean) GTSHelper.valueAtIndex(gts, i) ? 1 : 0);
          break;

          case STRING: ((VarCharVector) root.getVector(STRING_VALUES_KEY)).setSafe(i % nTicksPerBatch, new Text((String) GTSHelper.valueAtIndex(gts, i)));
          break;

          case UNDEFINED: throw new WarpScriptException("Cannot create an Arrow stream for a GTS with data of undefined type.");
        }

        if (i % nTicksPerBatch == nTicksPerBatch - 1) {
          writer.writeBatch();
        }
      }

      if ((gts.size() - 1) % nTicksPerBatch != nTicksPerBatch - 1 ) {
        root.setRowCount((gts.size() - 1) % nTicksPerBatch);
        writer.writeBatch();
      }

      writer.end();
    } catch (IOException e) {
      throw new WarpScriptException(e);
    }

    return ((ByteArrayOutputStream) out).toByteArray();
  }
}
