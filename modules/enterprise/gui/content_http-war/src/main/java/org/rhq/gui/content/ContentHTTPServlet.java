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
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ContentHTTPServlet extends DefaultServlet {
    private final Log log = LogFactory.getLog(ContentHTTPServlet.class);

    protected static final String CONTENT_URI = "/content";

    protected static final String PACKAGES = "packages";
    protected static final String DISTRIBUTIONS = "distributions";

    protected RepoManagerLocal repoMgr;
    protected ContentManagerLocal contentMgr;
    protected ContentSourceManagerLocal contentSourceMgr;

    public ContentHTTPServlet() {
        super();
    }

    public void init() throws ServletException {
        super.init();
        repoMgr = LookupUtil.getRepoManagerLocal();
        contentMgr = LookupUtil.getContentManager();
        contentSourceMgr = LookupUtil.getContentSourceManager();
        log.info("ContentHTTPServlet resolved references in init.");
    }

    protected boolean isIconRequest(HttpServletRequest request) {
        String dir = getNthPiece(2, request.getRequestURI());
        return StringUtils.equalsIgnoreCase(dir, "icons");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.info("doGet():  requestURI = " + request.getRequestURI());
        if (isIconRequest(request)) {
            super.doGet(request, response);
            return;
        }
        // Check if repo has been specified
        Repo repo = getRepo(request, response);
        if (repo == null) {
            log.info("No repo found so we'll render a list of all repos.");
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
                HtmlRenderer.formDirEntry(request, sb, r.getName(), lastMod);
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
        HtmlRenderer.formDirEntry(request, sb, PACKAGES, "-");
        HtmlRenderer.formDirEntry(request, sb, DISTRIBUTIONS, "-");
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
            writePackageVersionBits(pv, response.getOutputStream());
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
            HtmlRenderer.formFileEntry(sb, pv.getFileName(), new Date(pv.getFileCreatedDate()).toString(), pv
                .getFileSize());
        }
        HtmlRenderer.formEnd(sb);
        writeResponse(sb.toString(), response);
    }

    protected void renderDistributions(HttpServletRequest request, HttpServletResponse response, Repo repo)
        throws IOException {

        StringBuffer sb = new StringBuffer();
        HtmlRenderer.formStart(sb, "Index of ", request.getRequestURI());
        HtmlRenderer.formParentLink(sb, getParentURI(request.getRequestURI()));
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

    protected boolean writePackageVersionBits(PackageVersion pkgVer, ServletOutputStream output) {

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
