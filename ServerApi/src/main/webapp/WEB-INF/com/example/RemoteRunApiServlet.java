package com.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
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
        File tmpOut = new File("/tmp/test.gzip");
        tmpOut.createNewFile();

        System.out.println("11");
        FileOutputStream outputStream = new FileOutputStream(tmpOut);
        System.out.println("2");
        InputStream inputStream = request.getInputStream();
        System.out.println("3");
        byte[] buffer = new byte[4096];
        int bytesRead;
        System.out.println("starting dtata read");


        while ((bytesRead = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytesRead);
        }
        System.out.println("WTF done");
        System.out.println(tmpOut);

        //String name = request.getParameter("name");
        //String about = request.getParameter("about");
        //int birthYear = Integer.parseInt(request.getParameter("birthYear"));

        outputStream.close();
        inputStream.close();

        System.out.println("4");


        System.out.println("5aa");
        File f = new File("/runvol");

        System.out.println(tmpOut.exists());
        System.out.println(tmpOut.canRead());
        System.out.println(tmpOut.setReadable(true));
        System.out.println(new File("/runvol/out").exists());
        System.out.println(new File("/runvol").canWrite());
        try{
            Compression.unzip(tmpOut, f);
        } catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println(e.toString());
        }

        System.out.println("5.2");
        //tmpOut.delete();
        System.out.println("5.5");

        response.getOutputStream().println("all ok");
        System.out.println("6");

    }
}
