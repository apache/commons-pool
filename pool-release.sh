#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------------
# Performs the local svn steps necessary to publish a pool release.
#
# Preconditions:
# 0) Successful release VOTE has completed, based on artifacts in rc_path
#    (checkout of https://dist.apache.org/repos/dist/dev/commons/pool)
# 1) release_path points to a local checkout of
#     https://dist.apache.org/repos/dist/release/commons/pool
# 2) RELEASE-NOTES.txt for the new release is in top level of rc_path
#
# NOTE: This script does not commit the local changes to actually publish
# the artifacts.
# 
# -----------------------------------------------------------------------------
# Set script variables
version=2.12.0        # version being released
last_version=2.11.1   # previous version, will be replaced in README.html
rc_name=2.12.0-RC5    # subdirectory of /dist/dev/commons/pool containing final RC
rc_path=${HOME}/pool-tag/commons-pool/target/commons-release-plugin/scm/${rc_name}  # checkout of https://dist.apache.org/repos/dist/dev/commons/pool
release_path=${HOME}/pool #checkout of https://dist.apache.org/repos/dist/release/commons/pool
#
# Move release notes
cp $rc_path/RELEASE-NOTES.txt $release_path
#
# Update README.html
# sed -i "" "s/$last_version/$version/g" $release_path/README.html
#   OSX  ^^ required suffix
sed -i "s/$last_version/$version/g" $release_path/README.html
#
# Move release artifacts
svn mv $rc_path/source/*${version}* $release_path/source
svn mv $rc_path/binaries/*${version}* $release_path/binaries
#
# Drop artifacts from the last release
cd $release_path/source
for file in *${last_version}*; do svn rm $file; done
cd $release_path/binaries
for file in *${last_version}*; do svn rm $file; done
#
# Drop RC directory
svn rm $rc_path

echo "Local svn changes complete."
echo "Inspect the files in $release_path and commit to publish the release."
echo "Also make sure that the rc_name directory in the /dist/dev checkout has been removed."




