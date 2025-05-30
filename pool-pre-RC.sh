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
#
# Shell script to update download page and release notes prior
# to preparing a commons pool release candidate.
#
# Note: RELEASE-NOTES.txt may need a little reformatting prior
# to checkin.  Both RELEASE-NOTES.txt and the generated download
# page need to be checked in after review.
#
# ----------------------------------------------------------------------------
version=2.4.3
mvn changes:announcement-generate -Prelease-notes -Dchanges.version=${version}
mvn commons:download-page -Dcommons.componentid=pool -Dcommons.release.version=${version}
