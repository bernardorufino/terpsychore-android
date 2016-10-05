package com.brufino.terpsychore.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoreUtils {

    public static String getJsonAsStringOrNull(JsonElement jsonElement) {
        return jsonElement.isJsonNull() ? null : jsonElement.getAsString();
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

    public static JsonArray stringListToJsonArray(List<String> stringList) {
        JsonArray jsonArray = new JsonArray();
        for (String string : stringList) {
            jsonArray.add(string);
        }
        return jsonArray;
    }

    public static <T> void mergeIntoJsonObject(JsonObject jsonObject, Map<String, T> map) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            String property = entry.getKey();
            T value = entry.getValue();
            JsonElement element;
            if (value instanceof JsonElement) {
                element = (JsonElement) value;
            } else if (value instanceof Map) {
                //noinspection unchecked
                element = mapToJsonObject((Map<String, ?>) value);
            } else if (value instanceof String) {
                element = new JsonPrimitive((String) value);
            } else if (value instanceof Number) {
                element = new JsonPrimitive((Number) value);
            } else if (value instanceof Boolean) {
                element = new JsonPrimitive((Boolean) value);
            } else if (value instanceof Character) {
                element = new JsonPrimitive((Character) value);
            } else {
                throw new IllegalArgumentException("Illegal value type for map");
            }
            jsonObject.add(property, element);
        }
    }

    public static <T> JsonObject mapToJsonObject(Map<String, T> map) {
        JsonObject jsonObject = new JsonObject();
        mergeIntoJsonObject(jsonObject, map);
        return jsonObject;
    }

    // Prevents instantiation
    private CoreUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
