# Quick Start: Running Integration Test

This guide shows you how to quickly run the ExamplePlugin integration test.

## Prerequisites

1. RuneLite with ProjectX installed
2. OSRS account credentials
3. Maven installed

## Quick Start (2 Steps)

### Step 1: Run the Test

Choose your preferred method:

#### 🔹 Command Line
```bash
mvn test -DskipTests=false \
         -Dtest=ExamplePluginIntegrationTest \
         -Dtest.username=YOUR_USERNAME \
         -Dtest.password=YOUR_PASSWORD \
         -Dtest.isMember=true
```

#### 🔹 IntelliJ IDEA
1. Open `ExamplePluginIntegrationTest.java`
2. Remove the `@Ignore` annotation at line 42
3. Right-click → "Modify Run Configuration"
4. Add VM options:
   ```
   -Dtest.username=YOUR_USERNAME -Dtest.password=YOUR_PASSWORD -Dtest.isMember=true
   ```
5. Click "Run"

### Step 2: View Results

The test will automatically:
1. ✅ Create fresh profile `test-[username]`
2. ✅ Start RuneLite client (~5 seconds)
3. ✅ Delete any existing test profile (clean state)
4. ✅ Log into OSRS (~10-30 seconds)
5. ✅ Enable ExamplePlugin
6. ✅ Run performance metrics for 30 seconds
7. ✅ Verify results
8. ✅ Delete test profile (cleanup)
9. ✅ Shut down

**Total time:** ~1-2 minutes

Check the console output for:
```
[INFO] Performance Test Results:
[INFO]   Total GameObjects: 1234
[INFO]   Compositions retrieved: 1234
[INFO]   Time taken: 234 ms
[INFO]   Average time per object: 189 μs
```

✅ **Pass Criteria:** Test completes without errors

## Common Issues

### Issue: "No credentials provided"
**Solution:** Ensure you're passing both username and password:
```bash
# Correct:
mvn test -DskipTests=false \
         -Dtest=ExamplePluginIntegrationTest \
         -Dtest.username=myuser \
         -Dtest.password=mypass

# Wrong (missing password):
mvn test -Dtest=ExamplePluginIntegrationTest -Dtest.username=myuser
```

### Issue: "Tests are skipped"
**Solution:** Add `-DskipTests=false` to your maven command

### Issue: "Login timeout"
**Possible causes:**
- Incorrect credentials
- OSRS servers down
- Network issues
- 2FA/Authenticator required (not supported in automated tests)

**Solution:**
- Verify credentials manually first
- Check OSRS server status
- Disable authenticator for test account (if safe to do so)

### Issue: "Client not initialized"
**Solution:** Increase wait time in test:
```java
// In setUp() method, increase from 5000 to 10000:
Thread.sleep(10000); // Give client more time to initialize
```

## Security Notes

⚠️ **IMPORTANT:**
- Never commit credentials to git
- Use environment variables for CI/CD:
  ```bash
  export TEST_USERNAME="username"
  export TEST_PASSWORD="password"
  mvn test -Dtest.username=$TEST_USERNAME -Dtest.password=$TEST_PASSWORD
  ```
- For CI, use GitHub Secrets or similar
- Test accounts should be separate from main accounts

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'

    - name: Run Integration Tests
      env:
        TEST_USERNAME: ${{ secrets.OSRS_TEST_USERNAME }}
        TEST_PASSWORD: ${{ secrets.OSRS_TEST_PASSWORD }}
      run: |
        mvn test -DskipTests=false \
                 -Dtest=ExamplePluginIntegrationTest \
                 -Dtest.username=$TEST_USERNAME \
                 -Dtest.password=$TEST_PASSWORD \
                 -Dtest.isMember=false
```

## What's Being Tested?

The integration test validates:

1. **Client Startup** - RuneLite initializes correctly
2. **Profile Loading** - Credentials are loaded and encrypted properly
3. **Login System** - LoginManager.login() works with profiles
4. **Plugin Lifecycle** - Plugin starts and runs without errors
5. **Performance** - GameObject composition caching provides speedup
6. **Cleanup** - Resources are properly released

## Next Steps

- Run unit tests first: `mvn test -Dtest=ExamplePluginPerformanceTest`
- Check logs in: `~/.runelite/logs/`
- Modify test timeout if needed
- Add more assertions for your specific use case

## Support

For issues:
1. Check the [README.md](README.md) for detailed documentation
2. Review the test code comments
3. Check ProjectX logs: `~/.runelite/logs/projectx.log`
4. Ask in ProjectX Discord/Issues
