#!/bin/bash
cd /Users/bian/faculty/printscript-tck

./gradlew test --tests "*line-break-after-statement-enforced*1.0*" 2>&1 | grep -A 10 "ComparisonFailure"

