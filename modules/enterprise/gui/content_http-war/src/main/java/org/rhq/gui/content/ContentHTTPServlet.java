/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.gui.content;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlets.DefaultServlet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ContentHTTPServlet extends DefaultServlet {
    private final Log log = LogFactory.getLog(ContentHTTPServlet.class);

    protected static final String CONTENT_URI = "/content/";

    protected static final String PACKAGES = "packages";
    protected static final String DISTRIBUTIONS = "distributions";

    protected RepoManagerLocal repoMgr;
    protected ContentManagerLocal contentMgr;
    protected ContentSourceManagerLocal contentSourceMgr;
    protected DistributionManagerLocal distroMgr;

    public ContentHTTPServlet() {
        super();
    }

    public void init() throws ServletException {
        super.init();
        repoMgr = LookupUtil.getRepoManagerLocal();
        contentMgr = LookupUtil.getContentManager();
        contentSourceMgr = LookupUtil.getContentSourceManager();
        distroMgr = LookupUtil.getDistributionManagerLocal();
        log.info("ContentHTTPServlet resolved references in init.");
    }

    protected boolean isIconRequest(HttpServletRequest request) {
        String dir = getNthPiece(2, request.getRequestURI());
        return StringUtils.equalsIgnoreCase(dir, "icons");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.info("doGet():  requestURI = " + request.getRequestURI());
        if (isIconRequest(request)) {
            // this request is for our icons which are static content in the webapp
            // defer back to DefaultServlet.doGet and this will be served automatically
            super.doGet(request, response);
            return;
        }
        // Check if repo has been specified
        Repo repo = getRepo(request, response);
        if (repo == null) {
            log.info("No repo found, possibly bad repo name, or no name was entered.");
            renderRepoList(request, response);
            return;
        }
        // Check if type of content has been specified
        String typeOfContent = getTypeOfContent(request.getRequestURI());
        if (StringUtils.isBlank(typeOfContent)) {
            log.info("no info was specified for type of content.");
            renderChoiceOfContent(request, response, repo);
            return;
        }
        //
        // Determine what we should render, i.e.: packages or distributions
        //
        if (StringUtils.equalsIgnoreCase(typeOfContent, PACKAGES)) {
            log.info("render packages");
            renderPackages(request, response, repo);
            return;

        } else if (StringUtils.equalsIgnoreCase(typeOfContent, DISTRIBUTIONS)) {
            log.info("render distributions");
            renderDistributions(request, response, repo);
            return;
        } else {
            log.info("Unable to determine what type of content was requested: " + typeOfContent);
            renderErrorPage(request, response);
            return;
        }
    }

    protected void renderRepoList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Two known situations
        // #1 User entered a bad URL
        // #2 User entered no repo name

        String repoName = getRepoName(request.getRequestURI());
        if (!StringUtils.isEmpty(repoName)) {
            log.info("Assuming bad repo name for: " + repoName + ", will render error page");
            renderErrorPage(request, response);
            return;
        }
        // Entered repo name is blank, so we'll render a list of repos

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        PageList<Repo> repos = repoMgr.findRepos(overlord, PageControl.getUnlimitedInstance());
        log.info("Returned list of repos: " + repos.getTotalSize() + " entries");
        for (Repo r : repos) {
            log.info("Potential repo: Name = " + r.getName() + ", ID = " + r.getId());
        }
        log.info("TODO: generate index.html");
        StringBuffer sb = new StringBuffer();
        HtmlRenderer.formStart(sb, "Index of ", request.getRequestURI());
        for (Repo r : repos) {
            log.info("Potential repo: Name = " + r.getName() + ", ID = " + r.getId());
            // Skip candidate repos
            if (!r.isCandidate()) {
                String lastMod = new Date(r.getLastModifiedDate()).toString();
                HtmlRenderer.formDirEntry(sb, request, r.getName(), lastMod);
            }
        }
        HtmlRenderer.formEnd(sb);
        writeResponse(sb.toString(), response);
    }

    protected void renderChoiceOfContent(HttpServletRequest request, HttpServletResponse response, Repo repo)
        throws IOException {
        log.info("Choice of content is: {" + PACKAGES + ", " + DISTRIBUTIONS + "}");
        StringBuffer sb = new StringBuffer();
        HtmlRenderer.formStart(sb, "Index of ", request.getRequestURI());
        HtmlRenderer.formParentLink(sb, getParentURI(request.getRequestURI()));
        HtmlRenderer.formDirEntry(sb, request, PACKAGES, "-");
        HtmlRenderer.formDirEntry(sb, request, DISTRIBUTIONS, "-");
        HtmlRenderer.formEnd(sb);
        writeResponse(sb.toString(), response);
    }

    protected void renderPackages(HttpServletRequest request, HttpServletResponse response, Repo repo)
        throws IOException {

        log.info("renderPackages(repo name = " + repo.getName() + ", id = " + repo.getId() + ", isCandidate = "
            + repo.isCandidate() + ")");

        String fileName = getFileName(request.getRequestURI());
        log.info("Parsed file name = " + fileName);
        if (StringUtils.isBlank(fileName)) {
            // form a directory listing for each package in repository. 
            log.info("TODO: create index.html listing all the packages for this repo: " + repo.getName());
            renderPackageIndex(request, response, repo);
            return;
        } else {
            log.info("fetch package bits and return them.");
            PackageVersion pv = getPackageVersionFromFileName(repo, fileName);
            if (pv == null) {
                log.info("Unable to find PackageVersion from filename: " + fileName);
                renderErrorPage(request, response);
                return;
            }
            response.setContentType("application/octet-stream");
            writePackageVersionBits(response.getOutputStream(), pv);
        }
    }

    protected void renderPackageIndex(HttpServletRequest request, HttpServletResponse response, Repo repo)
        throws IOException {
        log.info("Forming packages index.html for repo: " + repo.getName() + ", ID = " + repo.getId());

        List<PackageVersion> pvs = repoMgr.findPackageVersionsInRepo(LookupUtil.getSubjectManager().getOverlord(), repo
            .getId(), PageControl.getUnlimitedInstance());

        StringBuffer sb = new StringBuffer();
        HtmlRenderer.formStart(sb, "Index of ", request.getRequestURI());
        HtmlRenderer.formParentLink(sb, getParentURI(request.getRequestURI()));
        for (PackageVersion pv : pvs) {
            HtmlRenderer.formFileEntry(sb, request, pv.getFileName(), new Date(pv.getFileCreatedDate()).toString(), pv
                .getFileSize());
        }
        HtmlRenderer.formEnd(sb);
        writeResponse(sb.toString(), response);
    }

    protected void renderDistributions(HttpServletRequest request, HttpServletResponse response, Repo repo)
        throws IOException {
        log.info("renderDistributions(repo name = " + repo.getName() + ", id = " + repo.getId() + ", isCandidate = "
            + repo.isCandidate() + ")");

        String distLabel = getDistLabel(request.getRequestURI());
        log.info("Parsed dist label is = " + distLabel);
        if (StringUtils.isBlank(distLabel)) {
            // form a directory listing for each distribution in repository.
            log.info("TODO: create index.html listing all the distribution labels for this repo: " + repo.getName());
            renderDistributionLabels(request, response, repo);
            return;
        }
        // Get Distribution
        Distribution dist = distroMgr.getDistributionByLabel(distLabel);
        if (dist == null) {
            log.info("Unable to find Distribution by label '" + distLabel + "'");
            renderErrorPage(request, response);
            return;
        }
        String fileRequest = getDistFilePath(request.getRequestURI());
        if (StringUtils.isEmpty(fileRequest)) {
            log.info("no distribution file was found in request, so render list of all distribution files");
            renderDistributionFileList(request, response, dist);
            return;
        }
        log.info("Parsed DistributionFile request is for: " + fileRequest);
        // Looks like a request for a distribution file
        List<DistributionFile> distFiles = distroMgr.getDistributionFilesByDistId(dist.getId());
        if (distFiles.isEmpty()) {
            log.info("Unable to find any distribution files for dist: " + dist.getLabel());
            renderErrorPage(request, response);
            return;
        }
        for (DistributionFile dFile : distFiles) {
            log.info("Compare: " + dFile.getRelativeFilename() + " to " + fileRequest);
            if (StringUtils.equalsIgnoreCase(dFile.getRelativeFilename(), fileRequest)) {
                response.setContentType("application/octet-stream");
                writeDistributionFileBits(response.getOutputStream(), dFile);
                return;
            }
        }
        log.info("Searched through DistributionFiles and unable to find: " + fileRequest + ", in Distribution: "
            + dist.getLabel());

        renderErrorPage(request, response);

    }

    protected void renderDistributionFileList(HttpServletRequest request, HttpServletResponse response,
        Distribution dist) throws IOException {

        List<DistributionFile> distFiles = distroMgr.getDistributionFilesByDistId(dist.getId());
        log.info("For Distribution label '" + dist.getLabel() + "' " + distFiles.size()
            + " distribution files were found");

        StringBuffer sb = new StringBuffer();
        HtmlRenderer.formStart(sb, "Index of ", request.getRequestURI());
        HtmlRenderer.formParentLink(sb, getParentURI(request.getRequestURI()));
        for (DistributionFile dFile : distFiles) {
            HtmlRenderer.formFileEntry(sb, request, dFile.getRelativeFilename(), new Date(dFile.getLastModified())
                .toString(), -1);
        }
        HtmlRenderer.formEnd(sb);
        writeResponse(sb.toString(), response);
    }

    protected void renderDistributionLabels(HttpServletRequest request, HttpServletResponse response, Repo repo)
        throws IOException {

        StringBuffer sb = new StringBuffer();
        HtmlRenderer.formStart(sb, "Index of ", request.getRequestURI());
        HtmlRenderer.formParentLink(sb, getParentURI(request.getRequestURI()));
        // Get list of Distributions per repo
        List<Distribution> distros = repoMgr.findAssociatedDistributions(LookupUtil.getSubjectManager().getOverlord(),
            repo.getId(), PageControl.getUnlimitedInstance());
        log.info("Found " + distros.size() + " for repo " + repo.getName());
        for (Distribution d : distros) {
            log.info("Creating link for distribution label: " + d.getLabel());
            HtmlRenderer.formDirEntry(sb, request, d.getLabel(), new Date(d.getLastModifiedDate()).toString());
        }
        HtmlRenderer.formEnd(sb);
        writeResponse(sb.toString(), response);
    }

    protected void renderErrorPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("render error page for request: " + request.getRequestURI());
        response.sendError(response.SC_NOT_FOUND);
    }

    protected boolean writeResponse(String data, HttpServletResponse response) throws IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.write(data);
        out.flush();
        out.close();
        return true;
    }

    protected Repo getRepo(HttpServletRequest request, HttpServletResponse response) {

        String repoName = getRepoName(request.getRequestURI());
        log.info("Parsed repo name = " + repoName);
        List<Repo> targetRepos = repoMgr.getRepoByName(repoName);
        if (targetRepos.isEmpty()) {
            // Check if maybe this is the repoID passed in as an int, instead of repo name
            try {
                Integer repoId = Integer.parseInt(repoName);
                return repoMgr.getRepo(LookupUtil.getSubjectManager().getOverlord(), repoId);
            } catch (NumberFormatException e) {
                //ignore
            }
            return null;
        }
        return targetRepos.get(0);
    }

    /**
     * Expecting URI format of:
     * 127.0.0.1:7080/content/$REPONAME/$TYPE_OF_CONTENT/$FILENAME
     */

    /**
     * 
     * @param requestURI
     * @return repo name or "" if no repo name could be determined
     */
    protected String getRepoName(String requestURI) {
        return getNthPiece(2, requestURI);
    }

    /**
     * 
     * @param requestURI
     * @return string that denotes if this is a package/distribution/etc kind of request
     */
    protected String getTypeOfContent(String requestURI) {
        return getNthPiece(3, requestURI);
    }

    /**
     * 
     * @param requestURI
     * @return file name or "" if no file name could be determined
     */
    protected String getFileName(String requestURI) {
        return getNthPiece(4, requestURI);
    }

    protected String getDistLabel(String requestURI) {
        return getNthPiece(4, requestURI);
    }

    /**
     *
     * @param requestURI
     * @return
     */
    protected String getDistFilePath(String requestURI) {
        // Goal is we find where the distribution file path starts
        // then we return the entire path, it may include an unknown
        // level of sub-directories.
        StrTokenizer st = new StrTokenizer(requestURI, "/");
        List<String> tokens = st.getTokenList();
        if (tokens.isEmpty()) {
            return "";
        }
        int startIndex = 4;
        String distFilePath = "";
        for (int index = startIndex; index < tokens.size(); index++) {
            distFilePath = distFilePath + "/" + tokens.get(index);
            log.info("index = " + index + ", distFilePath = " + distFilePath);
        }
        // Remove the '/' we added to the front of this string
        if (distFilePath.startsWith("/")) {
            distFilePath = distFilePath.substring(1);
        }
        return distFilePath;
    }

    /**
     * @param n nth element to return from requestURI, (first element corresponds to 1, not 0)
     * @param requestURI
     * 
     */
    protected String getNthPiece(int n, String requestURI) {
        StrTokenizer st = new StrTokenizer(requestURI, "/");
        List<String> tokens = st.getTokenList();
        if (tokens.size() < n) {
            return "";
        }
        return tokens.get(n - 1); // caller is starting at 1 not 0
    }

    protected String getParentURI(String uri) {
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        int index = uri.lastIndexOf("/");
        if (index == -1) {
            return uri;
        }
        return uri.substring(0, index);
    }

    protected PackageVersion getPackageVersionFromFileName(Repo repo, String fileName) {

        PackageVersionCriteria criteria = new PackageVersionCriteria();
        criteria.addFilterFileName(fileName);
        criteria.addFilterRepoId(repo.getId());
        log.info("Created criteria for repoId = " + repo.getId() + ", fileName = " + fileName);
        List<PackageVersion> pkgVers = contentMgr.findPackageVersionsByCriteria(LookupUtil.getSubjectManager()
            .getOverlord(), criteria);
        for (PackageVersion pkgV : pkgVers) {
            log.info("PackageVersion found: " + pkgVers);
        }
        log.info("Found " + pkgVers.size() + " entries");
        PackageVersion pv = null;
        if (pkgVers.size() > 0) {
            pv = pkgVers.get(0);
        } else {
            log.info("Couldn't find " + fileName + " in " + repo.getName());
        }
        return pv;
    }

    protected boolean writeDistributionFileBits(ServletOutputStream output, DistributionFile distFile)
        throws IOException {

        try {
            contentSourceMgr.outputDistributionFileBits(distFile, output);
            output.flush();
            output.close();
        } catch (IllegalStateException e) {
            log.error(e);
            return false;
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    protected boolean writePackageVersionBits(ServletOutputStream output, PackageVersion pkgVer) {

        try {
            contentSourceMgr.outputPackageVersionBits(pkgVer, output);
            output.flush();
            output.close();
        } catch (IllegalStateException e) {
            log.error(e);
            return false;
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }
}
