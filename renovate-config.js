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

  // Enable vulnerability alerts
  osvVulnerabilityAlerts: true,

  // Produce conventional-commit PR titles so validate-pr-title passes
  semanticCommits: 'enabled',

  // Extend with recommended config
  extends: ['config:recommended'],

  // Only manage Maven deps and GitHub Actions (leaves the `specification` submodule alone)
  enabledManagers: ['maven', 'github-actions'],

  packageRules: [
    {
      // GitHub Actions: pin third-party actions to a commit SHA for security.
      matchManagers: ['github-actions'],
      matchPackageNames: [
        '!actions/**', // Trust official GitHub actions (checkout, setup-java, etc.); don't pin.
      ],
      pinDigests: true,
    },
  ],
};
