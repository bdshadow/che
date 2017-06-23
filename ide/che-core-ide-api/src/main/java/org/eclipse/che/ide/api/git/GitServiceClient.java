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
package org.eclipse.che.ide.api.git;

import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchListMode;
import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.git.shared.DiffType;
import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.MergeResult;
import org.eclipse.che.api.git.shared.PullResponse;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.git.shared.ResetRequest.ResetType;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.git.shared.ShowFileContentResponse;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.api.git.shared.StatusFormat;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.resource.Path;

import java.util.List;
import java.util.Map;

/**
 * Service contains methods for working with Git repository from client side.
 *
 * @author Ann Zhuleva
 * @author Vlad Zhukovskyi
 * @author Igor Vinokur
 */
public interface GitServiceClient {

    /**
     * Add changes to Git index (temporary storage). Sends request over WebSocket.
     *
     * @param update
     *         if <code>true</code> then never stage new files, but stage modified new contents of tracked files and remove files from
     *         the index if the corresponding files in the working tree have been removed
     * @param paths
     *         pattern of the files to be added, default is "." (all files are added)
     */
    Promise<Void> add(boolean update, Path[] paths);

    /**
     * Fetch changes from remote repository to local one (sends request over WebSocket).
     *
     * @param remote
     *         remote repository's name
     * @param refspec
     *         list of refspec to fetch.
     *         <p/>
     *         Expected form is:
     *         <ul>
     *         <li>refs/heads/featured:refs/remotes/origin/featured - branch 'featured' from remote repository will be fetched to
     *         'refs/remotes/origin/featured'.</li>
     *         <li>featured - remote branch name.</li>
     *         </ul>
     * @param removeDeletedRefs
     *         if <code>true</code> then delete removed refs from local repository
     */
    Promise<Void> fetch(String remote, List<String> refspec, boolean removeDeletedRefs);

    /**
     * Get the list of the branches. For now, all branches cannot be returned at once, so the parameter <code>remote</code> tells to get
     * remote branches if <code>true</code> or local ones (if <code>false</code>).
     *
     * @param mode
     *         get remote branches
     */
    Promise<List<Branch>> branchList(BranchListMode mode);

    /**
     * Delete branch.
     *
     * @param name
     *         name of the branch to delete
     * @param force
     *         force if <code>true</code> delete branch {@code name} even if it is not fully merged
     */
    Promise<Void> branchDelete(String name, boolean force);

    /**
     * Checkout the branch with pointed name.
     *
     * @param oldName
     *         branch's current name
     * @param newName
     *         branch's new name
     */
    Promise<Void> branchRename(String oldName, String newName);

    /**
     * Create new branch with pointed name.
     *
     * @param name
     *         new branch's name
     * @param startPoint
     *         name of a commit at which to start the new branch
     */
    Promise<Branch> branchCreate(String name, String startPoint);

    /**
     * Checkout the branch with pointed name.
     *
     * @param request
     *         checkout request
     */
    Promise<String> checkout(CheckoutRequest request);

    /**
     * Get the list of remote repositories for pointed by {@code projectConfig} parameter one.
     *
     * @param remote
     *         remote repository's name. Can be null in case when it is need to fetch all {@link Remote}
     * @param verbose
     *         If <code>true</code> show remote url and name otherwise show remote name
     * @return a promise that provides list {@link Remote} repositories for the {@code workspaceId}, {@code projectConfig},
     * {@code remoteName}, {@code verbose} or rejects with an error.
     */
    Promise<List<Remote>> remoteList(String remote, boolean verbose);

    /**
     * Adds remote repository to the list of remote repositories.
     *
     * @param name
     *         remote repository's name
     * @param url
     *         remote repository's URL
     */
    Promise<Void> remoteAdd(String name, String url);

    /**
     * Deletes the pointed(by name) remote repository from the list of repositories.
     *
     * @param name
     *         remote repository name to delete
     */
    Promise<Void> remoteDelete(String name);

    /**
     * Remove items from the working tree and the index.
     *
     * @param items
     *         items to remove
     * @param cached
     *         is for removal only from index
     */
    Promise<Void> remove(Path[] items, boolean cached);

    /**
     * Reset current HEAD to the specified state. There two types of the reset: <br>
     * 1. Reset files in index - content of files is untouched. Typically it is useful to remove from index mistakenly added files.<br>
     * <code>git reset [paths]</code> is the opposite of <code>git add [paths]</code>. 2. Reset the current branch head to [commit] and
     * possibly updates the index (resetting it to the tree of [commit]) and the working tree depending on [mode].
     *
     * @param commit
     *         commit to which current head should be reset
     * @param resetType
     *         type of the reset
     * @param files
     *         pattern of the files to reset the index. If <code>null</code> then reset the current branch head to [commit],
     *         else reset received files in index.
     */
    Promise<Void> reset(String commit, ResetType resetType, Path[] files);

    /**
     * Initializes new Git repository (over WebSocket).
     *
     * @param bare
     *         to create bare repository or not
     */
    Promise<Void> init(boolean bare);

