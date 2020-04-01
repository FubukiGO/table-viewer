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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
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

    private DecimalFormat df = new DecimalFormat("#");

    private JTextField txtAls = new JTextField("");
    private JTextField txtAls2 = new JTextField("");

    private int nextFoundRow = 0;
    private int nextFoundCol = 0;

    private JList lsTable = new JList();
    private JTable detailTable = new JTable();
    private JPanel sp2box = new JPanel();
    private JPanel searchBox = new JPanel();

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
        JMenuItem miFind = new JMenuItem("查找");


        fileMenu.add(miOpen);
        fileMenu.addSeparator();
        fileMenu.add(miExit);
        editMenu.add(miFind);


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
        miFind.addActionListener(e -> switchSearchBox());

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

        txtAls2.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                resetLastFoundPosition();
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
            searchDetailContent(false, false);
        });

        b2.setText(">");
        b2.setSize(30, 30);
        b2.setBackground(Color.LIGHT_GRAY);
        b2.addActionListener(e -> {
            searchDetailContent(true, false);
        });

        searchBox.setLayout(new BoxLayout(searchBox, BoxLayout.LINE_AXIS));
        searchBox.add(txtAls2);
        searchBox.add(b1);
        searchBox.add(b2);

        getContentPane().add(p1, BorderLayout.NORTH);

        getContentPane().add(new JLabel("<html><font> Version: V1.2    Date：2020-01 </font> <font color=blue>&nbsp;&nbsp;Created By akhan</font></html>"), BorderLayout.SOUTH);

        sp1 = new JScrollPane(lsTable);
        sp2 = new JScrollPane(detailTable);

        sp2box.setLayout(new BoxLayout(sp2box, BoxLayout.PAGE_AXIS));
        sp2box.add(searchBox);
        sp2box.add(sp2);
        sp2box.getActionMap().put("action_find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchSearchBox();
            }
        });

        JSplitPane p2 = new JSplitPane(1, false, sp1, sp2box);

        getContentPane().add(p2, BorderLayout.CENTER);

        pack();

        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBackground(Color.GRAY);

        FontUIResource font = new FontUIResource(new Font("微软雅黑", Font.PLAIN, 16));
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }

        sp2box.getInputMap().put(KeyStroke.getKeyStroke("control A"), "action_find");

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
        resetLastFoundPosition();
    }

    private void searchDetailContent(boolean asc, boolean isSecond) {
        String search = txtAls2.getText().toLowerCase();

        if (StringUtils.isBlank(search)) return;

        boolean found = false;

        Vector row = model.getDataVector();

        int rowCnt = model.getRowCount();
        //int colCnt = model.getColumnCount();
        int colCnt = 2;


        if (asc && nextFoundRow + 1 == rowCnt) {
            nextFoundRow = 0;
        } else if (!asc && nextFoundRow == 0) {
            nextFoundRow = rowCnt - 1;
        } else {
            nextFoundRow = ps(asc, nextFoundRow);
        }

        l1:
        for (int i = nextFoundRow; asc ? i < rowCnt : i > -1; i = ps(asc, i)) {
            Vector col = (Vector) row.elementAt(i);
            for (int j = 0; j < colCnt; j++) {

                String v = String.valueOf(col.elementAt(j));
                if (StringUtils.isNotBlank(v)) {
                    if (v.toLowerCase().contains(search)) {
                        detailTable.setRowSelectionInterval(i, i);

                        nextFoundRow = i;
                        nextFoundCol = j;

                        found = true;

                        moveScrollBarByFocus();
                        break l1;
                    }
                }
            }
        }

        if (!found && !isSecond) {
            resetLastFoundPosition();
            searchDetailContent(asc, true);
        }
    }

    private int ps(boolean _f, int _a) {
        if (_f) return _a + 1;
        else return _a - 1;
    }

    private void resetLastFoundPosition() {
        nextFoundRow = 0;
        nextFoundCol = 0;
    }

    private void moveScrollBarByFocus() {
        JScrollBar scrollBar = sp2.getVerticalScrollBar();
        int _m = scrollBar.getMaximum();
        int _s = scrollBar.getMinimum();
        scrollBar.setValue(Integer.parseInt(df.format((double) detailTable.getSelectedRow() / detailTable.getRowCount() * _m)));
    }

    private void switchSearchBox() {
        if (sp2box.isVisible()) {
            sp2box.setVisible(false);
        } else {
            search.setVisible(true);
        }
    }
}
