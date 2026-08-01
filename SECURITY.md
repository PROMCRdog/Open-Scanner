# Security policy

## Supported version

Security fixes currently target the latest `0.1.x` source on the default branch.

## Reporting

Report suspected vulnerabilities through a [private GitHub security advisory](https://github.com/PROMCRdog/Open-Scanner/security/advisories/new). Do not open a public issue for a vulnerability or publish real network identifiers, device dumps, credentials, or exploit details.

Include the affected commit, Android version, device class, permission state, and a minimal reproduction. Replace SSIDs, BSSIDs, IP addresses, gateways, DNS servers, and timestamps with synthetic values.

The maintainers will acknowledge reports on a best-effort basis, coordinate a fix and disclosure window when the report is accepted, and credit reporters who request attribution. Please do not test beyond the minimum needed to demonstrate the issue.

## In-scope concerns

- Identifier leakage through UI semantics, logs, exceptions, backup, clipboard, or exports.
- Export redaction bypass or temporary-file retention.
- Unexpected network traffic or new dangerous permissions.
- Components exported unintentionally.
- Dependency or build-integrity compromise.
- A path that violates the passive safety boundary.

Do not test against networks or devices you do not own or have explicit authorization to assess.
