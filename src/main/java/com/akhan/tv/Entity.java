package com.akhan.tv;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: akhan
 * @description:
 * @date: 10:14 2019/12/31
 */
@Data
public class Entity implements Serializable {

     private String key = null;
    String name = null;
    String label = null;
    String tableMap = null;

    String type = null;
    private String parent = null;
    List columns = new ArrayList();

    public String getParent() {
        return this.parent;
    }

    public void setParent(String parent) {
        if (parent != null)
        {
            parent = parent.trim();
        }
        this.parent = parent;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String key) {
        if (key != null)
        {
            key = key.trim();
        }
        this.key = key;
    }

    public void addColumn(Pro p)
    {
        this.columns.add(p);
    }

    @Override
    public String toString()
    {
        return this.label + "(" + this.tableMap + ")";
    }
}
