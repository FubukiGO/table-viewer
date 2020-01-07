package com.akhan.tv;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author: akhan
 * @description:
 * @date: 10:18 2019/12/31
 */
public class TableResearch extends JFrame {

    JTextField txtAls = new JTextField("");

    JList lsTable = new JList();
    DefaultTableModel model = null;
    JTable cols = null;
    Map data = null;
    Map rel = null;
    Map em = null;
    boolean isFinish = true;

    JScrollPane sp1 = null;
    JScrollPane sp2 = null;

    public TableResearch() {
        setTitle("表结构");
        this.model = new DefaultTableModel();
        this.model.addColumn("列名");
        this.model.addColumn("别名");
        this.model.addColumn("描述");
        this.model.addColumn("数据类型");
        this.model.addColumn("关联关系");
        this.cols = new JTable(this.model);
        try {
            ObjectInputStream out = new ObjectInputStream(new FileInputStream("." + File.separator + "dic.db"));
            this.data = ((Map) out.readObject());
            this.rel = ((Map) out.readObject());
            this.em = ((Map) out.readObject());

            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        this.txtAls.setPreferredSize(new Dimension(300, 30));

        this.lsTable.setModel(new DefaultListModel());

        this.txtAls.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                TableResearch.this.res();
            }

            public void keyTyped(KeyEvent e) {
            }
        });
        this.lsTable.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                TableResearch.this.loadDetial();
            }
        });
        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, 2));
        p1.add(new JLabel("表名称:"));
        p1.add(this.txtAls);

        getContentPane().add(p1, "North");

        getContentPane().add(new JLabel("<html><font color=blue> 版本: V0.9    日期：2020 </font> <font color=red>EMail:wzysz555@foxmail.com</font></html>"), "South");

        this.sp1 = new JScrollPane(this.lsTable);

        this.sp2 = new JScrollPane(this.cols);

        JSplitPane p2 = new JSplitPane(1, false, this.sp1, this.sp2);

        getContentPane().add(p2, "Center");

        pack();

        setLocationRelativeTo(null);
        setDefaultCloseOperation(3);
        setVisible(true);
    }

    public void res2() {
    }

    public void res() {
        if (!this.isFinish) {
            return;
        }

        this.isFinish = false;
        String key = null;

        key = this.txtAls.getText();

        if (key == null) {
            key = "";
        }
        key = key.toLowerCase();

        ArrayList l = new ArrayList(this.data.values());

        DefaultListModel m = (DefaultListModel) this.lsTable.getModel();
        m.removeAllElements();

        for (int i = 0; i < l.size(); i++) {
            Entity en = (Entity) l.get(i);
            String name = "";
            String label = "";
            if (en.tableMap != null) {
                name = en.tableMap.toLowerCase();
            }
            if (en.label != null) {
                label = en.label.toLowerCase();
            }

            if ((name.indexOf(key) < 0) && (label.indexOf(key) < 0))
                continue;
            m.addElement(en);
        }

        this.isFinish = true;
    }

    public void loadDetial() {
        Entity en = (Entity) this.lsTable.getSelectedValue();
        if (en == null) {
            return;
        }
        StringBuffer bf = new StringBuffer(100);
        while (this.model.getRowCount() > 0) {
            this.model.removeRow(0);
        }

        this.model.addRow(new String[]{en.tableMap, en.label, en.name, "", ""});
        this.model.addRow(new String[]{"", "", "", "", ""});

        while (en != null) {
            for (int i = 0; i < en.columns.size(); i++) {
                Pro c = (Pro) en.columns.get(i);
                String relEnName = "";

                if (c.relation != null) {
                    String relName = (String) this.rel.get(c.relation);

                    if (relName != null) {
                        Entity ent = (Entity) this.data.get(relName);
                        if (ent != null) {
                            relEnName = ent.tableMap;
                        }
                    }

                }

                if ((c.em != null) && (this.em.get(c.em) != null)) {
                    relEnName = (String) this.em.get(c.em);
                }

                this.model.addRow(new String[]{c.tblMap, c.label, c.desc, c.datatype, relEnName});
            }
            if (this.data.containsKey(en.getParent())) {
                en = (Entity) this.data.get(en.getParent());
            } else
                en = null;
        }
    }

    public static void main(String[] args) {
        new TableResearch();
    }
}
