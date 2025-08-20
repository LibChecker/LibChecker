# GitHub Copilot Instructions for LibChecker

## Project Overview

LibChecker is an Android application written in Kotlin that analyzes installed applications to display information about their third-party libraries, native libraries, components (activities, services, receivers, providers), permissions, and other APK/HAP metadata. The app helps users understand the composition and architecture of Android apps on their device.

## Architecture & Patterns

### MVVM Architecture
- Use ViewModel classes to manage UI-related data
- Implement Repository pattern for data access
- Use LiveData/Flow for reactive data streams
- Follow Android Architecture Components best practices

### Package Structure
```
com.absinthe.libchecker/
├── annotation/          # Custom annotations
├── api/                # API-related classes, REST clients
├── app/                # Application-level components
├── compat/             # Compatibility helpers
├── constant/           # Application constants
├── data/               # Data models and entities
├── database/           # Room database components
├── features/           # Feature modules (MVVM organized)
│   ├── album/          # Snapshot comparison features
│   ├── applist/        # App listing and details
│   ├── chart/          # Statistics and charts
│   ├── home/           # Main dashboard
│   ├── settings/       # App settings
│   └── statistics/     # Statistics features
├── integrations/       # External service integrations
├── services/           # Android services
├── ui/                # Common UI components
├── utils/             # Utility classes
└── view/              # Custom views
```

### Key Libraries & Frameworks
- **Room**: Database persistence layer with DAO pattern
- **Retrofit + OkHttp**: Network requests with Moshi converter
- **Coroutines + Flow**: Asynchronous programming and reactive streams
- **Coil**: Image loading with custom transformations
- **Material Design Components**: UI framework following Material Design 3
- **ViewBinding**: Type-safe view references (no findViewById)
- **KSP**: Kotlin Symbol Processing for annotation processing
- **Protobuf**: Data serialization for efficient storage
- **Moshi**: JSON parsing with KSP code generation
- **Timber**: Structured logging
- **Firebase**: Crashlytics for crash reporting
- **Custom Libraries**: 
  - `LCRules`: Library rule matching system
  - `rulesbundle`: External rule repository integration

## Coding Conventions

### Kotlin Style
- Use ktlint for code formatting (configured in build.gradle.kts)
- Prefer `val` over `var` when possible
- Use data classes for simple data holders
- Leverage Kotlin extensions and higher-order functions
- Use nullable types appropriately with safe calls

### Android Specifics
- Use ViewBinding instead of findViewById
- Implement proper lifecycle awareness
- Handle configuration changes properly
- Use resource qualifiers for different screen sizes/orientations
- Follow Material Design guidelines

### File Naming
- Activities: `*Activity.kt` 
- Fragments: `*Fragment.kt`
- ViewModels: `*ViewModel.kt`
- Adapters: `*Adapter.kt`
- Custom Views: `*View.kt`
- Extensions: `*Extensions.kt`
- Constants: Use `object` declarations

### Resource Naming
- Layouts: `activity_*.xml`, `fragment_*.xml`, `item_*.xml`
- Drawables: `ic_*.xml` (icons), `bg_*.xml` (backgrounds)
- Colors: Use semantic naming (`colorPrimary`, `colorOnSurface`)
- Strings: Use descriptive keys with feature prefixes

## Common Development Patterns

### APK Analysis
When working with APK analysis features:
- Use `PackageUtils` for extracting package information
- Handle different ABI architectures (ARM, x86, etc.)
- Consider both APK and HAP (HarmonyOS) file formats
- Implement proper error handling for corrupted packages

### Library Detection
- Use `LCRules` for library rule matching
- Support regex patterns for dynamic library detection
- Handle both native libraries (.so files) and DEX classes
- Implement caching for performance

### UI Components
- Extend `BaseActivity` for common activity functionality
- Use `BottomSheetDialogFragment` for modal content
- Implement proper loading states and error handling
- Use RecyclerView with proper ViewHolder patterns

### Database Operations
- Use Repository pattern for database access (see `LCRepository`)
- All database operations should use Room DAOs (see `LCDao`)
- Use proper transaction handling with `@Transaction` annotations
- Handle database migrations properly in `LCDatabase`
- Use Flow for reactive database queries
- Implement proper entity relationships with foreign keys

