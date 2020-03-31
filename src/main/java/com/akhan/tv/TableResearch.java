package com.akhan.tv;

import com.akhan.tv.gen.GenEasDicNew;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import javafx.scene.control.TableSelectionModel;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: akhan
 * @description:
 * @date: 10:18 2019/12/31
 */
public class TableResearch extends JFrame {

    private final static Pattern TABLE_REGX = Pattern.compile("^[a-zA-Z][a-zA-Z_]+[a-zA-Z]$");

    private JTextField txtAls = new JTextField("");
    private JTextField txtAls2 = new JTextField("");

    private int lastFoundRow = 0;
    private int lastFoundCol = 0;

    private JList lsTable = new JList();
    private JTable detailTable = new JTable();
    private DefaultTableModel model = null;
    private HashMap<String, Entity> data = null;
    private HashMap<String, String> rel = null;
    private HashMap<String, String> em = null;
    private volatile boolean isFinish = true;
    private final Object mutex = new Object();

    private final ArrayList<Entity> NULL = new ArrayList<>();

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
        JMenuBar mBar = new JMenuBar();
        setJMenuBar(mBar);
        JMenu fileMenu = new JMenu("文件");
        JMenu editMenu = new JMenu("编辑");
        mBar.add(fileMenu);
        mBar.add(editMenu);

        JMenuItem miOpen = new JMenuItem("重新载入");
        JMenuItem miExit = new JMenuItem("退出");


        fileMenu.add(miOpen);
        fileMenu.addSeparator();
        fileMenu.add(miExit);


        miOpen.addActionListener(e -> {
            GenEasDicNew genEasDic = new GenEasDicNew();

            genEasDic = null;
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
        detailTable.setModel(model);
        detailTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loadData();

        txtAls.setPreferredSize(new Dimension(300, 30));
        txtAls2.setPreferredSize(new Dimension(100, 30));

        lsTable.setModel(new DefaultListModel());

        txtAls.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            @SneakyThrows
            public void keyReleased(KeyEvent e) {
                res();
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
        lsTable.addListSelectionListener(e -> loadDetail());
        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
        p1.add(new JLabel("表名称:"));
        p1.add(txtAls);


        JButton b1 = new JButton();
        JButton b2 = new JButton();
        b1.setText("<");
        b1.setSize(30, 30);
        b1.setBackground(Color.LIGHT_GRAY);
        b1.addActionListener(e -> {
            searchDetailContent(false);
        });

        b2.setText(">");
        b2.setSize(30, 30);
        b2.setBackground(Color.LIGHT_GRAY);
        b2.addActionListener(e -> {
            searchDetailContent(true);
        });

        JPanel searchBox = new JPanel();
        searchBox.setLayout(new BoxLayout(searchBox, BoxLayout.LINE_AXIS));
        searchBox.add(txtAls2);
        searchBox.add(b1);
        searchBox.add(b2);

        getContentPane().add(p1, "North");

        getContentPane().add(new JLabel("<html><font> Version: V1.2    Date：2020-01 </font> <font color=blue>&nbsp;&nbsp;Created By akhan</font></html>"), "South");

        sp1 = new JScrollPane(lsTable);
        sp2 = new JScrollPane(detailTable);

        JPanel sp2box = new JPanel();
        sp2box.setLayout(new BoxLayout(sp2box, BoxLayout.PAGE_AXIS));
        sp2box.add(searchBox);
        sp2box.add(sp2);

        JSplitPane p2 = new JSplitPane(1, false, sp1, sp2box);

        getContentPane().add(p2, "Center");

        pack();

        setLocationRelativeTo(null);
        setDefaultCloseOperation(3);
        setBackground(Color.GRAY);

        FontUIResource font = new FontUIResource(new Font("微软雅黑", Font.PLAIN, 16));
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
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
            System.out.println("loading....");
            data = null;
            rel = null;
            em = null;
            System.gc();

            ObjectInputStream out = new ObjectInputStream(new FileInputStream("." + File.separator + "dic.db"));
            data = ((HashMap) out.readObject());
            rel = ((HashMap) out.readObject());
            em = ((HashMap) out.readObject());

            System.out.println("load success");
            out.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void res() throws ExecutionException {
        if (isFinish) {
            synchronized (mutex) {
                if (isFinish) {
                    long start = System.currentTimeMillis();

                    ArrayList<Entity> foo = null;
                    isFinish = false;

                    String _key = StringUtils.defaultIfEmpty(txtAls.getText(), "").toLowerCase();

                    DefaultListModel m = (DefaultListModel) lsTable.getModel();
                    m.removeAllElements();

                    foo = RESULT_CACHE.get(_key);

                    boolean isTable = TABLE_REGX.matcher(_key).matches();

                    if (foo.isEmpty()) {
                        Stream<Entity> s = data.values().stream();

                        foo = (isTable ? s.filter(entity -> StringUtils.contains(StringUtils.lowerCase(entity.getTableMap()), _key)) :
                                s.filter(entity -> StringUtils.contains(StringUtils.lowerCase(entity.getName()), _key) ||
                                        StringUtils.contains(StringUtils.lowerCase(entity.getLabel()), _key)))
                                .collect(Collectors.toCollection(ArrayList::new));
                        RESULT_CACHE.put(_key, foo);
                    }
                    foo.forEach(m::addElement);
                    foo.clear();

                    isFinish = true;

                    System.out.println(String.format("检索完成!%dms", System.currentTimeMillis() - start));
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

        //初始化最后查找坐标
        lastFoundCol = 0;
        lastFoundRow = 0;
    }

    private void searchDetailContent(boolean asc) {
        String search = txtAls2.getText();
        int rowCnt = model.getRowCount();
        int colCnt = model.getColumnCount();

        for (int i = 0; i < rowCnt; i++) {
            for (int j = 0; j < colCnt; j++) {
                String v = String.valueOf(model.getValueAt(asc ? i : rowCnt - i, asc ? j : colCnt - j));

                if(v.contains(search)) {
                    lastFoundRow = asc ? i : rowCnt - i;
                    lastFoundCol = asc ? j : colCnt - j;

                    detailTable.setRowSelectionInterval(lastFoundRow, lastFoundCol);
                    break;
                }
            }
        }
    }
}
