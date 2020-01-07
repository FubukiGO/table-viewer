package com.akhan.tv;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: akhan
 * @description:
 * @date: 10:17 2019/12/31
 */
@Data
public class Pro implements Serializable {

    String name = null;
    String tblMap = null;
    String label = null;
    String datatype = null;
    String relation = null;
    String desc = null;
    String em = null;

    @Override
    public String toString() {
        return this.label + "(" + this.tblMap + ") " + this.relation;
    }
}
