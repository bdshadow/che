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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.RegisteredProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.toList;
import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

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
public class FileWatcherExcludesFileTracker {
    private static final Logger LOG                             = LoggerFactory.getLogger(ProjectManager.class);
    private static final String FILE_WATCHER_EXCLUDES_FILE_NAME = "fileWatcherExcludes";
    private static final String FILE_WATCHER_EXCLUDES_FILE_PATH = "/" + CHE_DIR + "/" + FILE_WATCHER_EXCLUDES_FILE_NAME;

    private Map<Path, Set<Path>> excludes;
    private FileWatcherManager   fileWatcherManager;
    private ProjectManager       projectManager;
    private int                  excludesFileWatchingOperationID;

    @Inject
    public FileWatcherExcludesFileTracker(FileWatcherManager fileWatcherManager,
                                          ProjectManager projectManager) {
        this.fileWatcherManager = fileWatcherManager;
        this.projectManager = projectManager;
        this.excludes = new HashMap<>();
    }

    @PostConstruct
    public void initialize() {
        LOG.error("=== initialize FileWatcherExcludesFileTracker ");
        startTrackingExcludesFile();
        readFromIgnoreFiles();
        addFileWatcherExcludesFromIgnoreFile();
    }

    private void readFromIgnoreFiles() {
        List<RegisteredProject> projects = null;
        try {
            projects = projectManager.getProjects();
        } catch (ServerException e) {
            LOG.error(e.getLocalizedMessage());
        }

        if (projects == null) {
            return;
        }

        LOG.error("=== read from ignore files, project size " + projects.size());

        for (RegisteredProject project : projects) {
            try {
                FolderEntry baseFolder = project.getBaseFolder();
                if (baseFolder == null) {
                    continue;
                }

                String baseFolerPath = toNormalPath(baseFolder.getPath().toString());

                String fileWatcherIgnoreFileLocation = baseFolerPath + FILE_WATCHER_EXCLUDES_FILE_PATH;
                Path fileWatcherIgnoreFilePath = Paths.get(fileWatcherIgnoreFileLocation);
                boolean isFileWatcherIgnoreFileExists = Files.exists(fileWatcherIgnoreFilePath);

                if (!isFileWatcherIgnoreFileExists) {
                    continue;
                }

                handleModify(fileWatcherIgnoreFilePath.toString());

            } catch (Exception e) {
                LOG.error("=== ERROR when reading projects ");
            }
        }
    }

    private void startTrackingExcludesFile() {
        fileWatcherManager.addIncludeMatcher(getExcludesFileMatcher());

        excludesFileWatchingOperationID = fileWatcherManager.registerByMatcher(getExcludesFileMatcher(),
                                                                               getCreateConsumer(),
                                                                               getModifyConsumer(),
                                                                               getDeleteConsumer());
    }

    private PathMatcher getExcludesFileMatcher() {
        return path -> !isDirectory(path) &&
                       FILE_WATCHER_EXCLUDES_FILE_NAME.equals(path.getFileName().toString()) &&
                       CHE_DIR.equals(path.getParent().getFileName().toString());
    }

    private Consumer<String> getCreateConsumer() {
        return excludesFileLocation -> {
            LOG.error("=== created " + excludesFileLocation);

            handleModify(excludesFileLocation);
        };
    }

    private Consumer<String> getModifyConsumer() {
        return excludesFileLocation -> {
            LOG.error("=== modified " + excludesFileLocation);

            handleModify(excludesFileLocation);
        };

    }

    private void handleModify(String excludesFileLocation) {
        try {
            Path excludesFilePath = Paths.get(toNormalPath(excludesFileLocation));
            Path projectPath = excludesFilePath.getParent().getParent();
            excludes.remove(projectPath);

            List<String> lines = Files.lines(excludesFilePath).collect(toList());
            Set<Path> projectExcludes = new HashSet<>(lines.size());

            for (String line : lines) {
                Path excludePath = projectPath.resolve(line.trim());
                if (!isNullOrEmpty(line) && Files.exists(excludePath)) {
                    projectExcludes.add(excludePath);
                }
            }

            if (!projectExcludes.isEmpty()) {
                excludes.put(projectPath, projectExcludes);
            }
        } catch (IOException e) {
            LOG.error("=== ERROR " + excludesFileLocation);
        }
    }

    private Consumer<String> getDeleteConsumer() {
        return excludesFileLocation -> {
            LOG.error("=== deleted " + excludesFileLocation);
            Path excludesFilePath = Paths.get(toNormalPath(excludesFileLocation));
            Path projectPath = excludesFilePath.getParent().getParent();
            excludes.remove(projectPath);
        };
    }

    private void addFileWatcherExcludesFromIgnoreFile() {
        fileWatcherManager.addExcludeMatcher(new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                for (Set<Path> projectExcludes : excludes.values()) {
                    for (Path filter : projectExcludes) {
                        if (filter.equals(path)) {
                            return true;
                        }
                    }

                }
                return false;
            }
        });
    }

    @PreDestroy
    public void stopWatcher() {
        fileWatcherManager.unRegisterByMatcher(excludesFileWatchingOperationID);
    }

    private String toNormalPath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
