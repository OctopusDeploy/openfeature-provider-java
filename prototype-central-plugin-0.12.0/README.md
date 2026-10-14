# PROTOTYPE: central-publishing-maven-plugin 0.12.0 (throwaway, do not merge)

**Question (BMBB-1251):** Does plugin 0.12.0 with `skipUpload=true` build a clean `central-bundle.zip`, without uploading, on Maven 3.10?

**Run:** `./prototype-central-plugin-0.12.0/run.sh` from the repo root. It patches `pom.xml` for each scenario, builds with `-Dgpg.skip=true -DskipTests`, and diffs the bundle's file list against the Oct 7 bundle that validated on Central (`known-good-oct7-unsigned.txt`, from CI run 37576757642 with `.asc` removed).

## Result (2026-10-15, local, JDK 25)

| Scenario | Bundle | vs known-good |
| -- | -- | -- |
| 0.8.0 `skipPublishing`, Maven 3.10.0 (control) | 26 files | +`maven-metadata-local.xml`, +`_remote.repositories` and its 4 checksums (reproduces the CI failure) |
| 0.12.0 `skipUpload`, Maven 3.9.16 | 20 files | identical |
| 0.12.0 `skipUpload`, Maven 3.10.0 | 20 files | identical |
| 0.12.0 `skipPublishing`, Maven 3.10.0 | none | "No files to stage", same as #34 on 0.11.0 |

**Verdict:** Yes. The fix is a bump to 0.12.0 plus renaming `skipPublishing` to `skipUpload` in `pom.xml`. The bump alone isn't enough, because `skipPublishing` still skips staging. After the change the bundle is clean on Maven 3.10, so the Maven pin is no longer needed for correctness.

**Also found:** with `skipUpload`, the build passes with no `central` server in settings, so the `server-id`/`server-username`/`server-password` placeholders in `setup-java` look removable. (0.8.0 and 0.12.0 `skipPublishing` both fail without them.)

**Not tested:** GPG-signed builds (the four `.asc` files) and whether Central validates the bundle. Those need CI plus an Octopus Development deployment.
