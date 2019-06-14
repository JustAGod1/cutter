package ru.justagod.mincer.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


public class TreeMap<Node, Value extends TreeMap.Mergeable<Value>> {

    private final Map<Node, TreeMap<Node, Value>> children = new HashMap<>();
    private final Map<Node, Value> deadEnd = new HashMap<>();
    @Nullable
    private final Node asterisk;

    public TreeMap(@Nullable Node asterisk) {
        this.asterisk = asterisk;
    }

    public void put(List<Node> path, Value value) {
        put(0, path, value);
    }

    private List<Value> getAll() {
        List<Value> result = new ArrayList<>(deadEnd.values());
        result.addAll(children.values().stream().flatMap((child) -> child.getAll().stream()).collect(Collectors.toList()));
        return result;
    }

    private void performMerge(Value target) {
        for (Value value : getAll()) {
            value.merge(target);
        }
    }

    public boolean hasNode(List<Node> path) {
        return hasNode(0, path);
    }

    public boolean hasNode(int startIndex, List<Node> path) {
        if (path.size() == startIndex || path.isEmpty()) return false;
        Node key = path.get(startIndex);
        if (path.size() - 1 == startIndex) return deadEnd.containsKey(key) || children.containsKey(key);
        TreeMap<Node, Value> child = children.get(key);
        if (child == null) return false;
        return child.hasNode(startIndex + 1, path);
    }

    public void put(int startIndex, List<Node> path, Value value) {
        if (path.size() == startIndex || path.isEmpty()) return;
        Node key = path.get(startIndex);
        if (startIndex == path.size() - 1) {
            if (asterisk != null && Objects.equals(asterisk, key)) {
                performMerge(value);
            }
            deadEnd.put(key, value);
            return;
        }
        if (asterisk != null && Objects.equals(asterisk, key)) {
            performMerge(value);
            deadEnd.put(asterisk, value);
            return;
        }
        TreeMap<Node, Value> child = children.get(key);
        if (asterisk != null && deadEnd.containsKey(asterisk)) {
            value = value.merge(deadEnd.get(asterisk));
        }
        if (child == null) {
            child = new TreeMap<>(asterisk);
            children.put(key, child);
        }
        child.put(startIndex + 1, path, value);

    }

    public boolean contains(List<Node> value) {
        return contains(0, value);
    }

    public boolean contains(int startIndex, List<Node> path) {
        if (path.size() == startIndex || path.isEmpty()) return false;
        if (deadEnd.containsKey(asterisk)) return true;
        Node key = path.get(startIndex);
        if (startIndex == path.size() - 1) {
            return deadEnd.containsKey(key);
        }
        TreeMap<Node, Value> child = children.get(key);
        if (child != null) return child.contains(startIndex + 1, path);
        return false;
    }

    @Nullable
    public Value get(int startIndex, List<Node> path) {
        if (path.size() == startIndex || path.isEmpty()) return null;
        Node key = path.get(startIndex);
        if (path.size() - 1 == startIndex) {
            Value tmp = deadEnd.get(key);
            if (tmp != null) return tmp;
            else if (asterisk != null && deadEnd.containsKey(asterisk)) return deadEnd.get(asterisk);
        } else if (children.containsKey(key)) {
            Value tmp = children.get(key).get(startIndex + 1, path);
            if (tmp != null) return tmp;
            else if (asterisk != null && deadEnd.containsKey(asterisk)) return deadEnd.get(asterisk);
        }
        return null;
    }

    @Nullable
    public Value get(List<Node> path) {
        return get(0, path);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Node, Value> entry : deadEnd.entrySet()) {
            result.append('\n');
            result.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        for (Map.Entry<Node, TreeMap<Node, Value>> entry : children.entrySet()) {
            String data = entry.getValue().toString().replace("\n", "\n\t");
            result.append("\n");
            result.append(entry.getKey()).append(": ").append(data);
        }
        return result.toString();
    }

    public interface Mergeable<T extends Mergeable<T>> {

        @NotNull
        T merge(@NotNull T other);
    }
}
