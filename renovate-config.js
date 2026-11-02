module.exports = {
  extends: ['github>OctopusDeploy/team-bmbb-renovate-config:default.json5'],

  // Produce conventional-commit PR titles so validate-pr-title passes
  semanticCommits: 'enabled',

  // Only manage Maven deps and GitHub Actions (leaves the `specification` submodule alone)
  enabledManagers: ['maven', 'github-actions'],

  packageRules: [
    {
      // OpenFeature SDK updates are worth releasing as a minor version, even if not breaking
      matchPackageNames: ['dev.openfeature:sdk'],
      matchUpdateTypes: ['minor', 'patch'],
      semanticCommitType: 'feat',
    },
  ],
};
