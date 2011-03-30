#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------------
#
# Shell script to create commons pool RCs.
# This script should be run from a fresh checkout of the RC tag.
#
# $Revision$ $Date$
# -----------------------------------------------------------------------------
# Set script variables
version=1.5.6
rc=2
repo_path=~/.m2/repository/commons-pool/commons-pool/${version}
release_path=~/pool-${version}-rc${rc}
#
# Delete any locally installed artifacts from previous runs
rm -rf ${repo_path}
echo "Cleaned maven repo."
rm -rf ${release_path}
echo "Cleaned local release directory"
mvn clean
#
# Generate the release artifacts and install them locally
mvn site
mvn -Prc -DcreateChecksum=true install
#
# Copy the zips/tarballs and release notes to the release directory
mkdir ${release_path}
cp ${repo_path}/*.zip ${release_path}
cp ${repo_path}/*.zip.* ${release_path}
cp ${repo_path}/*.gz ${release_path}
cp ${repo_path}/*.gz.* ${release_path}
cp RELEASE-NOTES.txt ${release_path}
#
# Copy site
cp -R target/site ${release_path}
#
# Copy maven artifacts
cp -R ${repo_path} ${release_path}
#
# Rename maven, site directories
mv ${release_path}/${version} ${release_path}/maven
mv ${release_path}/site ${release_path}/docs
echo "Artifacts copied."
#
# Delete tars/zips from maven subdirectory
rm ${release_path}/maven/*.zip
rm ${release_path}/maven/*.zip*
rm ${release_path}/maven/*.gz
rm ${release_path}/maven/*.gz*
#
# Create combined tarball
cd ${release_path}
cd ..
tar -czf ${release_path}.tgz pool-${version}-rc${rc}

echo "Release candidate complete"

