#!/bin/bash
# Master Test Script
# Runs all service-level tests + Infrastructure check

echo "🚀 SYSTEM-WIDE TEST SUITE"
echo "========================="

# Permissions
chmod +x scripts/tests/*.sh

# 1. Unit Tests (Code Level) - Fast
./scripts/tests/test_intelligence.sh
if [ $? -ne 0 ]; then
    echo "❌ Aborting: Intelligence Tests Failed"
    exit 1
fi

./scripts/tests/test_java_backend.sh
if [ $? -ne 0 ]; then
    echo "❌ Aborting: Java Tests Failed"
    exit 1
fi

./scripts/tests/test_ui.sh
if [ $? -ne 0 ]; then
    echo "❌ Aborting: UI Tests Failed"
    exit 1
fi

# 2. Infrastructure Tests (Runtime Level) - Slow
# Optional: Uncomment if you want to enforce running environment check
# ./scripts/tests/test_infra.sh
# if [ $? -ne 0 ]; then
#     echo "❌ Aborting: Infrastructure Unhealthy"
#     exit 1
# fi

echo
echo "✅✅✅ ALL TESTS PASSED SUCCESSFULLY! ✅✅✅"
exit 0
