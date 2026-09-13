# ADR-018: Object-Aware Physical Estimation

- **Status:** Accepted; implementation pending
- **Decision date:** 2026-09-13
- **Project:** Aetheris
- **Scope:** Object classification, material modeling and mass estimation
- **Artifact revision:** day18-v1

## Context

Aetheris currently measures three spatial dimensions, estimates an external bounding-box volume and combines that volume with a selected material density to estimate mass.

The existing relation is physically valid for a predominantly homogeneous solid that occupies the measured volume:

```text
mass = external volume × material density
```

This assumption is not generally valid for common objects. A book contains paper, covers, adhesive and small air gaps. Furniture assembled from MDF is composed of panels surrounding empty space. Containers may be hollow, filled or partially filled. Other objects may combine wood, glass, metal, plastic, foam and air.

Applying the density of one material to the entire external volume would therefore produce a precise-looking result based on an incorrect physical model.

Aetheris already preserves spatial provenance, measurement uncertainty and a consolidated quality classification. Object-aware estimation must extend those principles rather than hide additional assumptions behind a single mass value.

## Decision

Aetheris will introduce object-aware physical estimation as a phased, domain-driven capability.

The application will keep spatial measurement, object classification, material selection and physical modeling as distinct sources of evidence.

An object category may suggest a physical model and common materials, but it will not silently determine the final estimate. The user must be able to confirm or correct relevant assumptions before an object-aware mass estimate is presented as complete.

The existing homogeneous-solid calculation will remain available as an explicit strategy. It will not be used implicitly for objects whose internal construction is unknown or known to contain substantial empty space.

## Evidence model

An object-aware estimate will distinguish at least four kinds of evidence:

1. **Measured geometry:** dimensions, volume, uncertainty and ARCore placement provenance.
2. **Object classification:** selected or suggested object category and classification confidence.
3. **Material evidence:** selected, inferred or confirmed material densities and their uncertainty.
4. **Physical-model assumptions:** solid occupancy, panel thickness, shell thickness, component composition, fill fraction or empirical reference data.

These evidence sources must remain independently inspectable so that a user can understand why an estimate was produced.

## Object profiles

The domain will support predefined object profiles. A profile describes a known object archetype without representing a specific physical instance.

Initial examples may include:

- solid rectangular object;
- book;
- box or container;
- shelf or bookcase assembled from panels;
- table;
- generic composite object.

An object profile may provide:

- a stable identifier;
- a user-facing name and description;
- a physical estimation strategy;
- compatible or commonly used materials;
- parameters required by its physical model;
- assumptions that require user confirmation;
- limitations and applicability notes.

Profiles must not encode a nominal mass as if every object in a category were identical.

## Physical estimation strategies

The design will allow different strategies instead of forcing every object through external volume multiplied by one density.

### Homogeneous solid

Uses the current model:

```text
mass = measured volume × material density
```

This strategy is appropriate only when the measured volume is predominantly occupied by one material.

### Occupancy-adjusted volume

Uses an explicitly declared occupied fraction:

```text
mass = external volume × occupancy fraction × material density
```

The occupancy fraction is a model assumption and must carry uncertainty. It must not be guessed without disclosure.

### Panel assembly

Estimates the volume of individual structural panels from external dimensions, panel count and panel thickness. This is suitable for shelves and some furniture.

The model must avoid counting overlapping panel volumes more than once when that difference is materially relevant.

### Shell or container

Estimates material volume as the difference between external and internal geometry. Contents, when present, are modeled separately.

### Component composition

Combines multiple components, each with its own geometry, material density and uncertainty:

```text
total mass = sum(component volume × component density)
```

This strategy is appropriate for books and other composite objects when sufficient parameters are available.

### Empirical reference

Uses traceable reference data associated with a clearly defined object family. Empirical estimates must record their source, applicability range and uncertainty. They must not be presented as direct measurements.

## Recognition policy

Automatic visual recognition will be introduced only after the manual object-profile workflow is stable and tested.

Recognition will produce ranked suggestions rather than an irreversible classification. The interface must show the suggested category, confidence and model version, and must permit user confirmation or correction.

