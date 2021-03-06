package com.brufino.terpsychore.util;

import com.brufino.terpsychore.lib.CodePointsIterable;
import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoreUtils {

    public static Iterable<Integer> codePoints(String string) {
        return new CodePointsIterable(string);
    }

    public static String getAsStringOrNull(JsonElement jsonElement) {
        return jsonElement.isJsonNull() ? null : jsonElement.getAsString();
    }

    public static JsonObject getAsJsonObjectOrNull(JsonElement jsonElement) {
        return jsonElement.isJsonNull() ? null : jsonElement.getAsJsonObject();
    }

    /* TODO: Search for places where we can use the methods below */

    public static List<JsonObject> jsonArrayToJsonObjectList(JsonArray jsonArray) {
        List<JsonObject> list = new ArrayList<>(jsonArray.size());
        for (JsonElement jsonElement : jsonArray) {
            list.add(jsonElement.getAsJsonObject());
        }
        return list;
    }

    public static JsonArray jsonObjectListToJsonArray(List<JsonObject> jsonObjectList) {
        JsonArray jsonArray = new JsonArray();
        for (JsonObject jsonObject : jsonObjectList) {
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    public static List<String> jsonArrayToStringList(JsonArray jsonArray) {
        List<String> list = new ArrayList<>(jsonArray.size());
        for (JsonElement jsonElement : jsonArray) {
            list.add(jsonElement.getAsString());
        }
        return list;
    }

    public static List<String> jsonArrayToNullableStringList(JsonArray jsonArray) {
        List<String> list = new ArrayList<>(jsonArray.size());
        for (JsonElement jsonElement : jsonArray) {
            list.add(getAsStringOrNull(jsonElement));
        }
        return list;
    }

    public static JsonArray stringListToJsonArray(List<String> stringList) {
        JsonArray jsonArray = new JsonArray();
        for (String string : stringList) {
            jsonArray.add(string);
        }
        return jsonArray;
    }

    public static JsonArray nullableStringListToJsonArray(List<String> stringList) {
        JsonArray jsonArray = new JsonArray();
        for (String string : stringList) {
            jsonArray.add(string != null ? new JsonPrimitive(string) : JsonNull.INSTANCE);
        }
        return jsonArray;
    }

    public static <T> JsonArray listToJsonArray(List<T> list) {
        JsonArray jsonArray = new JsonArray();
        for (T object : list) {
            jsonArray.add(toJsonElement(object));
        }
        return jsonArray;
    }

    public static <T> void mergeIntoJsonObject(JsonObject jsonObject, Map<String, T> map) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            String property = entry.getKey();
            T value = entry.getValue();
            jsonObject.add(property, toJsonElement(value));
        }
    }

    public static <T> JsonObject mapToJsonObject(Map<String, T> map) {
        JsonObject jsonObject = new JsonObject();
        mergeIntoJsonObject(jsonObject, map);
        return jsonObject;
    }

    private static <T> JsonElement toJsonElement(T object) {
        if (object instanceof JsonElement) {
            return (JsonElement) object;
        } else if (object instanceof Map) {
            //noinspection unchecked
            return mapToJsonObject((Map<String, ?>) object);
        } else if (object instanceof List) {
            return listToJsonArray((List<?>) object);
        } else if (object instanceof String) {
            return new JsonPrimitive((String) object);
        } else if (object instanceof Number) {
            return new JsonPrimitive((Number) object);
        } else if (object instanceof Boolean) {
            return new JsonPrimitive((Boolean) object);
        } else if (object instanceof Character) {
            return new JsonPrimitive((Character) object);
        }
        throw new IllegalArgumentException("Illegal value type for object");
    }

    // Prevents instantiation
    private CoreUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
