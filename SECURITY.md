# Security policy

## Supported versions

Until the first stable release, only the latest commit on `main` receives
security fixes.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting feature:

1. Open the repository's **Security** tab.
2. Select **Advisories**.
3. Select **Report a vulnerability**.

Include the affected revision, impact, reproduction steps or proof of concept,
and any suggested mitigation. Do not include real payment credentials or
customer data.

If private reporting is not enabled, open a minimal issue asking the maintainer
to enable it without disclosing vulnerability details.

You should receive an acknowledgement within seven days. Timelines for a fix and
coordinated disclosure depend on severity and maintainability.

## Scope warning

This repository is educational and is not production-ready. In particular, it
does not yet include authentication, authorization, tenant isolation, a
currency model, versioned database migrations, or complete operational controls.
Those documented limitations are not themselves vulnerabilities, but an
unexpected bypass or unsafe behavior in an implemented control is in scope.
