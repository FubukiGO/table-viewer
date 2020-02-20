package com.akhan.tv.gen;

import com.akhan.tv.Entity;
import com.akhan.tv.Pro;
import com.akhan.tv.XmlHelper;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author: akhan
 * @description:
 * @date: 10:16 2019/12/31
 */
public class GenEasDicNew {

    private HashMap<String, Entity> data = Maps.newHashMapWithExpectedSize(300);
    private HashMap<String, String> rel = Maps.newHashMapWithExpectedSize(300);
    private HashMap<String, String> em = Maps.newHashMapWithExpectedSize(300);

    private ConcurrentLinkedQueue<String> jarQueue = Queues.newConcurrentLinkedQueue();
    private ExecutorService threadPool = null;
    private final int THREAD_COUNT = 8;

    private JFrame frame = new JFrame();
    private JTextArea txtOut = new JTextArea();
    private JLabel title = new JLabel();
    private JButton btnClose = new JButton("关闭");

    public GenEasDicNew() {

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException e1) {
            e1.printStackTrace();
        }

        try {
            System.setOut(new PrintStream(new FileOutputStream(new File("EAS数据字典.txt"))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        JFileChooser f = new JFileChooser();
        f.setCurrentDirectory(new File("."));
        f.setFileSelectionMode(2);
        f.showOpenDialog(null);
        File file = f.getSelectedFile();
        if ((file == null) || (!file.exists())) {
            System.exit(0);
        }

        createFrame();
        listDir(new File("." + File.separator + "metas").getAbsolutePath());
        listDir(file.getAbsolutePath());

        threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            threadPool.execute(new FileReader());
        }

        while (jarQueue.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        display();

        title.setText("运行成功");
        out("运行成功，查看文件[EAS数据字典.txt]!");
        btnClose.setEnabled(true);
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("." + File.separator + "dic.db"));
            out.writeObject(data);
            out.writeObject(rel);
            out.writeObject(em);

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createFrame() {
        JScrollPane p = new JScrollPane();
        title.setText("程序运行中...");
        frame.getContentPane().add(title, "North");
        frame.getContentPane().add(p, "Center");
        JPanel pBtn = new JPanel();
        pBtn.add(btnClose);
        btnClose.setEnabled(false);
        frame.getContentPane().add(pBtn, "South");
        btnClose.addActionListener(e -> close());
        p.getViewport().add(txtOut);

        frame.setSize(600, 400);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
        frame.setUndecorated(true);
        frame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void close() {
        frame.dispose();
        //System.exit(1);
    }

    public void out(String out) {
        txtOut.append("\n" + out);
        txtOut.setCaretPosition(txtOut.getText().length());
    }

    public void readFile(InputStream file) throws MalformedURLException, DocumentException {
        Document doc = XmlHelper.getDocument(file);
        HashMap<String, String> map = Maps.newHashMapWithExpectedSize(20);
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
                map.put(rs.attributeValue("key"), e.attributeValue("value"));
                break;
            }

        }

        Element cols = tb.element("properties");
        if (cols == null) {
            return;
        }
        List cs = cols.elements();

        String tblName = "";
        if ((tb.element("table") != null) && (tb.element("table").elements("key") != null)) {
            List keys = tb.element("table").elements("key");
            for (int i = 0; i < keys.size(); i++) {
                Element key = (Element) keys.get(i);
                if (!"name".equals(key.attributeValue("name")))
                    continue;
                tblName = key.attributeValue("value").toUpperCase();
                break;
            }

        }

        String parent = null;
        String pp = "";
        String pn = "";
        if (tb.element("baseEntity") != null) {
            List keys = tb.element("baseEntity").elements();
            for (int i = 0; i < keys.size(); i++) {
                Element key = (Element) keys.get(i);
                if ("package".equals(key.attributeValue("name"))) {
                    pp = key.attributeValue("value");
                }
                if (!"name".equals(key.attributeValue("name"))) {
                    continue;
                }
                pn = key.attributeValue("value");
            }

            parent = pp + "." + pn;
        }

        Entity ent = new Entity();
        ent.setKey(tb.element("package").getTextTrim() + "." + tb.element("name").getTextTrim());
        ent.setName(tb.element("name").getTextTrim());
        ent.setTableMap(tblName);
        ent.setType(tb.element("bosType").getTextTrim());
        ent.setParent(parent);

        ent.setLabel(map.get(tb.element("alias").getTextTrim()));
        data.put(ent.getKey(), ent);

        for (int i = 0; i < cs.size(); i++) {
            Pro p = new Pro();
            ent.addColumn(p);
            Element col = (Element) cs.get(i);

            p.setName(col.element("name").getTextTrim());
            if ((col.element("mappingField") != null) && (col.element("mappingField").element("key") != null)) {
                p.setTblMap(col.element("mappingField").element("key").attributeValue("value"));
            }

            p.setLabel(map.get(col.element("alias").getTextTrim()));

            if (col.element("relationship") != null) {
                List ch = col.element("relationship").elements();
                String pa = null;
                String name = null;
                for (int j = 0; j < ch.size(); j++) {
                    Element e = (Element) ch.get(j);

                    if ("package".equals(e.attributeValue("name"))) {
                        pa = e.attributeValue("value");
                    } else {
                        if (!"name".equals(e.attributeValue("name"))) {
                            continue;
                        }
                        name = e.attributeValue("value");
                    }
                }

                p.setRelation(pa + "." + name);
            }

            if (col.element("metadataRef") != null) {
                p.setEm(col.elementText("metadataRef").trim());
            }

            p.setDatatype(col.elementText("dataType"));
            p.setDesc(map.get(col.elementText("description")));
        }
    }

    public void readEnumFile(InputStream file) throws MalformedURLException, DocumentException, FileNotFoundException {
        Document doc = XmlHelper.getDocument(file);
        HashMap<String, String> map = Maps.newHashMapWithExpectedSize(20);

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
                map.put(rs.attributeValue("key"), e.attributeValue("value"));
                break;
            }

        }