Low-confidence or unsupported recognition must fall back to manual selection. A visual label alone is not sufficient evidence for internal construction, material composition or mass.

Optional future evidence may include segmentation, geometric ratios, OCR, barcodes or external product identifiers. Each source must retain its own provenance.

## Material policy

`MaterialDensity` and `MaterialDensityCatalog` remain valid for density references.

The catalog will later be extended with materials relevant to object profiles, such as paper, cardboard, MDF, plywood and common plastics. Density ranges must be based on documented references and must retain uncertainty caused by composition, moisture, manufacturing and grade.

An object profile may suggest materials, but the suggestion must remain distinguishable from a user-confirmed material.

## Mass and uncertainty policy

`CalculateMassUseCase` remains the calculation strategy for a homogeneous solid. Existing callers and tests must continue to behave consistently.

Object-aware estimation will use separate strategies or use cases so that structural assumptions are explicit in the type system.

The final uncertainty must consider, when applicable:

- spatial measurement uncertainty;
- density uncertainty;
- object-classification uncertainty;
- model-parameter uncertainty;
- occupancy or construction uncertainty;
- uncertainty in empirical reference data.

The application must avoid collapsing unsupported model uncertainty into an unjustifiably narrow numeric interval.

## Human confirmation

Before completing an object-aware estimate, Aetheris should require confirmation of assumptions that materially affect the result, including:

- object category;
- physical estimation strategy;
- principal material or component materials;
- structural parameters not directly measured;
- whether a container is empty, filled or partially filled.

Confirmation records acceptance of the model assumptions; it does not certify metrological accuracy.

## Implementation sequence

The capability will be implemented incrementally:

1. Introduce domain types for object profiles and physical estimation strategies.
2. Add a small manually selected catalog of object profiles.
3. Add materials required by the initial profiles with documented uncertainty.
4. Implement and test one strategy at a time, preserving the existing solid calculation.
5. Propagate object, material and model provenance into presentation state.
6. Add user confirmation and correction controls.
7. Validate results against physical objects with known dimensions and independently measured mass.
8. Introduce automatic recognition only after the deterministic workflow is stable.

## Consequences

### Positive

- Prevents external bounding-box volume from being treated as solid material by default.
- Makes mass estimation more physically meaningful for common objects.
- Preserves scientific transparency and explicit assumptions.
- Allows deterministic manual operation before introducing machine learning.
- Creates a clean extension point for future computer-vision models.
- Supports richer validation and uncertainty analysis.

### Negative

- Requires more domain types, strategies and interface states.
- Some objects will require parameters that cannot be recovered from one camera view.
- Composite and irregular objects may remain difficult to estimate reliably.
- Model uncertainty may dominate sensor uncertainty.
- Automatic recognition will add model distribution, versioning and validation responsibilities.

## Alternatives considered

### Continue using external volume multiplied by one density

Rejected as the general solution because it treats hollow and composite objects as solid blocks and can produce large systematic errors.

### Store one average mass for every object category

Rejected because category-level averages ignore scale, construction, material and condition, and offer weak scientific traceability.

### Introduce automatic recognition before manual profiles

Rejected for the initial implementation because recognition would add probabilistic behavior before the physical estimation rules and confirmation workflow were independently validated.

### Replace the existing mass calculation immediately

Rejected because the current calculation remains correct for its stated homogeneous-solid assumptions and has existing test coverage.

## Validation requirements

Each physical strategy must be validated independently with:

- deterministic unit tests;
- boundary and invalid-parameter tests;
- uncertainty-propagation tests;
- comparison with analytically calculable examples;
- physical-device workflow tests;
- comparison with independently measured reference mass when practical.

Validation results must distinguish experimental estimates from certified measurements.

## Relationship to previous decisions

This decision extends:

- ADR-003, pure Kotlin mathematical domain isolation;
- ADR-004, first-class metrological uncertainty modeling;
- ADR-015, sequential axis capture and uncertainty-aware volume;
- ADR-016, frame-affine ARCore placement;
- ADR-017, anchor-placement provenance.

It does not change the frame-affine ARCore pipeline or the current spatial-quality classification.
