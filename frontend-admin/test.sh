#!/bin/bash

# 运行所有测试
echo "Running all tests..."
npm run test

# 如果指定了 --coverage 参数，则运行覆盖率测试
if [ "$1" = "--coverage" ]; then
  echo "Running coverage tests..."
  npm run test:coverage
fi 