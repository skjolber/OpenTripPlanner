package org.opentripplanner.osm.wayproperty.specifier;

import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.NONE;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.WILDCARD;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.osm.model.OsmEntity;

public sealed interface Condition {
  /**
   * Test to what degree the OSM entity matches with this operation when taking the regular tag keys
   * into account.
   */
  boolean isMatch(OsmEntity way);

  /**
   * Test to what degree the OSM entity matches with this operation.
   */

  MatchResult match(OsmEntity way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':left' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:right=lane` when the `cycleway=lane` was
   * required but `cycleway:left=lane` should match.
   */
  boolean isLeftMatch(OsmEntity way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':right' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:left=lane` when the `cycleway=lane` was
   * required but `cycleway:right=lane` should match.
   */
  boolean isRightMatch(OsmEntity way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':both' key
   * suffixes into account.
   */
  boolean isExplicitBothMatch(OsmEntity way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':forward' key
   * suffixes into account.
   */
  boolean isForwardMatch(OsmEntity way);

  MatchResult matchForward(OsmEntity way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':backward' key
   * suffixes into account.
   */

  boolean isBackwardMatch(OsmEntity way);

  MatchResult matchBackward(OsmEntity way);

  enum MatchResult {
    EXACT,
    WILDCARD,
    NONE,
  }

  abstract sealed class AbstractCondition implements Condition {

    protected final String key;
    protected final String keyLeft;
    protected final String keyRight;
    protected final String keyBoth;
    protected final String keyForward;
    protected final String keyBackward;
    protected final MatchResult matchType;

    public AbstractCondition(String key, MatchResult matchType) {
      // performance enhancement: prepare tag lookup keys
      // so that minimal work is performed per condition matching
      this.key = OsmEntity.normalizeTagKey(key);
      this.matchType = matchType;

      this.keyLeft = OsmEntity.normalizeTagKey(this.key + ":left");
      this.keyRight = OsmEntity.normalizeTagKey(this.key + ":right");
      this.keyBoth = OsmEntity.normalizeTagKey(this.key + ":both");
      this.keyForward = OsmEntity.normalizeTagKey(this.key + ":forward");
      this.keyBackward = OsmEntity.normalizeTagKey(this.key + ":backward");
    }

    public MatchResult match(OsmEntity way) {
      return isMatch(way) ? matchType : NONE;
    }

    public MatchResult matchForward(OsmEntity way) {
      return isForwardMatch(way) ? matchType : NONE;
    }

    public MatchResult matchBackward(OsmEntity way) {
      return isBackwardMatch(way) ? matchType : NONE;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      AbstractCondition that = (AbstractCondition) o;
      return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }
  }

  abstract sealed class AbstractValueCondition extends AbstractCondition {

    public AbstractValueCondition(String key, MatchResult matchType) {
      super(key, matchType);
    }

    public boolean isMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(key);
      return tag != null && isValueMatch(tag);
    }

    protected abstract boolean isValueMatch(String tag);

    public boolean isLeftMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyLeft);

      if (tag != null) {
        return isValueMatch(tag);
      }
      return isExplicitBothMatch(way);
    }

    public boolean isRightMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyRight);
      if (tag != null) {
        return isValueMatch(tag);
      }
      return isExplicitBothMatch(way);
    }

