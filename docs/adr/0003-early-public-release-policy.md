# ADR 0003: Early public release assurance levels

Date: 2026-08-01
Status: accepted

## Context

The original v0.1 plan treated an early open-source release and broad compatibility certification as the same milestone. That made an exhaustive multi-device, accessibility, performance, and state matrix a prerequisite for publishing any stable tag, even when the signed candidate had already passed automated privacy/build gates and bounded physical-device testing.

Open Scanner needs a release policy that permits transparent early distribution without turning incomplete validation into a false claim of completion.

## Decision

Use two assurance levels.

### Early public release gate

An early public release may be published when all of these blocking requirements pass:

- the public source is clean and excludes private captures, signing material, secrets, and machine-specific paths;
- strict dependency verification, JVM tests, instrumentation-test assembly, release lint, and release assembly pass in CI;
- the permission/component audit finds no unexpected sensitive permission or `INTERNET` access, and there is no known critical privacy or security defect;
- default report redaction and the explicit unredacted warning/preview/share boundary have automated coverage;
- a permanent maintainer identity signs a non-debuggable APK, and the release publishes its source revision, APK hash, certificate hash, and signature schemes;
- the exact signed candidate installs and cold-launches on at least one maintainer-authorized physical device, with the core redacted and explicit-unredacted report flows exercised;
- known OEM/tooling exceptions and the incomplete compatibility scope are disclosed in the checklist, evidence record, and release notes; and
- a maintainer explicitly accepts the residual risk and authorizes publication.

A failure in any item above still blocks publication.

### Compatibility certification

The broader API 26–36 device matrix, multiple hardware classes, permission and radio-state permutations, every destination/display mode, TalkBack, Switch Access, large text, RTL, rotation/multi-window, performance, battery, external traffic capture, upgrade/migration, and reproducibility audits remain required for a future compatibility-certified milestone.

These items are a visible post-release validation backlog for v0.1.0. They must not be described as passed until evidence exists, but their incomplete status does not block an explicitly scoped early public release.

## Consequences

- v0.1.0 release notes must call the build an early public release and state its tested scope.
- The exact device-tested RC may be promoted unchanged so the final APK remains byte-for-byte identical to the installed artifact.
- Privacy, security, source hygiene, signing identity, artifact integrity, and green automated checks are not weakened.
- Broad device compatibility and accessibility certification cannot be claimed for v0.1.0.
- Remaining matrix work stays public and can be completed incrementally without rewriting the historical release evidence.
