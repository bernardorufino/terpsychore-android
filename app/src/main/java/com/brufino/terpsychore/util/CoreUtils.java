package com.brufino.terpsychore.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

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

    // Prevents instantiation
    private CoreUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
