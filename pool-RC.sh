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
# Generates a pool RC and publishes (a superset of) maven artifacts to Nexus.
# Should be run from top-level directory of a fresh checkout of the RC tag.
#
# Preconditions:
# 0) pool-pre-RC has been run to update the download page and release notes
#    and these have been checked in and included in the RC tag.
# 1) Release artifacts from previous runs have been svn deleted from local
#    svn pub/sub dev checkout.
# 2) Nexus repo from previous RC has been dropped.
#
# Set script variables
version=2.4.3
repo_path=~/.m2/repository/org/apache/commons/commons-pool2/${version}
release_path=~/pool-rc  #checkout of https://dist.apache.org/repos/dist/dev/commons/pool
#
# Delete any locally installed artifacts from previous runs
rm -rf ${repo_path}
echo "Cleaned maven repo."
#
# Generate site and release artifacts, deploy locally and upload to Nexus
mvn clean site
mvn deploy -Prelease
#
# Copy the zips/tarballs and release notes to the local svn pub path
cp ${repo_path}/*bin.zip* ${release_path}/binaries
cp ${repo_path}/*bin.tar.gz* ${release_path}/binaries
cp ${repo_path}/*src.zip* ${release_path}/source
cp ${repo_path}/*src.tar.gz* ${release_path}/source
cp RELEASE-NOTES.txt ${release_path}

echo "Release candidate complete."
echo "svn add the generated artifacts and commit after inspection."
echo "log in to repository.apache.org, manually (sic) drop the cruft and close the repo."



