# Snapshot Detail Adapter Flattening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Snapshot Detail's custom node/provider graph with one two-row adapter while preserving section, interaction, accessibility, empty-state, and report behavior.

**Architecture:** `SnapshotDetailSection` and `SnapshotDetailItemDisplayData` remain the source models. A sealed `SnapshotDetailRow` flattens each section into one header plus its visible items; `SnapshotDetailAdapter` renders the two row types directly and owns only expansion state. Presentation mapping stays in the existing UI-model files, while navigation stays in `SnapshotDetailActivity`.

**Tech Stack:** Kotlin, Android Views, RecyclerView, BRVAH 4.4.1 compatibility adapter, JUnit 4, Android instrumentation tests.

**Spec:** `AGENTS.md`

## Global Constraints

- Preserve new-install, deleted, and empty state views.
- Preserve section order, default expanded state, title accessibility descriptions, moved-path rendering, rule-chip icon styling, click/long-click eligibility, and report ordering.
- Keep package lookup, report file writing, and other heavy work off the main thread.
- Do not change snapshot domain models or broaden the change to comparison/diff construction.
- Do not commit, reset, or overwrite unrelated work.

---

### Task 1: Freeze row, interaction, presentation, and report behavior

**Files:**
- Create: `app/src/test/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/SnapshotDetailRowPlannerTest.kt`
- Create: `app/src/test/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/SnapshotDetailInteractionPolicyTest.kt`
- Create: `app/src/test/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/SnapshotDetailPresentationTest.kt`
- Create: `app/src/test/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/SnapshotDetailReportTest.kt`

**Interfaces:**
- Consumes: `SnapshotDetailSection`, `SnapshotDetailItemDisplayData`, and existing render-state types.
- Produces: executable requirements for `SnapshotDetailRow`, `buildSnapshotDetailRows`, interaction policy extensions, presentation extensions, and visible-row report generation.

- [ ] **Step 1: Add row-planning tests**

Assert that two sections flatten as `Header, Item*, Header, Item*`, all sections start expanded, and collapsing one type removes only that section's item rows.

- [ ] **Step 2: Add interaction-policy tests**

Assert that removed rows do not open detail; DEX and owner-package components do not open reference; external components do; and rule-chip availability preserves label and regex metadata.

- [ ] **Step 3: Add presentation tests**

Assert expanded/collapsed descriptions, status/count mapping, moved previous-package path, and `ThemeTint`/`Original`/`Desaturated` chip styles.

- [ ] **Step 4: Add report tests**

Assert visible-row order: header report followed by expanded item reports, with collapsed item reports omitted to match the current flattened adapter data.

- [ ] **Step 5: Run the focused tests and confirm they fail because the new row API is absent**

Run:

```powershell
.\gradlew.bat :app:testFossDebugUnitTest --tests "com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetail*Test"
```

Expected: compilation failure for the not-yet-created row and policy APIs.

### Task 2: Implement the two-row adapter and presentation mappings

**Files:**
- Rewrite: `app/src/main/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/SnapshotDetailAdapter.kt`
- Modify: `app/src/main/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/model/SnapshotDetailTitleRenderState.kt`
- Modify: `app/src/main/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/model/SnapshotDetailItemCardRenderState.kt`

**Interfaces:**
- Produces: `SnapshotDetailRow.Header`, `SnapshotDetailRow.Item`, `buildSnapshotDetailRows`, `SnapshotDetailAdapter.submitSections`, `toggleSectionAt`, `itemAt`, and `reportText`.
- Produces: `SnapshotDetailSection.toTitleRenderState(expanded)` and `SnapshotDetailItemDisplayData.toItemViewRenderState()`.

- [ ] **Step 1: Add the sealed row model and pure planner**

`Header` stores the section and expanded flag. `Item` stores section type and display data. The planner emits items only when the section type is not collapsed.

- [ ] **Step 2: Add direct presentation mappings**

