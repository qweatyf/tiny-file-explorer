package com.example.myempty.activity2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AlarmData {

    public static final int TYPE_ONCE = 0;
    public static final int TYPE_DAILY = 1;
    public static final int TYPE_WEEKLY = 2;

    public long id;
    public int hour;
    public int minute;
    public int type;
    public boolean[] days = new boolean[7];
    public String label = "";
    public String musicPath = "";
    public long triggerAt;

    public AlarmData() {
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("hour", hour);
            o.put("minute", minute);
            o.put("type", type);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < 7; i++) arr.put(days[i]);
            o.put("days", arr);
            o.put("label", label == null ? "" : label);
            o.put("music", musicPath == null ? "" : musicPath);
            o.put("triggerAt", triggerAt);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static AlarmData fromJson(String s) {
        try {
            JSONObject o = new JSONObject(s);
            AlarmData d = new AlarmData();
            d.id = o.optLong("id", 0);
            d.hour = o.optInt("hour", 0);
            d.minute = o.optInt("minute", 0);
            d.type = o.optInt("type", TYPE_ONCE);
            JSONArray arr = o.optJSONArray("days");
            if (arr != null) {
                for (int i = 0; i < 7 && i < arr.length(); i++) {
                    d.days[i] = arr.optBoolean(i, false);
                }
            }
            d.label = o.optString("label", "");
            d.musicPath = o.optString("music", "");
            d.triggerAt = o.optLong("triggerAt", 0);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    public String timeText() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String typeText(String[] weekNames) {
        if (type == TYPE_ONCE) return "once";
        if (type == TYPE_DAILY) return "daily";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(weekNames[i]);
            }
        }
        if (sb.length() == 0) return "weekly";
        return sb.toString();
    }

    public static String listToJson(List<AlarmData> list) {
        JSONArray arr = new JSONArray();
        for (AlarmData d : list) {
            try {
                arr.put(new JSONObject(d.toJson()));
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    public static List<AlarmData> listFromJson(String s) {
        List<AlarmData> list = new ArrayList<>();
        if (s == null || s.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                AlarmData d = fromJson(arr.getJSONObject(i).toString());
                if (d != null) list.add(d);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public long nextTrigger(long now) {
        if (type == TYPE_DAILY) {
            java.util.Calendar t = java.util.Calendar.getInstance();
            t.setTimeInMillis(now);
            t.set(java.util.Calendar.HOUR_OF_DAY, hour);
            t.set(java.util.Calendar.MINUTE, minute);
            t.set(java.util.Calendar.SECOND, 0);
            t.set(java.util.Calendar.MILLISECOND, 0);
            if (t.getTimeInMillis() <= now) {
                t.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }
            return t.getTimeInMillis();
        }

        if (type == TYPE_WEEKLY) {
            for (int offset = 0; offset <= 7; offset++) {
                java.util.Calendar t = java.util.Calendar.getInstance();
                t.setTimeInMillis(now);
                t.add(java.util.Calendar.DAY_OF_MONTH, offset);
                t.set(java.util.Calendar.HOUR_OF_DAY, hour);
                t.set(java.util.Calendar.MINUTE, minute);
                t.set(java.util.Calendar.SECOND, 0);
                t.set(java.util.Calendar.MILLISECOND, 0);
                if (t.getTimeInMillis() <= now) continue;
                int dow = t.get(java.util.Calendar.DAY_OF_WEEK);
                int idx = dow - java.util.Calendar.MONDAY;
                if (idx < 0) idx += 7;
                if (days[idx]) return t.getTimeInMillis();
            }
            java.util.Calendar t = java.util.Calendar.getInstance();
            t.setTimeInMillis(now);
            t.add(java.util.Calendar.DAY_OF_MONTH, 7);
            t.set(java.util.Calendar.HOUR_OF_DAY, hour);
            t.set(java.util.Calendar.MINUTE, minute);
            t.set(java.util.Calendar.SECOND, 0);
            t.set(java.util.Calendar.MILLISECOND, 0);
            return t.getTimeInMillis();
        }

        java.util.Calendar t = java.util.Calendar.getInstance();
        t.setTimeInMillis(now);
        t.set(java.util.Calendar.HOUR_OF_DAY, hour);
        t.set(java.util.Calendar.MINUTE, minute);
        t.set(java.util.Calendar.SECOND, 0);
        t.set(java.util.Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= now) {
            t.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
        return t.getTimeInMillis();
    }
}