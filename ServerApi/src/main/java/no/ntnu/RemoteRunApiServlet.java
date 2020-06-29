package no.ntnu;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;

//@WebServlet(name="Api",urlPatterns={"/Api"})
public class RemoteRunApiServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        mail_test.test();
        System.out.println("a;laskd;lsakd;lsakdl;ksal;k");
        String requestUrl = request.getRequestURI();
        String name = requestUrl.substring("/people/".length());

        Person person = DataStore.getInstance().getPerson("ada");

        if(person != null){
            String json = "{\n";
            json += "\"name\": " + JSONObject.quote(person.getName()) + ",\n";
            json += "\"about\": " + JSONObject.quote(person.getAbout()) + ",\n";
            json += "\"birthYear\": " + person.getBirthYear() + "\n";
            json += "}";
            response.getOutputStream().println(json);
        }
        else{
            //That person wasn't found, so return an empty JSON object. We could also return an error.
            response.getOutputStream().println("{asdasdasd}");
        }
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

                    if (item.isFormField()) {
                        String name = item.getFieldName();
                        String value = item.getString();
                        System.out.println(name);
                        System.out.println(value);
                    } else {
                        String fieldName = item.getFieldName();
                        String fileName = item.getName();
                        String contentType = item.getContentType();
                        boolean isInMemory = item.isInMemory();
                        long sizeInBytes = item.getSize();

                        File uploadedFile = new File("/tmp/test.gzip");
                        item.write(uploadedFile);
                        System.out.println("write Ok");
                        File outDir = new File("/runvol");
                        Compression.unzip(uploadedFile);
                        uploadedFile.delete();
                        File runableDir = new File("/tmp/test").listFiles()[0];
                        File configfile = runableDir.listFiles(pathname -> pathname.equals(ApiConfig.configFile))[0];
                        RunType rt = ApiConfig.getRunType(configfile);
                        if (rt == RunType.JAVA){
                            JavaApiConfig typeConfig = new JavaApiConfig(configfile);
                            int ticket_id = PsqlInterface.insertNewTicket(typeConfig.getRunType(),typeConfig.getReturnMail(),typeConfig.getPriority());
                            runableDir.renameTo(new File(outDir, "ticket_" + ticket_id));
                        }

                        System.out.println("decompress Ok");

                    }
                }

            }
        } catch (Exception e) {e.printStackTrace();}
    }
}