    public boolean isExplicitBothMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyBoth);
      if (tag != null) {
        return isValueMatch(tag);
      }
      return isMatch(way);
    }

    public boolean isForwardMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyForward);
      if (tag != null) {
        return isValueMatch(tag);
      }
      /* Assumes right hand traffic */
      return isRightMatch(way);
    }

    public boolean isBackwardMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyBackward);
      if (tag != null) {
        return isValueMatch(tag);
      }
      /* Assumes right hand traffic */
      return isLeftMatch(way);
    }
  }

  /**
   * Selects tags where a given key/value matches.
   */
  public final class Equals extends AbstractValueCondition {

    private final String value;

    public Equals(String key, String value) {
      super(key, EXACT);
      this.value = value;
    }

    @Override
    protected boolean isValueMatch(String tag) {
      return value.equals(tag);
    }

    @Override
    public String toString() {
      return "%s=%s".formatted(key, value);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Equals equals = (Equals) o;
      return Objects.equals(value, equals.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), value);
    }
  }

  /**
   * Selects tags with a given key.
   */

  public final class Present extends AbstractCondition {

    public Present(String key) {
      super(key, WILDCARD);
    }

    @Override
    public boolean isMatch(OsmEntity way) {
      return way.hasTagForNormalizedTagKey(key);
    }

    @Override
    public boolean isLeftMatch(OsmEntity way) {
      return way.hasTagForNormalizedTagKey(keyLeft) || isExplicitBothMatch(way);
    }

    @Override
    public boolean isRightMatch(OsmEntity way) {
      return way.hasTagForNormalizedTagKey(keyRight) || isExplicitBothMatch(way);
    }

    @Override
    public boolean isExplicitBothMatch(OsmEntity way) {
      return way.hasTagForNormalizedTagKey(keyBoth) || isMatch(way);
    }

    @Override
    public boolean isForwardMatch(OsmEntity way) {
      return way.hasTagForNormalizedTagKey(keyForward) || isRightMatch(way);
    }

    @Override
    public boolean isBackwardMatch(OsmEntity way) {
      return way.hasTagForNormalizedTagKey(keyBackward) || isLeftMatch(way);
    }

    @Override
    public String toString() {
      return "present(%s)".formatted(key);
    }
  }

  /**
   * Selects tags where a given tag is absent.
   */

  public final class Absent extends AbstractCondition {

    public Absent(String key) {
      super(key, EXACT);
    }

    @Override
    public boolean isMatch(OsmEntity way) {
      return !way.hasTagForNormalizedTagKey(key);
    }

    @Override
    public boolean isLeftMatch(OsmEntity way) {
      return !way.hasTagForNormalizedTagKey(keyLeft) && isExplicitBothMatch(way);
    }

    @Override
    public boolean isRightMatch(OsmEntity way) {
      return !way.hasTagForNormalizedTagKey(keyRight) && isExplicitBothMatch(way);
    }

    @Override
    public boolean isExplicitBothMatch(OsmEntity way) {
      return !way.hasTagForNormalizedTagKey(keyBoth) && isMatch(way);
    }

    @Override
    public boolean isForwardMatch(OsmEntity way) {
      return !way.hasTagForNormalizedTagKey(keyForward) && isRightMatch(way);
    }

    @Override
    public boolean isBackwardMatch(OsmEntity way) {
      return !way.hasTagForNormalizedTagKey(keyBackward) && isLeftMatch(way);
    }

    @Override
    public String toString() {
      return "!%s".formatted(key);
    }
  }

  /**
   * Selects tags where the integer value is greater than a given number.
   */
  public final class GreaterThan extends AbstractValueCondition {

    private final int value;

    public GreaterThan(String key, int value) {
      super(key, EXACT);
      this.value = value;
    }

    @Override
    protected boolean isValueMatch(String tag) {
      try {
        return Integer.parseInt(tag) > value;
      } catch (Exception e) {
        // ignore
      }
      return false;
    }

    @Override
    public String toString() {
      return "%s > %s".formatted(key, value);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      GreaterThan that = (GreaterThan) o;
      return value == that.value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), value);
    }
  }

  /**
   * Selects tags where the integer value is less than a given number.
   */

  public final class LessThan extends AbstractValueCondition {

    private final int value;

    public LessThan(String key, int value) {
      super(key, EXACT);
      this.value = value;
    }

    @Override
    public String toString() {
      return "%s < %s".formatted(key, value);
    }

    @Override
    protected boolean isValueMatch(String tag) {
      try {
        return Integer.parseInt(tag) < value;
      } catch (Exception e) {
        // ignore
      }
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      LessThan lessThan = (LessThan) o;
      return value == lessThan.value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), value);
    }
  }

  /**
   * Selects integer tag values and checks if they are in between a lower and an upper bound.
   */
  public final class InclusiveRange extends AbstractValueCondition {

    private final int upper;
    private final int lower;

    public InclusiveRange(String key, int upper, int lower) {
      super(key, EXACT);
      if (upper < lower) {
        throw new IllegalArgumentException("Upper bound is lower than lower bound");
      }
      this.upper = upper;
      this.lower = lower;
    }

    @Override
    protected boolean isValueMatch(String value) {
      try {
        int integer = Integer.parseInt(value);
        return lower <= integer && integer <= upper;
      } catch (Exception e) {
        // ignore
      }
      return false;
    }

    @Override
    public String toString() {
      return "%s > %s < %s".formatted(lower, key, upper);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      InclusiveRange that = (InclusiveRange) o;
      return upper == that.upper && lower == that.lower;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), upper, lower);
    }
  }

  /**
   * Selects a tag which has one of a set of given values.
   */
  public final class OneOf extends AbstractValueCondition {

    // keep original value input order for documentation
    private final String[] values;
    private final Set<String> set;

    public OneOf(String key, String... values) {
      super(key, EXACT);
      this.values = values;
      this.set = Set.of(values);
    }

    @Override
    protected boolean isValueMatch(String tagValue) {
      return set.contains(tagValue);
    }

    @Override
    public String toString() {
      return "%s one of [%s]".formatted(key, String.join(", ", values));
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      OneOf oneOf = (OneOf) o;
      return Objects.equals(values, oneOf.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), values);
    }
  }

  /**
   * Selects a tag where one of the following conditions is true:
   *  - one of a set of given values matches
   *  - the tag is absent
   */
  public final class OneOfOrAbsent extends AbstractCondition {

    // keep original value input order for documentation
    private final String[] values;
    private final Set<String> set;

    public OneOfOrAbsent(String key) {
      this(key, "no", "none");
    }

    public OneOfOrAbsent(String key, String... values) {
      super(key, EXACT);
      this.values = values;
      this.set = Set.of(values);
    }

    public boolean isMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(key);
      return tag == null || set.contains(tag);
    }

    public boolean isLeftMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyLeft);
      if (tag != null) {
        return set.contains(tag);
      }
      return isExplicitBothMatch(way);
    }

    public boolean isRightMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyRight);
      if (tag != null) {
        return set.contains(tag);
      }
      return isExplicitBothMatch(way);
    }

    public boolean isExplicitBothMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyBoth);
      if (tag != null) {
        return set.contains(tag);
      }
      return isMatch(way);
    }

    public boolean isForwardMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyForward);
      if (tag != null) {
        return set.contains(tag);
      }
      return isRightMatch(way);
    }

    public boolean isBackwardMatch(OsmEntity way) {
      String tag = way.getTagForNormalizedTagKey(keyBackward);
      if (tag != null) {
        return set.contains(tag);
      }
      return isLeftMatch(way);
    }

    @Override
    public String toString() {
      return "%s not one of [%s] or absent".formatted(key, String.join(", ", values));
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      OneOfOrAbsent that = (OneOfOrAbsent) o;
      return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), values);
    }
  }
}
