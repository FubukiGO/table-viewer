package com.akhan.tv;

import javax.swing.*;

/**
 * @author: akhan
 * @description:
 * @date: 13:06 2020/1/8
 */
public class Client {
    public static void main(String[] args) {
        try {
            String os = System.getProperty("os.name");
            if(os.contains("OS X")){
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.mac.MacLookAndFeel");
            }else {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            }
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException e1) {
            e1.printStackTrace();
        }

        TableResearch.getInstance();
    }
}
