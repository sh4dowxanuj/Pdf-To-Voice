# Contributing to PDF to Voice

We love your input! We want to make contributing to PDF to Voice as easy and transparent as possible, whether it's:

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing new features
- Becoming a maintainer

## Development Process

We use GitHub to host code, to track issues and feature requests, as well as accept pull requests.

## Pull Requests

Pull requests are the best way to propose changes to the codebase. We actively welcome your pull requests:

1. Fork the repo and create your branch from `main`.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code lints.
6. Issue that pull request!

## Code Style

### Kotlin Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and function names

### Git Commit Messages
- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

Example:
```
Add user profile editing functionality

- Add ProfileEditFragment with form validation
- Update UserRepository with update methods
- Add tests for profile update flow
- Fixes #123
```

## Setting Up Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/pdf-to-voice.git
   cd pdf-to-voice
   ```

2. **Set up Firebase**
   - Create a Firebase project for development
   - Add your `google-services.json` file
   - See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for details

3. **Open in Android Studio**
   - Import the project
   - Sync Gradle files
   - Run the app to ensure everything works

## Testing

### Unit Tests
```bash
./gradlew test
```

### UI Tests
```bash
./gradlew connectedAndroidTest
```

### Lint Checks
```bash
./gradlew lint
```

## Issue Reporting

We use GitHub issues to track public bugs. Report a bug by [opening a new issue](https://github.com/your-username/pdf-to-voice/issues/new).

**Great Bug Reports** tend to have:

- A quick summary and/or background
- Steps to reproduce
  - Be specific!
  - Give sample code if you can
- What you expected would happen
- What actually happens
- Notes (possibly including why you think this might be happening, or stuff you tried that didn't work)

## Feature Requests

We love feature requests! Please provide:

- **Use case**: Why do you need this feature?
- **Proposed solution**: How should it work?
- **Alternatives considered**: What other approaches did you consider?

## Code Review Process

The core team looks at Pull Requests on a regular basis. After feedback has been given we expect responses within two weeks. After two weeks we may close the pull request if it isn't showing any activity.

## Security Issues

If you discover a security vulnerability, please send an email to [security@yourproject.com] instead of opening a public issue.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Questions?

Don't hesitate to reach out! You can:
- Open an issue for bugs or feature requests
- Start a discussion for questions
- Contact the maintainers directly

Thank you for contributing! 🎉
