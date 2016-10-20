/*
 * Copyright 2016 Yodle, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yodle.vantage.component.service;

import static com.yodle.vantage.component.service.MavenVersionUtils.isMavenStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yodle.vantage.component.dao.VersionDao;
import com.yodle.vantage.component.domain.VersionId;

@Component
public class PrecedenceFixer {
    @Autowired private VersionDao versionDao;

    public void fixPrecedence(Collection<VersionId> versions) {
        for (VersionId v : versions) {
            fixPrecendence(v);
        }
    }

    public void fixPrecendence(VersionId version) {
        //get all known versions
        List<String> versions = versionDao.getVersions(version.getComponent());

        //determine insertion strategy
        boolean standard = isMavenStrategy(versions);

        if (standard) {
            List<ComparableVersion> comparableVersions = versions.stream().map(ComparableVersion::new).collect(Collectors.toList());
            TreeSet<ComparableVersion> cvTree = new TreeSet<>();
            cvTree.addAll(comparableVersions);

            ComparableVersion inserted = new ComparableVersion(version.getVersion());

            SortedSet<ComparableVersion> allPrev = cvTree.headSet(inserted);
            String prev = allPrev.size() > 0 ? allPrev.last().toString() : null;
            SortedSet<ComparableVersion> allNext = cvTree.tailSet(inserted, false);
            String next = allNext.size() > 0 ? allNext.first().toString() : null;

            versionDao.insertPrecendenceBetween(version.getComponent(), version.getVersion(), prev, next);

        } else {
            versionDao.insertPrecedenceAtEnd(version.getComponent(), version.getVersion());
        }
    }

}
