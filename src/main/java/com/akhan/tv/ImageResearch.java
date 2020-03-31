package com.akhan.tv;

import com.google.common.collect.Maps;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author: akhan
 * @description:
 * @date: 10:16 2019/12/31
 */
public class ImageResearch {

    private HashMap<String, String> keyPath = Maps.newHashMapWithExpectedSize(300);
    private HashMap<String, String> jarPath = Maps.newHashMapWithExpectedSize(300);
    public String[][] data = null;

    public void init() {
        JFileChooser f = new JFileChooser();
        f.setCurrentDirectory(new File("."));
        f.setFileSelectionMode(2);
        f.showOpenDialog(null);
        File file = f.getSelectedFile();
        if ((file == null) || (!file.exists())) {
            System.exit(0);
        }

        File f1 = new File(file.getAbsoluteFile() + "\\lib");
        listImg(f1.getAbsolutePath());
        File f2 = new File(file.getAbsoluteFile() + "\\metas");
        listRes(f2.getAbsolutePath());
        int len = this.keyPath.keySet().size();
        this.data = new String[len][3];
        Iterator it = this.keyPath.keySet().iterator();
        int i = -1;
        while (it.hasNext()) {
            i++;
            String key = (String) it.next();
            String p = (String) this.keyPath.get(key);
            String jar = (String) this.jarPath.get(p);

            this.data[i] = new String[]{key, p, jar};
        }
    }

    public void readFile(InputStream file, String f)
            throws MalformedURLException, DocumentException, FileNotFoundException {
        Document doc = XmlHelper.getDocument(file);
        Map<String, String> map = Maps.newHashMap();
        Element tb = doc.getRootElement();
        Element resource = tb.element("resource");
        List rss = resource.elements("rs");
        for (int i = 0; i < rss.size(); i++) {
            Element rs = (Element) rss.get(i);

            List l = rs.elements("lang");
            for (int j = 0; j < l.size(); j++) {
                Element e = (Element) l.get(j);
                if (!"zh_CN".equals(e.attributeValue("locale")))
                    continue;
                map.put(rs.attributeValue("key"), e.attributeValue("value").substring(1));
                break;
            }

        }

        if ((tb.element("resourceItems") != null) && (tb.element("resourceItems").elements("resourceItem") != null)) {
            List cols = tb.element("resourceItems").elements("resourceItem");
            for (int i = 0; i < cols.size(); i++) {
                Element el = (Element) cols.get(i);
                this.keyPath.put(el.element("name").getTextTrim(), map.get(el.element("value").getTextTrim()));
            }
        }
    }

    public Vector getKeys() {
        Vector l = new Vector();
        Iterator i = this.keyPath.keySet().iterator();
        while (i.hasNext()) {
            l.add(i.next());
        }
        return l;
    }

    public void listJarGif(String jar)
            throws IOException {
        JarFile jarfile = new JarFile(
                jar);

        for (Enumeration e = jarfile.entries(); e.hasMoreElements(); ) {
            String key = String.valueOf(e.nextElement());
            if ((!key.endsWith(".gif")) && (!key.endsWith(".png")) && (!key.endsWith(".jpg"))) {
                continue;
            }

            this.jarPath.put(key, jar);
        }

        jarfile.close();
    }

    public void listJarRes(String jar)
            throws IOException {
        JarFile jarfile = new JarFile(
                jar);

        for (Enumeration e = jarfile.entries(); e.hasMoreElements(); ) {
            String key = String.valueOf(e.nextElement());
            if (!key.endsWith(".imageresource")) {
                continue;
            }
            ZipEntry entry = jarfile.getEntry(key);
            try {
                readFile(jarfile.getInputStream(entry), jar);
            } catch (DocumentException e1) {
                e1.printStackTrace();
            }

        }

        jarfile.close();
    }

    public void listImg(String dir) {
        File f = new File(dir);
        if (f.isDirectory()) {
            File[] cf = f.listFiles();
            for (int i = 0; i < cf.length; i++) {
                listImg(cf[i].getAbsolutePath());
            }

        } else if ((dir.endsWith(".jar")) && (dir.indexOf("resource") >= 0)) {
            try {
                listJarGif(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void listRes(String dir) {
        File f = new File(dir);
        if (f.isDirectory()) {
            File[] cf = f.listFiles();
            for (int i = 0; i < cf.length; i++) {
                listRes(cf[i].getAbsolutePath());
            }

        } else if ((dir.endsWith(".jar")) && (dir.indexOf("_common") >= 0)) {
            try {
                listJarRes(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void display() {
    }

    public String[] getPandJar(String key) {
        for (int i = 0; i < this.data.length; i++) {
            if (this.data[i][0].equals(key)) {
                String[] s = new String[3];
                s[0] = this.data[i][0];
                s[1] = this.data[i][1];
                s[2] = this.data[i][2];

                return s;
            }

        }

        return null;
    }

    public ImageIcon loadImage(String p, String jar) {
        if (jar != null) {
            JarFile jarfile = null;
            try {
                jarfile = new JarFile(
                        jar);
            } catch (IOException e2) {
                e2.printStackTrace();
            }

            for (Enumeration e = jarfile.entries(); e.hasMoreElements(); ) {
                String key = String.valueOf(e.nextElement());

                if (!p.equals(key)) {
                    continue;
                }

                ZipEntry entry = jarfile.getEntry(key);
                try {
                    ImageIcon im = new ImageIcon(ImageIO.read(jarfile.getInputStream(entry)));
                    return im;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }

        }

        return null;
    }

    public static void main(String[] args) {
        new ImageResearch().init();
    }
}
