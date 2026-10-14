module.exports = {
  platform: 'github',
  timezone: 'Australia/Brisbane',
  requireConfig: 'optional',
  onboarding: false,

  repositories: ['OctopusDeploy/openfeature-provider-java'],
  reviewers: ['team:team-devex'],
  branchPrefix: 'renovate/',

  // Limit concurrent PRs for better manageability
  prConcurrentLimit: 5,

  // Add labels to PRs
  labels: ['dependencies'],

  // Wait until a release has been published for at least 2 days before updating,
  // to avoid picking up versions that get yanked or hotfixed shortly after release.
  minimumReleaseAge: '2 days',

  // Enable vulnerability alerts
  osvVulnerabilityAlerts: true,

  // Produce conventional-commit PR titles so validate-pr-title passes
  semanticCommits: 'enabled',

  // Extend with recommended config
  globalExtends: ['config:recommended'],

  // Only manage Maven deps, the Maven Wrapper, and GitHub Actions (leaves the `specification` submodule alone)
  enabledManagers: ['maven', 'maven-wrapper', 'github-actions'],

  packageRules: [
    {
      // Hold Maven below 3.10. Under Maven 3.10, central-publishing-maven-plugin 0.8.0 adds local-repository files
      // to the bundle and Maven Central rejects it. CI doesn't catch this, because validation happens at publish time.
      // Remove once the plugin is upgraded and a Maven 3.10 bundle is confirmed clean. See BMBB-1251.
      matchManagers: ['maven-wrapper'],
      matchPackageNames: ['org.apache.maven:apache-maven'],
      allowedVersions: '<3.10.0',
    },
    {
      // OpenFeature SDK updates are worth releasing as a minor version, even if not breaking
      matchPackageNames: ['dev.openfeature:sdk'],
      matchUpdateTypes: ['minor', 'patch'],
      semanticCommitType: 'feat',
    },
    {
      // GitHub Actions: pin third-party actions to a commit SHA for security.
      matchManagers: ['github-actions'],
      matchPackageNames: [
        '!actions/**', // Trust official GitHub actions (checkout, setup-java, etc.); don't pin.
      ],
      pinDigests: true,
    },
    {
      // pin/pinDigest/digest updates have no release timestamp, so
      // minimumReleaseAge can never be satisfied and its stability check
      // would leave those PRs pending forever.
      // https://github.com/renovatebot/renovate/issues/40288
      matchUpdateTypes: ['pin', 'pinDigest', 'digest'],
      minimumReleaseAge: null,
      prBodyNotes: [
        '**Manual supply-chain check:** `minimumReleaseAge` cannot be enforced for pin/digest updates because they have no release timestamp. Before merging, confirm this commit SHA has been published for at least **2 days**.',
      ],
    },
  ],
};
