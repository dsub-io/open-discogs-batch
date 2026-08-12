package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/** Strict typed access to the JSON subset used by model-owned contracts. */
final class StrictJsonObject {

  private static final int MAXIMUM_NESTING_DEPTH = 32;
  private static final int MAXIMUM_CODE_POINTS = 1_000_000;

  private final String path;
  private final Map<String, Node> fields;

  private StrictJsonObject(String path, MappingNode node) {
    this.path = path;
    requireJsonObject(node, path);
    fields = new HashMap<>(node.getValue().size());
    for (NodeTuple tuple : node.getValue()) {
      ScalarNode key = requireStringNode(tuple.getKeyNode(), path + " key");
      String name = key.getValue();
      if (fields.putIfAbsent(name, tuple.getValueNode()) != null) {
        throw failure(path + " contains duplicate field " + name);
      }
    }
  }

  static StrictJsonObject parse(String json, String resourceName) {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(true);
    options.setMaxAliasesForCollections(0);
    options.setNestingDepthLimit(MAXIMUM_NESTING_DEPTH);
    options.setCodePointLimit(MAXIMUM_CODE_POINTS);
    try {
      Node root = new Yaml(options).compose(new StringReader(json));
      if (!(root instanceof MappingNode mapping)) {
        throw failure(resourceName + " root must be a JSON object");
      }
      requireNoAnchor(root, resourceName);
      return new StrictJsonObject(resourceName, mapping);
    } catch (YAMLException exception) {
      throw new InitializationFailureException(
          "parse JSON contract " + resourceName + ": " + exception.getMessage(), exception);
    }
  }

  void requireFields(Set<String> required, Set<String> optional) {
    Set<String> missing = new HashSet<>(required);
    missing.removeAll(fields.keySet());
    if (!missing.isEmpty()) {
      throw failure(path + " is missing fields " + missing.stream().sorted().toList());
    }
    Set<String> allowed = new HashSet<>(required);
    allowed.addAll(optional);
    Set<String> unknown = new HashSet<>(fields.keySet());
    unknown.removeAll(allowed);
    if (!unknown.isEmpty()) {
      throw failure(path + " contains unknown fields " + unknown.stream().sorted().toList());
    }
  }

  String string(String name) {
    return requireStringNode(required(name), childPath(name)).getValue();
  }

  String optionalString(String name) {
    Node node = fields.get(name);
    return node == null ? null : requireStringNode(node, childPath(name)).getValue();
  }

  int integer(String name) {
    Node node = required(name);
    if (!(node instanceof ScalarNode scalar)
        || !Tag.INT.equals(scalar.getTag())
        || scalar.getScalarStyle() != ScalarStyle.PLAIN) {
      throw failure(childPath(name) + " must be a JSON integer");
    }
    try {
      return Integer.parseInt(scalar.getValue());
    } catch (NumberFormatException exception) {
      throw new InitializationFailureException(
          childPath(name) + " is outside the supported integer range", exception);
    }
  }

  StrictJsonObject object(String name) {
    Node node = required(name);
    if (!(node instanceof MappingNode mapping)) {
      throw failure(childPath(name) + " must be a JSON object");
    }
    return new StrictJsonObject(childPath(name), mapping);
  }

  List<StrictJsonObject> objects(String name) {
    SequenceNode sequence = requireSequence(name);
    List<StrictJsonObject> objects = new ArrayList<>(sequence.getValue().size());
    for (int index = 0; index < sequence.getValue().size(); index++) {
      objects.add(objectElement(name, sequence.getValue().get(index), index));
    }
    return List.copyOf(objects);
  }

  List<String> strings(String name) {
    SequenceNode sequence = requireSequence(name);
    return sequence.getValue().stream()
        .map(node -> requireStringNode(node, childPath(name) + "[]").getValue())
        .toList();
  }

  private StrictJsonObject objectElement(String name, Node node, int index) {
    if (!(node instanceof MappingNode mapping)) {
      throw failure(childPath(name) + '[' + index + "] must be a JSON object");
    }
    return new StrictJsonObject(childPath(name) + '[' + index + ']', mapping);
  }

  private SequenceNode requireSequence(String name) {
    Node node = required(name);
    if (!(node instanceof SequenceNode sequence)) {
      throw failure(childPath(name) + " must be a JSON array");
    }
    return sequence;
  }

  private Node required(String name) {
    Node node = fields.get(name);
    if (node == null) {
      throw failure(childPath(name) + " is required");
    }
    return node;
  }

  private String childPath(String name) {
    return path + '.' + name;
  }

  private static ScalarNode requireStringNode(Node node, String path) {
    if (!(node instanceof ScalarNode scalar)
        || !Tag.STR.equals(scalar.getTag())
        || scalar.getScalarStyle() != ScalarStyle.DOUBLE_QUOTED) {
      throw failure(path + " must be a double-quoted JSON string");
    }
    return scalar;
  }

  private static void requireJsonObject(MappingNode node, String path) {
    if (!Tag.MAP.equals(node.getTag()) || node.getFlowStyle() != FlowStyle.FLOW) {
      throw failure(path + " must use JSON object syntax");
    }
  }

  private static void requireNoAnchor(Node node, String path) {
    if (node.getAnchor() != null) {
      throw failure(path + " must not contain YAML anchors");
    }
    if (node instanceof MappingNode mapping) {
      for (NodeTuple tuple : mapping.getValue()) {
        requireNoAnchor(tuple.getKeyNode(), path);
        requireNoAnchor(tuple.getValueNode(), path);
      }
    } else if (node instanceof SequenceNode sequence) {
      for (Node item : sequence.getValue()) {
        requireNoAnchor(item, path);
      }
    }
  }

  private static InitializationFailureException failure(String message) {
    return new InitializationFailureException(message);
  }
}
