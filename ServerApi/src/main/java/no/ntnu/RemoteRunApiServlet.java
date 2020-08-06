package no.ntnu;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;


import no.ntnu.config.ApiConfig;
import no.ntnu.config.JavaApiConfig;
import no.ntnu.config.PythonApiConfig;
import no.ntnu.enums.RunType;
import no.ntnu.sql.TicketDbFunctions;

import no.trygvejw.debugLogger.DebugLogger;
import no.trygvejw.util.Compression;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


//@WebServlet(name="Api",urlPatterns={"/Api"})
@MultipartConfig//( fileSizeThreshold = 0, maxFileSize = 1048576000, maxRequestSize = 1048576000)
public class RemoteRunApiServlet extends HttpServlet {

    private final File tmpdir = new File (System.getProperty("java.io.tmpdir"));
    private final File runDir = new File("/runvol");
    private final DebugLogger dbl = new DebugLogger(true);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.getOutputStream().println("{asdasdasd}");
    }

   /* @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        try {
            //dbl.log(request.toString());
            //dbl.log(new InputStreamReader(request.getInputStream()).getEncoding());
            dbl.log();
            request.getHeaderNames().asIterator().forEachRemaining(s -> System.out.println(s + " -heder_of-> " + request.getHeader(s)));
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            in.lines().forEach(System.out::println);
            //in.close();

            dbl.log();

            String description = request.getParameter("description"); // Retrieves <input type="text" name="description">
            Part filePart = request.getPart("aa"); // Retrieves <input type="file" name="file">

            dbl.log("parts", request.getParts().toString());

            dbl.log("desc:", description);
            dbl.log("file part is null", filePart == null);



            dbl.log();
            if(filePart != null){
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


                UUID ticket_id = java.util.UUID.randomUUID();
                File uploadedFile = new File(tmpdir, ticket_id.toString() + ".zip.gz");



                FileOutputStream outputStream = new FileOutputStream(uploadedFile);
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
                InputStream fileContent = filePart.getInputStream();

                byte[] buffer = new byte[4096];
                int bytesRead = -1;

                while ((bytesRead = fileContent.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                fileContent.close();
                outputStream.close();

                dbl.log("write ok");

                File decompressedDir = new File(tmpdir, ticket_id.toString());
                decompressedDir.mkdir();
                Compression.unzip(uploadedFile, decompressedDir );
                dbl.log("decompress ok");


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
                dbl.log("found inner dir");

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
                dbl.log("config found");


                // this should be extracted to another class
                RunType rt = ApiConfig.getRunType(configFile);
                ApiConfig config = switch (rt){
                    case JAVA -> new JavaApiConfig(configFile);
                    case PYTHON -> new PythonApiConfig(configFile);
                    default -> null;
                };



                // this can't possibly be the best solution
                ProcessBuilder builder = new ProcessBuilder();
                builder.command("bash", "-c",String.join(" ","mv", usedDir.getCanonicalPath(), new File(runDir, "ticket_" + ticket_id).getCanonicalPath()));


                Process process = builder.start();
                process.waitFor();
                dbl.log("mv exit value", process.exitValue());




                //Files.move(usedDir.toPath(), .toPath(), StandardCopyOption.REPLACE_EXISTING );
                dbl.log("saved at: ", new File(runDir, "ticket_" + ticket_id).getCanonicalPath());
                TicketDbFunctions.insertNewTicket(ticket_id, config.getRunType(),config.getReturnMail(),config.getPriority());


                dbl.log("Full ok");

                uploadedFile.delete();
                decompressedDir.delete();



            }
        } catch (Exception e) {
            // TODO: more finess in catching here
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }*/

   @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        dbl.log(isMultipart);
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
                        UUID ticket_id = java.util.UUID.randomUUID();
                        File uploadedFile = new File(tmpdir, ticket_id.toString() + ".zip.gz");

                        item.write(uploadedFile);
                        dbl.log("write ok");

                        File decompressedDir = new File(tmpdir, ticket_id.toString());
                        decompressedDir.mkdir();
                        Compression.unzip(uploadedFile, decompressedDir );
                        dbl.log("decompress ok");


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
                        dbl.log("found inner dir");

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
                        dbl.log("config found");


                        // this should be extracted to another class
                        RunType rt = ApiConfig.getRunType(configFile);
                        ApiConfig config = switch (rt){
                            case JAVA -> new JavaApiConfig(configFile);
                            case PYTHON -> new PythonApiConfig(configFile);
                            default -> null;
                        };



                        // this can't possibly be the best solution
                        ProcessBuilder builder = new ProcessBuilder();
                        builder.command("bash", "-c",String.join(" ","mv", usedDir.getCanonicalPath(), new File(runDir, "ticket_" + ticket_id).getCanonicalPath()));


                        Process process = builder.start();
                        process.waitFor();
                        dbl.log("mv exit value", process.exitValue());




                        //Files.move(usedDir.toPath(), .toPath(), StandardCopyOption.REPLACE_EXISTING );
                        dbl.log("saved at: ", new File(runDir, "ticket_" + ticket_id).getCanonicalPath());
                        TicketDbFunctions.insertNewTicket(ticket_id, config.getRunType(),config.getReturnMail(),config.getPriority());


                        dbl.log("Full ok");

                        uploadedFile.delete();
                        decompressedDir.delete();


                    }
                }

            }
        } catch (Exception e) {
            // TODO: more finess in catching here
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
