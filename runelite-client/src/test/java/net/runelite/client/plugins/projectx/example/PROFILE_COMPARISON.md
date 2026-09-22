# ⚠️ DEPRECATED - This file is no longer relevant

## Note

This document previously described three different methods for passing profiles to the integration test.

**The test has been simplified to use only one method:**
- Pass credentials via system properties (`-Dtest.username` and `-Dtest.password`)
- The test automatically creates a fresh profile named `test-[username]`
- Profiles are automatically deleted after the test completes

## Current Documentation

Please refer to:
- [README.md](README.md) - Complete documentation
- [INTEGRATION_TEST_GUIDE.md](INTEGRATION_TEST_GUIDE.md) - Quick start guide

## Simple Usage

```bash
mvn test -DskipTests=false \
         -Dtest=ExamplePluginIntegrationTest \
         -Dtest.username=YOUR_USERNAME \
         -Dtest.password=YOUR_PASSWORD \
         -Dtest.isMember=true
```

That's it! No profile setup required. The test handles everything automatically.

## Why the Change?

The previous multi-option approach added unnecessary complexity. The simplified approach:
- ✅ Ensures clean test state every time
- ✅ No manual profile setup required
- ✅ Automatic cleanup after tests
- ✅ Works consistently across different environments
- ✅ Easier to understand and maintain
- ✅ Better for CI/CD pipelines

---

*This file is kept for historical reference only. Last updated: 2025-11-11*
