#!/bin/bash
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
#
# Shell script to update download page and release notes prior
# to preparing a commons pool release candidate.
#
# Notes:
# 1. RELEASE-NOTES.txt may need a little reformatting prior to commit
# 2. Both RELEASE-NOTES.txt and the generated download
#    page need to be checked in after review.
# ----------------------------------------------------------------------------
# Update release notes
version=2.12.0
# Pop off any previously generated current release content first.
first_line=`head -1 RELEASE-NOTES.txt`
target="Apache Commons Pool $version RELEASE NOTES"
if [[ $first_line =~ $target ]]
then
    echo "Removing previously generated content for $version from RELEASE-NOTES.txt"
    sed -i '1,/------------------------------------------------/d' RELEASE-NOTES.txt
fi
# Make a copy of previous release notes to prepend the new release notes to
cp RELEASE-NOTES.txt RELEASE-NOTES.txt.orig
# Generate new release notes - only generates new content and replaces RELEASE-NOTES.txt with new content
mvn changes:announcement-generate -Prelease-notes -Dchanges.version=${version}
# Put Humpty back together again
cat RELEASE-NOTES.txt RELEASE-NOTES.txt.orig > RELEASE-NOTES.txt.new
mv RELEASE-NOTES.txt.new RELEASE-NOTES.txt
rm RELEASE-NOTES.txt.orig
# Generate the download page
mvn commons-build:download-page -Dcommons.componentid=pool -Dcommons.release.version=${version}
