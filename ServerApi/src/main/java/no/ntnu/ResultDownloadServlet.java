package no.ntnu;


import no.trygvejw.debugLogger.DebugLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@WebServlet(name="download",urlPatterns={"/download"})
public class ResultDownloadServlet extends HttpServlet {

    private DebugLogger dbl = new DebugLogger(true);

    /*
        todo: shold probably return a custom page when a correct or incorect ticket id is entered so it does not look broken



     */

    @Override
    public void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {


        File sendDir = new File(System.getenv("SEND_DIR"));

        Stream<File> sendDirContents = Arrays.stream(Objects.requireNonNull(sendDir.listFiles()));
        String downloadId = request.getRequestURI().replace("/download/", "") + ".zip";


        if (sendDirContents.anyMatch(file -> file.getName().equals(downloadId))){
            File downloadFile = new File(sendDir, downloadId);
            FileInputStream inStream = new FileInputStream(downloadFile);

            // obtains ServletContext
            ServletContext context = getServletContext();

            // gets MIME type of the file
            String mimeType = context.getMimeType(downloadFile.getCanonicalPath());
            if (mimeType == null) {
                // set to binary type if MIME mapping not found
                mimeType = "application/octet-stream";
            }
            dbl.log("MIME type: " + mimeType);

            // modifies response
            response.setContentType(mimeType);
            response.setContentLength((int) downloadFile.length());

            // forces download
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
            response.setHeader(headerKey, headerValue);

            // obtains response's output stream
            OutputStream outStream = response.getOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead = -1;

            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }

            inStream.close();
            outStream.close();
        }


    }
}