        String pk = tb.elementText("package");
        String name = tb.elementText("name");
        Element es = tb.element("enumValues");
        if (es == null) {
            return;
        }
        List els = es.elements();
        StringBuffer bf = new StringBuffer(100);

        for (int i = 0; i < els.size(); i++) {
            Element _enum = (Element) els.get(i);

            String key = map.get(_enum.elementText("alias")) + "=" + _enum.elementText("value") + ",";
            bf.append(key);
        }
        if (els.size() > 0) {
            bf.deleteCharAt(bf.length() - 1);
        }

        em.put(pk + "." + name, bf.toString());
    }

    public void readRelFile(InputStream file) throws MalformedURLException, DocumentException, FileNotFoundException {
        Document doc = XmlHelper.getDocument(file);
        Element tb = doc.getRootElement();
        String pk = tb.elementText("package");
        String name = tb.elementText("name");
        Element sup = tb.element("supplierObject");
        List ch = sup.elements();
        String spa = null;
        String sname = null;
        for (int j = 0; j < ch.size(); j++) {
            Element e = (Element) ch.get(j);

            if ("package".equals(e.attributeValue("name"))) {
                spa = e.attributeValue("value");
            } else {
                if (!"name".equals(e.attributeValue("name"))) {
                    continue;
                }
                sname = e.attributeValue("value");
            }
        }

        String skey = spa + "." + sname;
        String key = pk + "." + name;
        rel.put(key, skey);
    }

    public void listJar(String jar) throws IOException {
        out("扫描:" + jar);

        JarFile jarfile = new JarFile(
                jar);

        for (Enumeration e = jarfile.entries(); e.hasMoreElements(); ) {
            String key = String.valueOf(e.nextElement());
            if (key.endsWith("entity")) {
                ZipEntry entry = jarfile.getEntry(key);
                try {
                    out("\t正在解析:" + key);
                    readFile(jarfile.getInputStream(entry));
                } catch (DocumentException e1) {
                    e1.printStackTrace();
                }

            } else if (key.endsWith("relation")) {
                ZipEntry entry = jarfile.getEntry(key);
                try {
                    out("\t正在解析:" + key);
                    readRelFile(jarfile.getInputStream(entry));
                } catch (DocumentException e1) {
                    e1.printStackTrace();
                }
            } else {
                if (!key.endsWith(".enum"))
                    continue;
                ZipEntry entry = jarfile.getEntry(key);
                try {
                    out("\t正在解析:" + key);
                    readEnumFile(jarfile.getInputStream(entry));
                } catch (DocumentException e1) {
                    e1.printStackTrace();
                }

            }

        }

        jarfile.close();
    }

    public void listDir(String dir) {
        File f = new File(dir);
        if (f.isDirectory()) {
            File[] cf = f.listFiles();
            for (int i = 0; i < cf.length; i++) {
                listDir(cf[i].getAbsolutePath());
            }
        } else {
            jarQueue.offer(dir);
        }

    }

    class FileReader implements Runnable {
        @Override
        public void run() {
            XmlHelper.init();
            while (jarQueue.size() > 0) {
                String file = jarQueue.poll();
                if (file.endsWith(".jar")) {
                    try {
                        listJar(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (file.endsWith(".entity")) {
                    try {
                        out("\t正在解析:" + file);
                        readFile(new FileInputStream(new File(file)));
                    } catch (MalformedURLException | FileNotFoundException | DocumentException e) {
                        e.printStackTrace();
                    }
                } else if (file.endsWith(".relation")) {
                    try {
                        out("\t正在解析:" + file);
                        readRelFile(new FileInputStream(new File(file)));
                    } catch (MalformedURLException | FileNotFoundException | DocumentException e) {
                        e.printStackTrace();
                    }
                } else if (file.endsWith(".enum")) {
                    try {
                        out("\t正在解析:" + file);
                        readEnumFile(new FileInputStream(new File(file)));
                    } catch (MalformedURLException | FileNotFoundException | DocumentException e) {
                        e.printStackTrace();
                    }
                }
            }
            XmlHelper.clear();
        }
    }

    public void display() {
        out("正在输出文件...");
        List l = new ArrayList(data.size());

        l.addAll(data.values());

        l.sort((arg0, arg1) -> {
            Entity a = (Entity) arg0;
            Entity b = (Entity) arg1;

            return a.getKey().compareTo(b.getKey());
        });
        for (Object o : l) {
            Entity enty = (Entity) o;
            if ((enty.getKey().contains("com.kingdee.eas.framework")) || (enty.getKey().contains("com.kingdee.eas.bim"))) {
                continue;
            }
            Entity tmpEnty = enty;
            List cls = new ArrayList();

            while (tmpEnty != null) {
                cls.addAll(tmpEnty.getColumns());

                tmpEnty = data.get(tmpEnty.getParent());
            }

            cls.sort((arg0, arg1) -> {
                Pro a = (Pro) arg0;
                Pro b = (Pro) arg1;

                return a.getName().compareTo(b.getName());
            });
            System.out.println(enty.getName() + "\t" + enty.getTableMap() + " " + "\t" + enty.getLabel());
            System.out.println("{");
            for (int j = 0; j < cls.size(); j++) {
                Pro p = (Pro) cls.get(j);
                System.out.println("\t" + p.getName() + "\t" + p.getTblMap() + " " + "\t" + p.getLabel() + "\t" + p.getRelation());
            }
            System.out.println("}");
        }
    }


}
