# 🧱 Architecture boundary sensor

`./gradlew verifyArchitecture testArchitectureGuard` checks the current module
graph and executable positive/negative fixtures. Root `check` runs both; module
`check` tasks also depend on the architecture scan. CI runs the same tasks.

The Gradle inventory records actual declared project and external dependencies,
including catalog aliases and convention dependencies. The Python standard
library scanner maps Kotlin imports to the physical production source module,
so moving an implementation symbol into a package called `api` cannot exempt it.
It also checks fully qualified Material3/platform uses and package aliases.
Comments, ordinary strings, raw strings and test sources do not create violations.

The policies protect feature API/implementation separation, foundation purity,
design system domain/storage separation, platform source sets, Material3 ownership
in `designsystem/nucleus`, and storage/workflow ownership in `*Screen.kt`,
`*Root.kt`, and `*ViewModel.kt`. A ViewModel cannot own `DraftStore` or
`CommandCoordinator` publish sequencing. Component tests in the design system
and workout desktop test sources run in the ordinary desktop CI test selection.

Inside the single design system module, physical Kotlin directories own the
atomic rank: `nucleus < atoms < molecules < organisms < templates`. Imports and
qualified references may point to the same or a lower rank. Wildcards, symbol
aliases and package aliases are checked; declaring a different package does not
change a known symbol's physical rank. Nucleus adapters therefore cannot call
atoms. Domain-free semantic variants and interaction-size policy live in nucleus.

Explicit ownership exceptions are structural: `composeApp` and `androidApp` are
composition roots; feature DI/controller factories and the generic core editor
controller may bridge storage; platform and test source sets may import platform
types. These are tested positive fixtures, with no arbitrary file allowlist.

This sensor is deliberately not a Kotlin parser, dataflow checker or API review.
It cannot follow reflection, generated declarations, aliases of runtime values,
or storage protocols hidden behind a newly invented interface. The compiler,
behavioral tests and architecture review cover those responsibilities.
