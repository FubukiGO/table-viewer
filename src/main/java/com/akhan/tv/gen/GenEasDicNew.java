package com.akhan.tv.gen;

import com.akhan.tv.Entity;
import com.akhan.tv.Pro;
import com.akhan.tv.TableResearch;
import com.akhan.tv.XmlHelper;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author: akhan
 * @description:
 * @date: 10:16 2019/12/31
 */
public class GenEasDicNew extends JFrame {

    private Integer capacity = 300;
    private ConcurrentHashMap<String, Entity> data = new ConcurrentHashMap<>(capacity);
    private ConcurrentHashMap<String, String> rel = new ConcurrentHashMap<>(capacity);
    private ConcurrentHashMap<String, String> em = new ConcurrentHashMap<>(capacity);

    private volatile ConcurrentLinkedQueue<String> jarQueue = Queues.newConcurrentLinkedQueue();
    private ExecutorService threadPool = null;
    private final int THREAD_COUNT = 16;

    private JTextArea txtOut = new JTextArea();
    private JLabel title = new JLabel();
    private JButton btnClose = new JButton("关闭");
    private JProgressBar progressBar = new JProgressBar();

    public GenEasDicNew() {

        try {
            System.setOut(new PrintStream(new FileOutputStream(new File("log"))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        JFileChooser f = new JFileChooser();
        f.setCurrentDirectory(new File("."));
        f.setFileSelectionMode(2);
        f.showOpenDialog(null);
        File file = f.getSelectedFile();
        if ((file == null) || (!file.exists())) {
            dispose();
            return;
        }

        createFrame();
        listDir(new File("." + File.separator + "metas").getAbsolutePath());
        listDir(file.getAbsolutePath());

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("eas-dic-pool-%d").build();
        threadPool = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        for (int i = 0; i < THREAD_COUNT; i++) {
            threadPool.execute(new FileReader());
        }
        threadPool.shutdown();

//        while (jarQueue.size() > 0) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException ie) {
//                ie.printStackTrace();
//            }
//        }

        new Thread(new ProgressUpdater(jarQueue.size())).start();

    }

    private void createFrame() {
        JScrollPane p = new JScrollPane();
        title.setText("程序运行中...");
        getContentPane().add(title, "North");
        getContentPane().add(p, "Center");
        JPanel pBtn = new JPanel();
        pBtn.add(progressBar);
        pBtn.add(btnClose);
        btnClose.setEnabled(false);
        btnClose.setVisible(false);

        getContentPane().add(pBtn, "South");
        btnClose.addActionListener(e -> close());

        p.getViewport().add(txtOut);

        setSize(600, 400);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
        setUndecorated(true);
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void close() {
        TableResearch.getInstance().loadData();
        dispose();
        //System.exit(1);
    }

    private void out(String out) {
        txtOut.append("\n" + out);
        //显示光标位置
        txtOut.setCaretPosition(txtOut.getText().length());
    }

    private void readFile(InputStream file) throws MalformedURLException, DocumentException {
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

    private void readEnumFile(InputStream file) throws MalformedURLException, DocumentException, FileNotFoundException {
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

    private void readRelFile(InputStream file) throws MalformedURLException, DocumentException, FileNotFoundException {
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

    private void listJar(String jar) throws IOException {
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

    private void listDir(String dir) {
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

    class ProgressUpdater implements Runnable {
        private int oldSize = 0;
        private DecimalFormat df = new DecimalFormat("#");

        @Override
        public void run() {
            while (!jarQueue.isEmpty() || !threadPool.isShutdown()) {
                progressBar.setValue(Integer.parseInt(df.format((1 - ((double) jarQueue.size() / oldSize)) * 100)));

                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            printDicLog();
            writeDic();
        }

        public ProgressUpdater(int oldSize) {
            this.oldSize = oldSize;
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

    private void printDicLog() {
        out("正在保存日志...");
        List l = new ArrayList(data.size());

        l.addAll(data.values());

        l.sort((arg0, arg1) -> {
            Entity a = (Entity) arg0;
            Entity b = (Entity) arg1;

            return a.getKey().compareTo(b.getKey());
        });
        for (Object o : l) {
            Entity entry = (Entity) o;
            if ((entry.getKey().contains("com.kingdee.eas.framework")) || (entry.getKey().contains("com.kingdee.eas.bim"))) {
                continue;
            }
            Entity temp = entry;
            List cls = new ArrayList();

            while (temp != null) {
                cls.addAll(temp.getColumns());

                temp = temp.getParent() == null ? null : data.get(temp.getParent());
            }

            cls.sort((arg0, arg1) -> {
                Pro a = (Pro) arg0;
                Pro b = (Pro) arg1;

                return a.getName().compareTo(b.getName());
            });
            System.out.println(entry.getName() + "\t" + entry.getTableMap() + " " + "\t" + entry.getLabel());
            System.out.println("{");
            for (int j = 0; j < cls.size(); j++) {
                Pro p = (Pro) cls.get(j);
                System.out.println("\t" + p.getName() + "\t" + p.getTblMap() + " " + "\t" + p.getLabel() + "\t" + p.getRelation());
            }
            System.out.println("}");
        }
    }

    private void writeDic() {
        title.setText("运行成功");

        progressBar.setVisible(false);
        btnClose.setEnabled(true);
        btnClose.setVisible(true);

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

}
