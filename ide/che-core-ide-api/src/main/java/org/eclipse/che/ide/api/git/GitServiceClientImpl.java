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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.BranchListMode;
import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.DiffType;
import org.eclipse.che.api.git.shared.FetchRequest;
import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.MergeRequest;
import org.eclipse.che.api.git.shared.MergeResult;
import org.eclipse.che.api.git.shared.PullRequest;
import org.eclipse.che.api.git.shared.PullResponse;
import org.eclipse.che.api.git.shared.PushRequest;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.git.shared.RemoteAddRequest;
import org.eclipse.che.api.git.shared.ResetRequest;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.git.shared.ShowFileContentResponse;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.api.git.shared.StatusFormat;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.rest.AsyncRequest;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.git.shared.StatusFormat.PORCELAIN;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.MimeType.TEXT_PLAIN;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;

/**
 * Implementation of the {@link GitServiceClient}.
 *
 * @author Ann Zhuleva
 * @author Valeriy Svydenko
 */
@Singleton
public class GitServiceClientImpl implements GitServiceClient {
    private static final String ADD        = "/git/add";
    private static final String BRANCH     = "/git/branch";
    private static final String CHECKOUT   = "/git/checkout";
    private static final String COMMIT     = "/git/commit";
    private static final String CONFIG     = "/git/config";
    private static final String DIFF       = "/git/diff";
    private static final String FETCH      = "/git/fetch";
    private static final String INIT       = "/git/init";
    private static final String LOG        = "/git/log";
    private static final String SHOW       = "/git/show";
    private static final String MERGE      = "/git/merge";
    private static final String STATUS     = "/git/status";
    private static final String PUSH       = "/git/push";
    private static final String PULL       = "/git/pull";
    private static final String REMOTE     = "/git/remote";
    private static final String REMOVE     = "/git/remove";
    private static final String RESET      = "/git/reset";
    private static final String REPOSITORY = "/git/repository";

    /** Loader to be displayed. */
    private final AsyncRequestLoader     loader;
    private final DtoFactory             dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final AppContext             appContext;

    @Inject
    protected GitServiceClientImpl(LoaderFactory loaderFactory,
                                   DtoFactory dtoFactory,
                                   AsyncRequestFactory asyncRequestFactory,
                                   DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                   AppContext appContext) {
        this.appContext = appContext;
        this.loader = loaderFactory.newLoader();
        this.dtoFactory = dtoFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    @Override
    public Promise<Void> init(boolean bare) {
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + INIT +
                     "?projectPath=" + appContext.getRootProject().toString() + "&bare=" + bare;
        return asyncRequestFactory.createPostRequest(url, null).loader(loader).send();
    }

    @Override
    public Promise<String> statusText(StatusFormat format) {
        String params = "?projectPath=" + appContext.getRootProject().toString() + "&format=" + format;
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + STATUS + params;

        return asyncRequestFactory.createGetRequest(url)
                                  .loader(loader)
                                  .header(CONTENTTYPE, APPLICATION_JSON)
                                  .header(ACCEPT, TEXT_PLAIN)
                                  .send(new StringUnmarshaller());
    }

    @Override
    public Promise<Void> add(boolean update, Path[] paths) {
        final AddRequest addRequest = dtoFactory.createDto(AddRequest.class).withUpdate(update);

        if (paths == null) {
            addRequest.setFilePattern(AddRequest.DEFAULT_PATTERN);
        } else {
            final List<String> patterns = new ArrayList<>(); //need for compatible with server side
            for (Path path : paths) {
                patterns.add(path.isEmpty() ? "." : path.toString());
            }

            addRequest.setFilePattern(patterns);
        }

        final String url = appContext.getDevMachine().getWsAgentBaseUrl() + ADD +
                           "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, addRequest).loader(loader).send();
    }

    @Override
    public Promise<Revision> commit(String message, boolean all, boolean amend) {
        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(all);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + COMMIT +
                     "?projectPath=" + appContext.getRootProject().toString();

        return asyncRequestFactory.createPostRequest(url, commitRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Revision.class));
    }

