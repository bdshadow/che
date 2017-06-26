/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.vfs.watcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Watches directories for interactions with their entries. Based on
 * {@link WatchService} that uses underlying filesystem implementations. Does
 * not perform any data modification (including filesystem items) except for
 * tracking and notification the upper layers. Service operates with ordinary
 * java file system paths in counter to che virtual file system which may have
 * custom root element and structure. Transforming one we of path representation
 * into another and backwards is the responsibility of upper services.
 */
@Singleton
public class FileWatcherExcludePatternsRegistry {
    private List<PathMatcher> excludes;
    private List<PathMatcher> includes;

    @Inject
    public FileWatcherExcludePatternsRegistry(@Named("che.user.workspaces.storage.excludes") Set<PathMatcher> excludes) {
        this.excludes = new ArrayList<>(excludes);
        this.includes = new ArrayList<>();
    }

    public void addExcludeMatcher(PathMatcher matcher) {
        excludes.add(matcher);
    }

    public void removeExcludeMatcher(PathMatcher matcher) {
        excludes.remove(matcher);
    }

    public void addIncludeMatcher(PathMatcher matcher) {
        includes.add(matcher);
    }

    public void removeIncludeMatcher(PathMatcher matcher) {
        includes.remove(matcher);
    }

    public boolean isExcluded(Path path) {
        if (isIncluded(path)) {
            return false;
        } else {
            for (PathMatcher matcher : excludes) {
                if (matcher.matches(path)) {
                    return true;
                }
            }
            return false;
        }

    }

    public boolean isIncluded(Path path) {
        for (PathMatcher matcher : includes) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }
}