### Networking
- Use Retrofit with coroutines for async operations
- Implement proper error handling and retry logic
- Support multiple backend endpoints (GitHub, GitLab for rules)
- Use `BaseUrlInterceptor` for dynamic URL switching
- Handle different API endpoints with proper timeouts
- Use proper HTTP caching when appropriate
- Support offline-first patterns with cached data

## Build System

### Gradle Configuration
- Multi-module project with build-logic module
- Uses version catalog (libs.versions.toml) for dependency management
- Supports multiple product flavors (foss, market)
- Implements R8/ProGuard for release builds
- Uses KSP for annotation processing

### Build Features
- ViewBinding enabled for type-safe view access
- BuildConfig generation for configuration constants
- AIDL support for system-level interactions
- Resource optimization for release builds
- Multi-flavor support:
  - **foss**: Open source version (default)
  - **market**: Version with additional market-specific features
- Firebase integration in market flavor
- Proguard/R8 obfuscation for release builds

## Testing Guidelines

### Unit Tests
- Test business logic in ViewModels and Repositories
- Mock external dependencies using Mockito or MockK
- Test data transformation and utility functions
- Use JUnit 5 where possible

### Integration Tests
- Test database operations with Room's testing utilities
- Test API integrations with mock servers
- Use Android Test framework for UI tests
- Test different Android versions and device configurations

## Security Considerations

### Permissions
- Request minimal necessary permissions
- Handle runtime permissions properly
- Provide clear rationale for permission requests
- Implement graceful degradation when permissions denied

### Data Handling
- Avoid logging sensitive information
- Use proper encryption for sensitive data storage
- Validate all external inputs
- Handle deep links securely

## Performance Guidelines

### Memory Management
- Use weak references for callbacks
- Implement proper ViewHolder recycling
- Avoid memory leaks in AsyncTasks and handlers
- Use appropriate image loading strategies

### Background Processing
- Use coroutines for background operations
- Implement proper cancellation handling
- Use appropriate dispatchers (IO, Default, Main)
- Avoid blocking the main thread

## Specific Features

### APK Components Analysis
- Parse AndroidManifest.xml for components using `PackageUtils`
- Extract and categorize different component types:
  - **NATIVE**: Native libraries (.so files) with ABI detection
  - **SERVICE**: Android services (background processes)
  - **ACTIVITY**: Activities (UI components)
  - **RECEIVER**: Broadcast receivers (event handlers)
  - **PROVIDER**: Content providers (data sharing)
  - **DEX**: Dalvik/ART classes and libraries
  - **PERMISSION**: Declared and used permissions
  - **METADATA**: Application metadata
- Handle system vs user apps differently
- Support both installed apps and external APK/HAP files
- Implement proper ABI architecture detection (ARM64, ARM32, x86, etc.)
- Handle split APKs and bundle installations

### Library Rules Integration
- Sync with remote rule repositories (GitHub/GitLab)
- Cache rules locally for offline access using Room database
- Support custom rule additions and modifications
- Handle rule updates and versioning automatically
- Use `LCRules.getRule()` for library identification
- Implement regex-based pattern matching for dynamic libraries
- Support different rule sources (official and community)
- Handle rule contributors and metadata properly

### Rules Database Structure
- Rules are stored in SQLite database via Room
- Support for library metadata: name, description, team, contributors
- Version tracking and update timestamps
- Icon resources and visual representations
- Source links and documentation references

### Statistics and Charts
- Use appropriate chart libraries for visualization
- Handle large datasets efficiently
- Implement proper data aggregation
- Support different time ranges and filters

## Contributing Guidelines

When contributing new features:
1. Follow the existing package structure
2. Add appropriate unit tests
3. Update documentation if needed
4. Follow the established coding conventions
5. Handle edge cases and error conditions
6. Consider backwards compatibility
7. Test on different Android versions
8. Ensure proper accessibility support

## Common Gotchas

- Android version compatibility - check API levels
- Different OEM customizations may affect package detection
- Handle both APK and HAP formats appropriately
- Be aware of scoped storage restrictions on newer Android versions
- Consider different screen densities and sizes
- Handle app updates and package changes properly