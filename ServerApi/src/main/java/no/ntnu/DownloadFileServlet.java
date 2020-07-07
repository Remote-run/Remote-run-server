package no.ntnu;

import no.ntnu.util.DebugLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@WebServlet(name="download",urlPatterns={"/download"})
public class DownloadFileServlet extends HttpServlet {

    private DebugLogger dbp = new DebugLogger(true);



    @Override
    public void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        File sendDir = new File(System.getenv("SEND_DIR"));
        dbp.fileLog(sendDir);


        dbp.log(sendDir.listFiles());
        Stream<File> sendDirContents = Arrays.stream(Objects.requireNonNull(sendDir.listFiles()));
        String downloadId = request.getRequestURI().replace("/download/", ""); //request.getHeader("ticket-id");

        dbp.log("URI:", downloadId);

        dbp.log();

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
            dbp.log("MIME type: " + mimeType);

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