Map status counts directly from `SnapshotDetailStatusCount`; map moved paths and chip styles directly from `SnapshotDetailItemDisplayData` without intermediate node DTOs.

- [ ] **Step 3: Replace `BaseNodeAdapter` with `BaseQuickAdapter<SnapshotDetailRow, BaseViewHolder>`**

Use header/item view types, create `SnapshotDetailTitleView` and `SnapshotDetailItemView`, bind the direct render states, clear stale chip listeners on every bind, and retain `AntiShakeUtils` for chip clicks.

- [ ] **Step 4: Implement expansion and report APIs**

`submitSections` resets to the current default-expanded behavior. `toggleSectionAt` updates the collapsed type set and resubmits rows. `reportText` concatenates the currently visible rows exactly as the old flattened node list did.

- [ ] **Step 5: Run the new focused tests**

Run the Task 1 command. Expected: all new tests pass.

### Task 3: Route Activity interactions directly through domain display data

**Files:**
- Modify: `app/src/main/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/SnapshotDetailActivity.kt`

**Interfaces:**
- Consumes: `SnapshotDetailAdapter.itemAt`, `toggleSectionAt`, `submitSections`, `reportText`.

- [ ] **Step 1: Replace node/action imports and adapter construction**

Give the adapter a rule-chip callback that opens `LibDetailDialogFragment` from the Activity's own fragment manager.

- [ ] **Step 2: Preserve row click behavior**

Toggle headers first. For item rows, keep `AntiShakeUtils`, reject `REMOVED`, and launch the detail page with `item.name` and `item.itemType`.

- [ ] **Step 3: Preserve long-click behavior**

Reject DEX and owner-package components; otherwise launch the reference page with the rule label when present.

- [ ] **Step 4: Submit sections and generate reports through the new adapter**

Keep telemetry and state-view enabling unchanged; replace node mapping with `submitSections(content.sections)` and report iteration with `adapter.reportText()`.

### Task 4: Delete the redundant node/provider layer and consolidate tests

**Files:**
- Delete: `app/src/main/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/node/`
- Delete: `app/src/main/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/provider/`
- Delete: `app/src/test/kotlin/com/absinthe/libchecker/domain/snapshot/detail/ui/adapter/node/`

**Interfaces:**
- Consumes: the Task 1 replacement behavior tests.

- [ ] **Step 1: Verify no production or test references remain**

Run:

```powershell
rg -n "BaseSnapshotNode|SnapshotTitleNode|SnapshotDetailNode|SnapshotReportNode|SnapshotTitleProvider|SnapshotDetailItemProvider" app/src
```

Expected: no references outside files scheduled for deletion.

- [ ] **Step 2: Delete all obsolete node/provider production files and their wrapper-only tests**

Use an explicit file list and preserve the four new behavior suites.

- [ ] **Step 3: Run focused unit tests and Kotlin compilation**

```powershell
.\gradlew.bat :app:testFossDebugUnitTest --tests "com.absinthe.libchecker.domain.snapshot.detail.*"
.\gradlew.bat :app:compileFossDebugKotlin
```

Expected: both commands succeed.

### Task 5: Verify behavior and quantify the reduction

**Files:**
- Inspect: all files changed by Tasks 1-4.

- [ ] **Step 1: Run format, unit, compile, and APK gates**

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat :app:testFossDebugUnitTest --tests "com.absinthe.libchecker.domain.snapshot.detail.*"
.\gradlew.bat :app:assembleFossDebug
```

- [ ] **Step 2: Run focused device smoke**

Open a complex snapshot containing native libraries, components, metadata, DEX/resources, moved items, and rule chips. Verify expand/collapse, detail click, reference long-click, chip dialog, report export, and new/deleted/empty states.

- [ ] **Step 3: Measure the final production and total net change**

```powershell
git diff --numstat
git diff --stat
git diff --check
```

Expected: at least 17 production files and the wrapper-only tests removed, with a substantial negative net line count and no whitespace errors.

