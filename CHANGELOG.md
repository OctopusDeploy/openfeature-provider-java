# Changelog

## [2.0.1](https://github.com/OctopusDeploy/openfeature-provider-java/compare/v2.0.0...v2.0.1) (2026-08-20)


### Bug Fixes

* **deps:** update dependency com.fasterxml.jackson.core:jackson-databind to v2.22.2 ([#61](https://github.com/OctopusDeploy/openfeature-provider-java/issues/61)) ([ebdd62b](https://github.com/OctopusDeploy/openfeature-provider-java/commit/ebdd62b07af77655321fa2d3bf6b85efaa027808))

## [2.0.0](https://github.com/OctopusDeploy/openfeature-provider-java/compare/v1.0.0...v2.0.0) (2026-08-13)


### ⚠ BREAKING CHANGES

* align type and method names with the other provider libraries ([#55](https://github.com/OctopusDeploy/openfeature-provider-java/issues/55))

### Features

* Add support for upcoming rules-based evaluations ([207dc70](https://github.com/OctopusDeploy/openfeature-provider-java/commit/207dc70a4ee19e991886af6c453551147b962ca1))
* add v4 evaluation response types to provider library ([#52](https://github.com/OctopusDeploy/openfeature-provider-java/issues/52)) ([7e12fa7](https://github.com/OctopusDeploy/openfeature-provider-java/commit/7e12fa74437111b5f54c07f0b3cb32ce265ea3aa))
* **deps:** update dependency dev.openfeature:sdk to v1.21.0 ([#39](https://github.com/OctopusDeploy/openfeature-provider-java/issues/39)) ([8cdd52e](https://github.com/OctopusDeploy/openfeature-provider-java/commit/8cdd52e53ff7b5766958ea6a5502f9d7be56b4a9))
* **deps:** update dependency dev.openfeature:sdk to v1.22.0 ([#48](https://github.com/OctopusDeploy/openfeature-provider-java/issues/48)) ([dc3bf95](https://github.com/OctopusDeploy/openfeature-provider-java/commit/dc3bf952f7839bf9aaee53bae2b9dbd5e2a718d3))
* implement v4 client-side evaluation ([#51](https://github.com/OctopusDeploy/openfeature-provider-java/issues/51)) ([1f9103f](https://github.com/OctopusDeploy/openfeature-provider-java/commit/1f9103fe0d4fb4774bb3bed435ce866ca7d9359b))


### Bug Fixes

* **deps:** update dependency com.fasterxml.jackson.core:jackson-databind to v2.21.4 [security] ([#24](https://github.com/OctopusDeploy/openfeature-provider-java/issues/24)) ([8ff6288](https://github.com/OctopusDeploy/openfeature-provider-java/commit/8ff628897e4995d20a573d4ba4975a1a61162eeb))
* **deps:** update dependency com.fasterxml.jackson.core:jackson-databind to v2.21.5 [security] ([#33](https://github.com/OctopusDeploy/openfeature-provider-java/issues/33)) ([8168a03](https://github.com/OctopusDeploy/openfeature-provider-java/commit/8168a0366fbad21265a63a61a1e86558794b90f0))
* **deps:** update dependency com.fasterxml.jackson.core:jackson-databind to v2.22.1 ([#37](https://github.com/OctopusDeploy/openfeature-provider-java/issues/37)) ([bf9056e](https://github.com/OctopusDeploy/openfeature-provider-java/commit/bf9056e09a848eb17c32fa2889306a6988760477))
* **deps:** update dependency commons-codec:commons-codec to v1.22.0 ([#38](https://github.com/OctopusDeploy/openfeature-provider-java/issues/38)) ([e79cc55](https://github.com/OctopusDeploy/openfeature-provider-java/commit/e79cc55ccb62fb0d8badeef1642eb03c7c148229))
* **deps:** update dependency commons-codec:commons-codec to v1.22.1 ([#47](https://github.com/OctopusDeploy/openfeature-provider-java/issues/47)) ([4b4a548](https://github.com/OctopusDeploy/openfeature-provider-java/commit/4b4a548861519750aecc0e109f234297af8ea02a))


### Code Refactoring

* align type and method names with the other provider libraries ([#55](https://github.com/OctopusDeploy/openfeature-provider-java/issues/55)) ([27ea72d](https://github.com/OctopusDeploy/openfeature-provider-java/commit/27ea72da2dfa47961bc26c84689e49c3ea88443c))

## [1.0.0](https://github.com/OctopusDeploy/openfeature-provider-java/compare/0.3.0...v1.0.0) (2026-06-15)


### ⚠ BREAKING CHANGES

* Return correct errors for unsupported flag types ([#17](https://github.com/OctopusDeploy/openfeature-provider-java/issues/17))
* Send product metadata in custom header ([#13](https://github.com/OctopusDeploy/openfeature-provider-java/issues/13))

### Features

* Send product metadata in custom header ([#13](https://github.com/OctopusDeploy/openfeature-provider-java/issues/13)) ([d0bb351](https://github.com/OctopusDeploy/openfeature-provider-java/commit/d0bb35175fd46735ad4ce8d1fe4a53927d1c9dcd))


### Bug Fixes

* Fallback to existing evaluation context on failed refresh ([#14](https://github.com/OctopusDeploy/openfeature-provider-java/issues/14)) ([d7a1aff](https://github.com/OctopusDeploy/openfeature-provider-java/commit/d7a1affdcb990b40d17898d68d85a4c932b6d8fe))
* Return correct errors for unsupported flag types ([#17](https://github.com/OctopusDeploy/openfeature-provider-java/issues/17)) ([d95b5fa](https://github.com/OctopusDeploy/openfeature-provider-java/commit/d95b5fad88dfaa26a828832d7c1e64c01241a35f))
* Simplify refresh logic on a failed fetch ([#16](https://github.com/OctopusDeploy/openfeature-provider-java/issues/16)) ([e4c0e78](https://github.com/OctopusDeploy/openfeature-provider-java/commit/e4c0e78bf49353435c2cd013b94bdbe9d934eea1))
