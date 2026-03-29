---
name: class-diagram
description: Generate CLASS_DIAGRAM.md for completed LLD implementation
userInvocable: true
---

Generate CLASS_DIAGRAM.md with strict ASCII alignment and nomenclature. No deviations.

## File Structure (MANDATORY)
```
# [Problem] - Class Diagram

```
[ASCII diagram in code fence]
```

## Relationships
[lifecycle explanations required]

## Core Flow
[4-6 steps: Class.method() → outcome]

## Key Decisions
[optional trade-offs]
```

## ASCII Diagram Rules

**Characters (ONLY THESE):**
- Box: ┌ ┐ └ ┘ ─ │
- Junctions: ├ ┤ ┬ ┴

**Box Format:**
- Width: 21+ chars, consistent per row
- Spacing: 10+ spaces between boxes
- **CRITICAL**: Wrap entire diagram in ``` code fence ```

**Alignment (VERIFY):**
- All ┌ corners align vertically at same columns
- All │ bars align at consistent columns
- Spacing between boxes is uniform

**Class Members:**
- Visibility: `+` public, `-` private, `#` protected
- Attributes: `-attributeName: Type`
- Methods: `+methodName(param: Type): ReturnType`
- Abbreviate methods if > 18 chars (keep readable)

**Enums:**
```
┌─────────────────┐
│ «enumeration»   │
│   EnumName      │
├─────────────────┤
│ VALUE_ONE       │
│ VALUE_TWO       │
└─────────────────┘
```
Place at bottom of diagram, side-by-side.

## Relationship Nomenclature (STRICT)

**ONLY use these labels:**

| Label | Format | When |
|-------|--------|------|
| Strong ownership | `composition (1:many)` | Lifecycle tied - parent owns child |
| Weak ownership | `aggregation (1:many)` | Child exists independently |
| Inheritance | `is-a` | Subclass extends superclass |
| Dependency | `uses` | Uses without ownership |
| Interface | `implements` | Implements interface contract |

**Cardinality (ALWAYS in parentheses):**
- `(1:1)` - one-to-one
- `(1:many)` - one-to-many
- `(many:many)` - many-to-many

**Examples:**
```
ClassA──composition (1:many)──ClassB    ✓
ClassA──aggregation (1:1)──ClassB       ✓
ClassA──is-a──ClassB                    ✓

ClassA──has-a──ClassB                   ✗ vague
ClassA +1───*1 ClassB                   ✗ no symbols
```

## Relationships Section (MANDATORY)

**Format:**
```
## Relationships
- **ClassA → ClassB**: relationship_type (cardinality) - Lifecycle/purpose explanation
```

**Requirements:**
- Composition: MUST explain lifecycle dependency
- Aggregation: MUST clarify independent existence
- Uses: MUST state purpose

**Example:**
```
- **Controller → Elevator**: composition (1:many) - Elevators destroyed when controller removed
- **Elevator → Request**: aggregation (1:many) - Requests exist independently
- **Elevator → Direction**: uses - Current movement state from enum
```

## Core Flow Section (MANDATORY)

**Format:**
```
## Core Flow
1. ClassName.method() → outcome description
2. OtherClass.method() → next outcome
3. Final.method() → final state
```

**Rules:**
- 4-6 steps maximum
- Arrow (→) separates method from effect
- Sequential order (main use case only)

## Execution Steps

1. **Read** all `.java` files → identify classes, relationships
2. **Analyze** relationships:
   - Composition? Parent creates and owns child (lifecycle tied)
   - Aggregation? Child exists independently
   - Inheritance? Is-a relationship
   - Dependency? Uses without ownership
   - Interface? Implements contract
3. **Draw** diagram:
   - Layout: controller → entities → data (left-to-right)
   - Wrap in ``` code fence ```
   - Enums at bottom
4. **Verify** alignment in monospace viewer
5. **Write** Relationships section (lifecycle explanations)
6. **Write** Core Flow section (4-6 steps)
7. **Validate** (see checklist below)

## Final Validation (BEFORE SAVING)

- [ ] Diagram in ``` fence, vertical alignment perfect
- [ ] Relationships: composition/aggregation/is-a/uses/implements ONLY
- [ ] Cardinality: (1:1), (1:many), (many:many) format
- [ ] Lifecycle explanations for composition/aggregation
- [ ] Core Flow: 4-6 steps with Class.method() → outcome
- [ ] File saved as CLASS_DIAGRAM.md in package directory

## Guardrails

**REJECT these:**
- Vague labels: "has-a", "contains", "holds"
- Cardinality symbols: +1, *1, 0..*
- Missing code fence (breaks GitHub rendering)
- Inconsistent spacing/alignment
- Missing lifecycle explanations
- > 6 steps in Core Flow

Reference: `src/main/java/com/oshaklya/elevator/CLASS_DIAGRAM.md`