    @Override
    public Promise<Revision> commit(String message, boolean all, Path[] files, boolean amend) {
        List<String> paths = new ArrayList<>(files.length);

        for (Path file : files) {
            if (!file.isEmpty()) {
                paths.add(file.toString());
            }
        }

        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(all)
                                                .withFiles(paths);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + COMMIT +
                     "?projectPath=" + appContext.getRootProject().toString();

        return asyncRequestFactory.createPostRequest(url, commitRequest)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Revision.class));
    }

    @Override
    public Promise<Map<String, String>> config(List<String> requestedConfig) {
        String params = "?projectPath=" + appContext.getRootProject().toString();
        if (requestedConfig != null) {
            for (String entry : requestedConfig) {
                params += "&requestedConfig=" + entry;
            }
        }
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + CONFIG + params;
        return asyncRequestFactory.createGetRequest(url).loader(loader).send(new StringMapUnmarshaller());
    }

    @Override
    public Promise<PushResponse> push(List<String> refSpec, String remote, boolean force) {
        PushRequest pushRequest = dtoFactory.createDto(PushRequest.class)
                                            .withRemote(remote)
                                            .withRefSpec(refSpec)
                                            .withForce(force);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + PUSH + "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, pushRequest)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(PushResponse.class));
    }

    @Override
    public Promise<List<Remote>> remoteList(String remoteName, boolean verbose) {
        String params = "?projectPath=" + appContext.getRootProject().toString() + (remoteName != null ? "&remoteName=" + remoteName : "") +
                        "&verbose=" + String.valueOf(verbose);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + REMOTE + params;
        return asyncRequestFactory.createGetRequest(url)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newListUnmarshaller(Remote.class));
    }

    @Override
    public Promise<List<Branch>> branchList(BranchListMode listMode) {
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + BRANCH + "?projectPath=" + appContext.getRootProject().toString() +
                     (listMode == null ? "" : "&listMode=" + listMode);
        return asyncRequestFactory.createGetRequest(url).send(dtoUnmarshallerFactory.newListUnmarshaller(Branch.class));
    }

    @Override
    public Promise<Status> getStatus() {
        final String params = "?projectPath=" + appContext.getRootProject().toString() + "&format=" + PORCELAIN;
        final String url = appContext.getDevMachine().getWsAgentBaseUrl() + STATUS + params;
        return asyncRequestFactory.createGetRequest(url)
                                  .loader(loader)
                                  .header(CONTENTTYPE, APPLICATION_JSON)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Status.class));
    }

    @Override
    public Promise<Void> branchDelete(String name, boolean force) {
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + BRANCH + "?projectPath=" + appContext.getRootProject().toString()
                     + "&name=" + name + "&force=" + force;
        return asyncRequestFactory.createDeleteRequest(url).loader(loader).send();
    }

    @Override
    public Promise<Void> branchRename(String oldName, String newName) {
        String params = "?projectPath=" + appContext.getRootProject().toString() + "&oldName=" + oldName + "&newName=" + newName;
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + BRANCH + params;
        return asyncRequestFactory.createPostRequest(url, null).loader(loader)
                                  .header(CONTENTTYPE, MimeType.APPLICATION_FORM_URLENCODED)
                                  .send();
    }

    @Override
    public Promise<Branch> branchCreate(String name, String startPoint) {
        BranchCreateRequest branchCreateRequest = dtoFactory.createDto(BranchCreateRequest.class).withName(name).withStartPoint(startPoint);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + BRANCH + "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, branchCreateRequest)
                                  .loader(loader)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(Branch.class));
    }

    @Override
    public Promise<String> checkout(CheckoutRequest request) {

        final String url = appContext.getDevMachine().getWsAgentBaseUrl() + CHECKOUT + "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, request).loader(loader).send(new StringUnmarshaller());
    }

    @Override
    public Promise<Void> remove(Path[] items, boolean cached) {
        String params = "?projectPath=" + appContext.getRootProject().toString();
        if (items != null) {
            for (Path item : items) {
                params += "&items=" + item.toString();
            }
        }
        params += "&cached=" + String.valueOf(cached);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + REMOVE + params;
        return asyncRequestFactory.createDeleteRequest(url).loader(loader).send();
    }

    @Override
    public Promise<Void> reset(String commit, ResetRequest.ResetType resetType, Path[] files) {
        ResetRequest resetRequest = dtoFactory.createDto(ResetRequest.class).withCommit(commit);
        if (resetType != null) {
            resetRequest.setType(resetType);
        }
        if (files != null) {
            List<String> fileList = new ArrayList<>(files.length);
            for (Path file : files) {
                fileList.add(file.isEmpty() ? "." : file.toString());
            }
            resetRequest.setFilePattern(fileList);
        }
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + RESET + "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, resetRequest).loader(loader).send();
    }

    @Override
    public Promise<LogResponse> log(Path[] fileFilter, int skip, int maxCount, boolean plainText) {
        StringBuilder params = new StringBuilder().append("?projectPath=").append(appContext.getRootProject().toString());
        if (fileFilter != null) {
            for (Path file : fileFilter) {
                params.append("&fileFilter=").append(file.toString());
            }
        }
        params.append("&skip=").append(skip);
        params.append("&maxCount=").append(maxCount);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + LOG + params;
        if (plainText) {
            return asyncRequestFactory.createGetRequest(url)
                                      .send(dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class));
        } else {
            return asyncRequestFactory.createGetRequest(url)
                                      .header(ACCEPT, APPLICATION_JSON)
                                      .send(dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class));
        }
    }

    @Override
    public Promise<Void> remoteAdd(String name, String url) {
        RemoteAddRequest remoteAddRequest = dtoFactory.createDto(RemoteAddRequest.class).withName(name).withUrl(url);
        String requestUrl = appContext.getDevMachine().getWsAgentBaseUrl() + REMOTE +
                            "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPutRequest(requestUrl, remoteAddRequest).loader(loader).send();
    }

    @Override
    public Promise<Void> remoteDelete(String name) {
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + REMOTE + '/' + name +
                     "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createDeleteRequest(url).loader(loader).send();
    }

    @Override
    public Promise<Void> fetch(String remote, List<String> refspec, boolean removeDeletedRefs) {
        FetchRequest fetchRequest = dtoFactory.createDto(FetchRequest.class)
                                              .withRefSpec(refspec)
                                              .withRemote(remote)
                                              .withRemoveDeletedRefs(removeDeletedRefs);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() +  FETCH + "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, fetchRequest).send();
    }

    @Override
    public Promise<PullResponse> pull(String refSpec, String remote) {
        PullRequest pullRequest = dtoFactory.createDto(PullRequest.class).withRemote(remote).withRefSpec(refSpec);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() +  PULL + "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, pullRequest).send(dtoUnmarshallerFactory.newUnmarshaller(PullResponse.class));
    }

    @Override
    public Promise<String> diff(List<String> fileFilter,
                                DiffType type,
                                boolean noRenames,
                                int renameLimit,
                                String commitA,
                                String commitB) {
        return diff(appContext.getProjectsRoot(),
                    fileFilter,
                    type,
                    noRenames,
                    renameLimit,
                    commitA,
                    commitB,
                    false).send(new StringUnmarshaller());
    }

    @Override
    public Promise<String> diff(List<String> files,
                                DiffType type,
                                boolean noRenames,
                                int renameLimit,
                                String commitA,
                                boolean cached) {
        return diff(appContext.getProjectsRoot(),
                    files,
                    type,
                    noRenames,
                    renameLimit,
                    commitA,
                    null,
                    cached).send(new StringUnmarshaller());
    }

    private AsyncRequest diff(Path project,
                              List<String> fileFilter,
                              DiffType type,
                              boolean noRenames,
                              int renameLimit,
                              String commitA,
                              String commitB,
                              boolean cached) {
        StringBuilder params = new StringBuilder().append("?projectPath=").append(project.toString());
        if (fileFilter != null) {
            for (String file : fileFilter) {
                if (file.isEmpty()) {
                    continue;
                }
                params.append("&fileFilter=").append(file);
            }
        }
        if (type != null) {
            params.append("&diffType=").append(type);
        }
        params.append("&noRenames=").append(noRenames);
        params.append("&renameLimit=").append(renameLimit);
        if (commitA != null) {
            params.append("&commitA=").append(commitA);
        }
        if (commitB != null) {
            params.append("&commitB=").append(commitB);
        }
        params.append("&cached=").append(cached);

        String url = appContext.getDevMachine().getWsAgentBaseUrl() + DIFF + params;
        return asyncRequestFactory.createGetRequest(url).loader(loader);
    }

    @Override
    public Promise<ShowFileContentResponse> showFileContent(Path file, String version) {
        String params = "?projectPath=" + appContext.getRootProject().toString() + "&file=" + file + "&version=" + version;
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + SHOW + params;
        return asyncRequestFactory.createGetRequest(url)
                                  .loader(loader)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(ShowFileContentResponse.class));
    }

    @Override
    public Promise<MergeResult> merge(String commit) {
        MergeRequest mergeRequest = dtoFactory.createDto(MergeRequest.class).withCommit(commit);
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + MERGE + "?projectPath=" + appContext.getRootProject();
        return asyncRequestFactory.createPostRequest(url, mergeRequest)
                                  .loader(loader)
                                  .header(ACCEPT, APPLICATION_JSON)
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(MergeResult.class));
    }

    @Override
    public Promise<Void> deleteRepository() {
        String url = appContext.getDevMachine().getWsAgentBaseUrl() + REPOSITORY +
                     "?projectPath=" + appContext.getRootProject().toString();
        return asyncRequestFactory.createPostRequest(url, null).loader(loader).send();
    }
}
