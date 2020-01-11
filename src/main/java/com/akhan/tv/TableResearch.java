package com.akhan.tv;

import com.akhan.tv.gen.GenEasDicNew;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: akhan
 * @description:
 * @date: 10:18 2019/12/31
 */
public class TableResearch extends JFrame {

    private JTextField txtAls = new JTextField("");

    private JMenuBar mBar;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenuItem miOpen;
    private JMenuItem miExit;

    private JList lsTable = new JList();
    private DefaultTableModel model = null;
    private JTable cols = null;
    private HashMap<String, Entity> data = null;
    private HashMap<String, String> rel = null;
    private HashMap<String, String> em = null;
    private volatile boolean isFinish = true;
    private final Object mutex = new Object();

    private final ArrayList<Entity> NULL = new ArrayList<>();

    private String _key;
    private final LoadingCache<String, ArrayList<Entity>> RESULT_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(20)
            .maximumSize(100)
            .expireAfterAccess(100, TimeUnit.SECONDS)
            .build(new CacheLoader<String, ArrayList<Entity>>() {
                @Override
                public ArrayList<Entity> load(String s) throws Exception {
                    return NULL;
                }
            });

    private JScrollPane sp1 = null;
    private JScrollPane sp2 = null;

    public TableResearch() {
        mBar = new JMenuBar();
        setJMenuBar(mBar);
        fileMenu = new JMenu("文件");
        editMenu = new JMenu("编辑");
        mBar.add(fileMenu);
        mBar.add(editMenu);

        miOpen = new JMenuItem("重新载入");
        miExit = new JMenuItem("退出");


        fileMenu.add(miOpen);
        fileMenu.addSeparator();
        fileMenu.add(miExit);


        miOpen.addActionListener(e -> {
            new GenEasDicNew();

            loadData();
        });
        miExit.addActionListener(e -> {
            int judge = JOptionPane.showConfirmDialog(TableResearch.this, "确认退出？");
            if (judge == JOptionPane.OK_OPTION) {
                System.exit(0);
            }
        });

        setTitle("表结构");
        model = new DefaultTableModel();
        model.addColumn("列名");
        model.addColumn("别名");
        model.addColumn("描述");
        model.addColumn("数据类型");
        model.addColumn("关联关系");
        cols = new JTable(model);

        loadData();

        txtAls.setPreferredSize(new Dimension(300, 30));

        lsTable.setModel(new DefaultListModel());

        txtAls.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }

            @SneakyThrows
            public void keyReleased(KeyEvent e) {
                res();
            }

            public void keyTyped(KeyEvent e) {
            }
        });
        lsTable.addListSelectionListener(e -> loadDetail());
        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, 2));
        p1.add(new JLabel("表名称:"));
        p1.add(txtAls);

        getContentPane().add(p1, "North");

        getContentPane().add(new JLabel("<html><font> 版本: V0.9    日期：2020 </font> <font color=blue>EMail:wzysz555@foxmail.com</font></html>"), "South");

        sp1 = new JScrollPane(lsTable);

        sp2 = new JScrollPane(cols);

        JSplitPane p2 = new JSplitPane(1, false, sp1, sp2);

        getContentPane().add(p2, "Center");

        pack();

        setLocationRelativeTo(null);
        setDefaultCloseOperation(3);
        setBackground(Color.GRAY);

        FontUIResource font = new FontUIResource(new Font("微软雅黑",Font.PLAIN,16));
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }

        setVisible(true);
    }

    private void loadData() {
        try {
            data = null;
            rel = null;
            em = null;
            System.gc();

            ObjectInputStream out = new ObjectInputStream(new FileInputStream("." + File.separator + "dic.db"));
            data = ((HashMap) out.readObject());
            rel = ((HashMap) out.readObject());
            em = ((HashMap) out.readObject());

            out.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void res() throws ExecutionException {
        if (isFinish) {
            synchronized (mutex) {
                if (isFinish) {
                    isFinish = false;

                    _key = StringUtils.defaultIfEmpty(txtAls.getText(), "").toLowerCase();

                    DefaultListModel m = (DefaultListModel) lsTable.getModel();
                    m.removeAllElements();

                    ArrayList<Entity> foo = null;

                    foo = RESULT_CACHE.get(_key);
                    if (foo.isEmpty()) {
                        foo = data.values()
                                .stream()
                                .filter(entity -> StringUtils.contains(StringUtils.lowerCase(entity.getName()), _key) ||
                                        StringUtils.contains(StringUtils.lowerCase(entity.getLabel()), _key))
                                .collect(Collectors.toCollection(ArrayList::new));
                        RESULT_CACHE.put(_key, foo);
                    }
                    foo.forEach(m::addElement);
                    isFinish = true;
                }
            }
        }
    }

    private void loadDetail() {
        Entity en = (Entity) lsTable.getSelectedValue();
        if (en == null) {
            return;
        }
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        model.addRow(new String[]{en.getTableMap(), en.getLabel(), en.getName(), "", ""});
        model.addRow(new String[]{"", "", "", "", ""});

        while (en != null) {
            for (int i = 0; i < en.getColumns().size(); i++) {
                Pro c = (Pro) en.getColumns().get(i);
                String relEnName = "";

                if (c.relation != null) {
                    String relName = rel.get(c.relation);

                    if (relName != null) {
                        Entity ent = data.get(relName);
                        if (ent != null) {
                            relEnName = ent.getTableMap();
                        }
                    }

                }

                if ((c.em != null) && (em.get(c.em) != null)) {
                    relEnName = em.get(c.em);
                }

                model.addRow(new String[]{c.tblMap, c.label, c.desc, c.datatype, relEnName});
            }
            en = data.getOrDefault(en.getParent(), null);
        }
    }
}
