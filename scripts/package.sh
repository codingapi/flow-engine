#!/usr/bin/env bash

cd ../frontend
pnpm i
pnpm run build
pnpm run build:app-pc


rm -rf ../flow-engine-example/src/main/resources/static
mkdir -p ../flow-engine-example/src/main/resources/static

cp -r apps/app-pc/dist/* ../flow-engine-example/src/main/resources/static/

cd ../
mvn clean package -DskipTests
cp ./flow-engine-example/target/*.jar ./scripts/server.jar