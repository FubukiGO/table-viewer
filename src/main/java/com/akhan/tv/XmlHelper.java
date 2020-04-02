package com.akhan.tv;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author: akhan
 * @description:
 * @date: 10:18 2019/12/31
 */
public class XmlHelper {

    private static final ThreadLocal<SAXReader> SAX_READER_THREAD_LOCAL = new ThreadLocal<>();

    public static Document getDocument(InputStream inputStream)
            throws MalformedURLException, DocumentException {
        Document document = SAX_READER_THREAD_LOCAL.get().read(inputStream);

        return document;
    }

    public static Document createDocument() {
        return DocumentHelper.createDocument();
    }

    public static Document getDocument(File file)
            throws MalformedURLException, DocumentException {
        Document document = SAX_READER_THREAD_LOCAL.get().read(file);

        return document;
    }

    public static Document getDocument(File file, String format)
            throws MalformedURLException, DocumentException, FileNotFoundException {
        Document document = SAX_READER_THREAD_LOCAL.get().read(new FileInputStream(file), format);

        return document;
    }

    public static Document getDocument(URL url)
            throws MalformedURLException, DocumentException {
        Document document = SAX_READER_THREAD_LOCAL.get().read(url);

        return document;
    }

    public static Writer createWriter(OutputStream outStream, String encoding) throws UnsupportedEncodingException {
        return new BufferedWriter(
                new OutputStreamWriter(outStream, encoding));
    }

    public static void save(Document document, OutputStream outputStream) {
        try {
            OutputFormat format = OutputFormat.createCompactFormat();
            format.setEncoding("GBK");
            format.setNewlines(true);
            format.setIndentSize(2);
            format.setTrimText(false);

            XMLWriter xmlWriter = new XMLWriter(
                    outputStream, format);

            xmlWriter.write(document);

            xmlWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void save(Document document, File f) {
        try {
            XMLWriter writer = new XMLWriter(new FileWriter(f));

            writer.write(document);

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void saveAsFormat(Document document, File f) {
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new FileWriter(f), format);

            writer.write(document);

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void init() {
        SAX_READER_THREAD_LOCAL.set(new SAXReader());
    }

    public static void clear() {
        SAX_READER_THREAD_LOCAL.remove();
    }
}
