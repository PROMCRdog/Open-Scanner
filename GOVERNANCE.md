# Governance

Open Scanner currently uses a maintainer-led governance model with public, evidence-based decisions.

## Roles

- **Contributors** report issues, improve documentation, submit code, test changes, and participate in review.
- **Maintainers** triage work, protect the product's privacy and passive-safety boundaries, review contributions, manage releases, and administer the repository.

Consistent, constructive contributions may lead to additional maintenance responsibility. Access is granted gradually and can be removed when it is no longer needed or project safeguards are not followed.

## Decisions

Routine changes are decided through issue and pull-request review. Changes to architecture, privacy behavior, permissions, safety boundaries, compatibility, or public APIs require an ADR or an update to an existing decision record.

Maintainers seek practical consensus, but may reject or defer changes that are unsupported, unsafe, outside scope, or too costly to maintain. Security-sensitive discussion remains private until coordinated disclosure is safe.

## Releases

The default branch is development source, not a release channel. A release requires completed release checks, an annotated version tag, published checksums, and an artifact signed through the private maintainer process. Signing keys, credentials, and private release procedures are never accepted in issues or pull requests.

Project policies may evolve through a reviewed pull request that explains the reason and migration impact.
