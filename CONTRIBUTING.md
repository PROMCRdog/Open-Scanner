# Contributing to Open Scanner

Thank you for helping build a trustworthy Wi-Fi tool.

## Ground rules

- Keep the stock-app boundary passive and defensive. Do not add password attacks, deauthentication, packet injection, stealth scanning, automatic router configuration, or identifier uploads.
- Treat SSIDs, BSSIDs, IP addresses, aliases, and screenshots as sensitive. Use synthetic fixtures in tests and public issues.
- Preserve the single scan coordinator. A screen must not register its own scan receiver, timer, or `startScan()` loop.
- Every derived claim must expose freshness, missing evidence, and limitations.
- Keep controls labelled, keyboard/TalkBack reachable, at least 48 dp, and usable at large font scales.
- Add tests for channel/security/privacy logic and update the relevant decision record when behavior or scope changes.

## Local checks

```bash
./gradlew --dependency-verification=strict test :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew --dependency-verification=strict :app:lintRelease :app:assembleRelease
```

For device work, document the model, Android version, permission state, and whether Android returned new or cached results. Redact all captured identifiers before committing evidence.

## Commit style

Use small, reviewable commits with an imperative summary, for example `feat: add channel filter`. If commit-email privacy matters to you, enable GitHub's private-email setting before creating commits.

Do not commit APKs, AABs, signing keys, signing configuration, private signing procedures, IDE state, machine-specific paths, or private device captures.

## Public workflow

1. Search existing issues and pull requests before starting work.
2. For a non-trivial change, open an issue describing the problem, privacy impact, and proposed behavior.
3. Create a focused branch in your fork and include tests or documentation updates with the change.
4. Run the local checks above and inspect the staged diff for sensitive data.
5. Open a pull request using the repository template and respond to review feedback.

Pull requests should explain user-visible behavior, Android-version assumptions, privacy or permission changes, and the evidence used to validate the result. Screenshots and fixtures must use synthetic identifiers.

Security vulnerabilities follow [SECURITY.md](SECURITY.md), not the public issue workflow.
