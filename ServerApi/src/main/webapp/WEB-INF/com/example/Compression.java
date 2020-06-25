package com.example;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.zip.*;

// todo: this class sorly needs cleanup and polish
// TODO: Realy neds option for progress bar compression somtimes takes long
public class Compression {
    private static final File systemTmpDir = new File(System.getProperty("java.io.tmpdir"));

    public static void zip(File filePath) throws IOException{
        Compression.zip(filePath, null);
    }

    public static void zip(File filePath, File outPath) throws IOException{
        outPath = (outPath == null) ? new File(filePath.getCanonicalPath() + ".zip"): outPath;
        if (outPath.exists()) throw new FileAlreadyExistsException("Zip file alredy exists");
        if (!filePath.exists()) throw new IOException("Zip target not found");


        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outPath));
        zipOutputStream.setLevel(Deflater.NO_COMPRESSION); // using the zip as a tar
        if (filePath.isDirectory()){

            Compression.zipDirectory(filePath, filePath.getName(), zipOutputStream);

        } else if (filePath.isFile()){
            Compression.zipFile(filePath, filePath.getName(), zipOutputStream);
        }
        zipOutputStream.close();
    }

    public static void zip(File filePath, ZipOutputStream zipOutputStream, String zippedDirCurrentPath) throws IOException{
        if (!filePath.exists()) throw new IOException("Zip target not found");

        if (filePath.isDirectory()){
            Compression.zipDirectory(filePath, zippedDirCurrentPath, zipOutputStream);

        } else if (filePath.isFile()){
            Compression.zipFile(filePath, zippedDirCurrentPath, zipOutputStream);
        }
    }



    public static void gZip(File filePath) throws IOException{
        Compression.gZip(filePath, null);
    }

    public static void gZip(File filePath, File outPath) throws IOException{
        outPath = (outPath == null) ? new File(filePath.getCanonicalPath() + ".gzip"): outPath;
        if (outPath.exists()) throw new FileAlreadyExistsException("Zip file alredy exists");
        if (!filePath.exists()) throw new IOException("Zip target not found");



        File compressTarget = filePath;
        if (filePath.isDirectory()){
            File tmpZipFile = new File(Compression.systemTmpDir, filePath.getName() + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(tmpZipFile));
            zipOutputStream.setLevel(Deflater.NO_COMPRESSION); // using the zip as a tar
            Compression.zipDirectory(filePath, tmpZipFile.getName(), zipOutputStream);
            zipOutputStream.close();
            compressTarget = tmpZipFile;
        }

        Compression.gzipFileCompress(compressTarget, outPath);
    }

    public static void unzip(File filePath) throws IOException{
        Compression.unzip(filePath, null);
    }

    public static void unzip(File filePath, File outDir) throws IOException{
        if (outDir == null){
            outDir = Compression.getPathWithoutEnding(filePath);
            outDir.mkdir();
        }

        if (filePath.getName().endsWith(".gzip")){
            File tmpZipFile = new File(Compression.systemTmpDir.getCanonicalPath() + File.separator  + filePath.getName().substring(0,".gzip".length() + 1));
            gzipFileDecompress(filePath, tmpZipFile);
            _unzip(tmpZipFile, outDir);
        }else {
            _unzip(filePath, outDir);
        }
    }

    // -- private
    private static File getPathWithoutEnding(File file){
        return new File(file.getName().substring(0, file.getName().lastIndexOf(".") - 1));

    }

    private static void zipDirectory(File dirPath, String zippedDirPath, ZipOutputStream stream) throws IOException {
        zippedDirPath = zippedDirPath.endsWith(File.separator ) ? zippedDirPath : zippedDirPath + File.separator ;

        stream.putNextEntry(new ZipEntry(zippedDirPath));
        stream.closeEntry();


        for (File childFile : dirPath.listFiles()) {
            File childFp = new File(dirPath.getCanonicalPath() + File.separator + childFile.getName());
            if (childFile.isDirectory()){
                Compression.zipDirectory(childFp, zippedDirPath + childFile.getName() + File.separator , stream );
            } else {
                Compression.zipFile(childFp, zippedDirPath + childFile.getName(), stream);
            }
        }
    }


    private static void zipFile(File filePath, String zippedPath, ZipOutputStream stream) throws IOException{
        FileInputStream inputStream = new FileInputStream(filePath);
        stream.putNextEntry(new ZipEntry(zippedPath));

        byte[] bytes = new byte[1024];
        int  bytes_read;
        while ((bytes_read = inputStream.read(bytes)) >= 0) {
            stream.write(bytes, 0, bytes_read);
        }
        inputStream.close();
        stream.closeEntry();
    }

    private static void _unzip(File filePath, File outDir) throws IOException {
        if (!filePath.exists()) throw new IOException("unzip target not found");

        byte[] buffer = new byte[1024];

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filePath));
        ZipEntry zipEntry = zipInputStream.getNextEntry();

        while (zipEntry != null) {
            File entryFile = new File(outDir, zipEntry.getName());
            testForZipSlip(entryFile,outDir);
            if (zipEntry.isDirectory()){
                entryFile.mkdir();
            } else {
                FileOutputStream outputStream = new FileOutputStream(entryFile);

                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.close();
            }

            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.closeEntry();
        zipInputStream.close();
    }

    private static void testForZipSlip(File entryPath, File outDirPath) throws IOException{
        if (!entryPath.getCanonicalPath().startsWith(outDirPath.getCanonicalPath() + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + entryPath.getCanonicalPath());
        }
    }

    private static void gzipFileCompress(File toCompress, File outFile) throws FileNotFoundException, IOException {
        FileInputStream inputStream = new FileInputStream(toCompress);
        FileOutputStream outputStream = new FileOutputStream(outFile);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

        byte[] buffer = new byte[4096];
        int bytes_read;
        while ((bytes_read = inputStream.read(buffer)) >= 0) {
            gzipOutputStream.write(buffer, 0, bytes_read);
        }

        inputStream.close();
        gzipOutputStream.close();
        outputStream.close();
    }

    private static void gzipFileDecompress(File compressed, File outFile) throws  FileNotFoundException, IOException{
        FileInputStream inputStream = new FileInputStream(compressed);
        FileOutputStream outputStream = new FileOutputStream(outFile);
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);

        byte[] buffer = new byte[4096];
        int bytes_read;
        while ((bytes_read = gzipInputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytes_read);
        }

        inputStream.close();
        gzipInputStream.close();
        outputStream.close();
    }


}