    /**
     * Pull (fetch and merge) changes from remote repository to local one (sends request over WebSocket).
     *
     * @param refSpec
     *         list of refspec to fetch.
     *         <p/>
     *         Expected form is:
     *         <ul>
     *         <li>refs/heads/featured:refs/remotes/origin/featured - branch 'featured' from remote repository will be fetched to
     *         'refs/remotes/origin/featured'.</li>
     *         <li>featured - remote branch name.</li>
     *         </ul>
     * @param remote
     *         remote remote repository's name
     */
    Promise<PullResponse> pull(String refSpec, String remote);

    /**
     * Push changes from local repository to remote one (sends request over WebSocket).
     *
     * @param refSpec
     *         list of refspec to push
     * @param remote
     *         remote repository name or url
     * @param force
     *         push refuses to update a remote ref that is not an ancestor of the local ref used to overwrite it. If <code>true</code>
     *         disables the check. This can cause the remote repository to lose commits
     */
    Promise<PushResponse> push(List<String> refSpec, String remote, boolean force);

    /**
     * Performs commit changes from index to repository. The result of the commit is represented by {@link Revision}, which is returned by
     * callback in <code>onSuccess(Revision result)</code>. Sends request over WebSocket.
     *
     * @param message
     *         commit log message
     * @param all
     *         automatically stage files that have been modified and deleted
     * @param amend
     *         indicates that previous commit must be overwritten
     */
    Promise<Revision> commit(String message, boolean all, boolean amend);

    /**
     * Performs commit changes from index to repository.
     *
     * @param message
     *         commit log message
     * @param all
     *         automatically stage files that have been modified and deleted
     * @param files
     *         the list of files that are committed, ignoring the index
     * @param amend
     *         indicates that previous commit must be overwritten
     */
    Promise<Revision> commit(String message, boolean all, Path[] files, boolean amend);

    /**
     * Get repository options.
     *
     * @param requestedConfig
     *         list of config keys
     */
    Promise<Map<String, String>> config(List<String> requestedConfig);

    /**
     * Compare two commits, get the diff for pointed file(s) or for the whole project in text format.
     *
     * @param fileFilter
     *         files for which to show changes
     * @param type
     *         type of diff format
     * @param noRenames
     *         don't show renamed files
     * @param renameLimit
     *         the limit of shown renamed files
     * @param commitA
     *         first commit to compare
     * @param commitB
     *         second commit to be compared
     */
    Promise<String> diff(List<String> fileFilter,
                         DiffType type,
                         boolean noRenames,
                         int renameLimit,
                         String commitA,
                         String commitB);

    /**
     * Compare commit with index or working tree (depends on {@code cached}), get the diff for pointed file(s) or for the whole project in
     * text format.
     *
     * @param files
     *         files for which to show changes
     * @param type
     *         type of diff format
     * @param noRenames
     *         don't show renamed files
     * @param renameLimit
     *         the limit of shown renamed files
     * @param commitA
     *         commit to compare
     * @param cached
     *         if <code>true</code> then compare commit with index, if <code>false</code>, then compare with working tree.
     */
    Promise<String> diff(List<String> files,
                         DiffType type,
                         boolean noRenames,
                         int renameLimit,
                         String commitA,
                         boolean cached);

    /**
     * Get the file content from specified revision or branch.
     *
     * @param file
     *         file name with its full path
     * @param version
     *         revision or branch where the showed file is present
     */
    Promise<ShowFileContentResponse> showFileContent(Path file, String version);

    /**
     * Get log of commits.
     *
     * @param fileFilter
     *         range of files to filter revisions list
     * @param skip
     *         the number of commits that will be skipped
     * @param maxCount
     *         the number of commits that will be returned
     * @param plainText
     *         if <code>true</code> the loq response will be in text format
     */
    Promise<LogResponse> log(@Nullable Path[] fileFilter, int skip, int maxCount, boolean plainText);

    /**
     * Merge the pointed commit with current HEAD.
     *
     * @param commit
     *         commit's reference to merge with
     */
    Promise<MergeResult> merge(String commit);

    /**
     * Gets the working tree status. The status of added, modified or deleted files is shown is written in {@link String}. The format may
     * be
     * long, short or porcelain. Example of detailed format:<br>
     * <p/>
     * <p/>
     * <pre>
     * # Untracked files:
     * #
     * # file.html
     * # folder
     * </pre>
     * <p/>
     * Example of short format:
     * <p/>
     * <p/>
     * <pre>
     * M  pom.xml
     * A  folder/test.html
     * D  123.txt
     * ?? folder/test.css
     * </pre>
     *
     * @param format
     *         to show in short format or not
     */
    Promise<String> statusText(StatusFormat format);

    /**
     * Returns the current working tree status.
     *
     * @return the promise which either resolves working tree status or rejects with an error
     */
    Promise<Status> getStatus();

    /**
     * Remove the git repository from given path.
     *
     * @return the promise with success status
     */
    Promise<Void> deleteRepository();
}
