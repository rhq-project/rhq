package org.guest

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class GuestServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    doPost(request, response)
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    def scriptsDir = request.getParameter("scriptsDir")
    def suiteFile = request.getParameter("suiteFile")
    def outputDir = request.getParameter("outputDir")

    new GuestServer(scriptsDir: scriptsDir, suiteFile: suiteFile, outputDir: outputDir).executeTests()
  }

}
