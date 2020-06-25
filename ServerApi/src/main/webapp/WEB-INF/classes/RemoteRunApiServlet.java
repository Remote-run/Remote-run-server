
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

//@WebServlet(name="Api",urlPatterns={"/Api"})
public class RemoteRunApiServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
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

        System.out.println("Recived rest");
        File outFile = new File("/test.gzip");
        outFile.createNewFile();
        System.out.println("11");
        FileOutputStream outputStream = new FileOutputStream(outFile);
        System.out.println("2");
        InputStream inputStream = request.getInputStream();
        System.out.println("3");
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        System.out.println("starting dtata read");

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        System.out.println("WTF done");
        System.out.println(outFile.getCanonicalPath());

        //String name = request.getParameter("name");
        //String about = request.getParameter("about");
        //int birthYear = Integer.parseInt(request.getParameter("birthYear"));

        outputStream.close();
        inputStream.close();

        Compression.unzip(outFile.getCanonicalFile(), new File("/runvol/out.zip"));

        response.getOutputStream().println("all ok");

    }
}
