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

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.info("doGet():  requestURI = " + request.getRequestURI());

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

        // dont include candidate repos
    }

    protected void renderChoiceOfContent(HttpServletRequest request, HttpServletResponse response, Repo repo) {
        log.info("Choice of content is: {" + PACKAGES + ", " + DISTRIBUTIONS + "}");
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
            log.info("TODO: fetch package bits and return them.");
            PackageVersion pv = getPackageVersionFromFileName(repo, fileName);
            if (pv == null) {
                log.info("Unable to find PackageVersion from filename: " + fileName);
                renderErrorPage(request, response);
                return;
            }
            response.setContentType("application/x-rpm");
            writePackageVersionBits(pv, response.getOutputStream());
        }
    }

    protected void renderPackageIndex(HttpServletRequest request, HttpServletResponse response, Repo repo) {
        // placeholder for packages index.html
        log.info("Forming packages index.html for repo: " + repo.getName() + ", ID = " + repo.getId());

        List<PackageVersion> pvs = repoMgr.findPackageVersionsInRepo(LookupUtil.getSubjectManager().getOverlord(), repo
            .getId(), PageControl.getUnlimitedInstance());

        for (PackageVersion pv : pvs) {
            log.info("Add PackageVersion: " + pv);
        }

    }

    protected void renderDistributions(HttpServletRequest request, HttpServletResponse response, Repo repo) {

    }

    protected void renderErrorPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("render error page for request: " + request.getRequestURI());
        response.sendError(response.SC_NOT_FOUND);
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
        int index = n - 1;
        StrTokenizer st = new StrTokenizer(requestURI, "/");
        List<String> tokens = st.getTokenList();
        if (tokens.size() < index) {
            return "";
        }
        return tokens.get(index);
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
