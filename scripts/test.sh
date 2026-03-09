#!/usr/bin/env bash

cd ../frontend
pnpm i
pnpm run test

cd ../

mvn clean test -P travis