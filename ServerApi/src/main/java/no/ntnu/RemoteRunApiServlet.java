package no.ntnu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import no.ntnu.config.ApiConfig;
import no.ntnu.config.JavaApiConfig;
import no.ntnu.enums.RunType;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.util.Compression;
import no.ntnu.util.DebugLogger;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


//@WebServlet(name="Api",urlPatterns={"/Api"})
public class RemoteRunApiServlet extends HttpServlet {

    private final File tmpdir = new File (System.getProperty("java.io.tmpdir"));
    private final File runDir = new File("/runvol");
    private final DebugLogger debugLogger = new DebugLogger(true);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.getOutputStream().println("{asdasdasd}");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        try {
            if(isMultipart){
                // Create a factory for disk-based file items
                DiskFileItemFactory factory = new DiskFileItemFactory();

                // Configure a repository (to ensure a secure temp location is used)
                ServletContext servletContext = this.getServletConfig().getServletContext();
                File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
                factory.setRepository(repository);

                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);

                // Parse the request
                List<FileItem> items = upload.parseRequest(request);

                Iterator<FileItem> iter = items.iterator();
                while (iter.hasNext()) {
                    FileItem item = iter.next();

                    if (!item.isFormField()) {
                        // to ensure no name collisions if multithreaded
                        String tmpId = java.util.UUID.randomUUID().toString();
                        File uploadedFile = new File(tmpdir, tmpId + ".gz");

                        item.write(uploadedFile);
                        debugLogger.log("write ok");

                        // todo remove
                        uploadedFile.setReadable(true);

                        File decompressedDir = new File(tmpdir, tmpId);
                        decompressedDir.mkdir();
                        Compression.unzip(uploadedFile, decompressedDir );
                        debugLogger.log("decompress ok");


                        File[] decompressedContents = decompressedDir.listFiles();
                        if (decompressedContents == null){
                            throw new ZipException("File not dir found after decompression, probably zip error");
                        } else if (decompressedContents.length == 0){
                            throw new FileNotFoundException("Empty zip dir received");
                        } else if (!decompressedContents[0].isDirectory()){
                            // this should be changed to allow for a more flexible solution;
                            throw new FileUploadException("zipped content is not a dir");
                        }

                        File usedDir = decompressedContents[0];
                        debugLogger.log("found inner dir");

                        File[] runContents = usedDir.listFiles();
                        if (runContents == null){
                            throw new FileUploadException("Project dir is not a dir");
                        } else if (runContents.length == 0){
                            throw new FileNotFoundException("Project dir is empty");
                        }

                        Optional<File> configFileOption = Arrays.stream(runContents)
                                .filter(pathname -> pathname.getName().equals(ApiConfig.configFile.getName()))
                                .findFirst();

                        if (configFileOption.isEmpty()){
                            throw new FileUploadException("Config file not found");
                        }

                        File configFile = configFileOption.get();
                        debugLogger.log("config found");


                        // this should be extracted to another class
                        RunType rt = ApiConfig.getRunType(configFile);
                        if (rt == RunType.JAVA){
                            JavaApiConfig typeConfig = new JavaApiConfig(configFile);
                            int ticket_id = PsqlInterface.insertNewTicket(typeConfig.getRunType(),typeConfig.getReturnMail(),typeConfig.getPriority());

                            // this can't possibly be the best solution
                            System.out.println(usedDir.exists());
                            ProcessBuilder builder = new ProcessBuilder();
                            builder.command("mv", usedDir.getCanonicalPath(), new File(runDir, "ticket_" + ticket_id).getCanonicalPath());
                            builder.start();


                            //Files.move(usedDir.toPath(), .toPath(), StandardCopyOption.REPLACE_EXISTING );
                            System.out.println("saved at: " + new File(runDir, "ticket_" + ticket_id).getCanonicalPath());
                        }

                        System.out.println("Full ok");

                        uploadedFile.delete();
                        decompressedDir.delete();

                    }
                }

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
