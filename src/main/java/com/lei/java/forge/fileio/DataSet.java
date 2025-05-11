package com.lei.java.forge.fileio;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * DataSet
 * </p>
 *
 * @author 伍磊
 */
public class DataSet {

    public String name;
    public int size;

    public DataSet(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public static List loadTestDataSet() {
        List<DataSet> testDataSet = new ArrayList<>();
        testDataSet.add(new DataSet("64B", 64));
        testDataSet.add(new DataSet("128B", 128));
        testDataSet.add(new DataSet("512B", 512));
        testDataSet.add(new DataSet("1K", 1024));
        testDataSet.add(new DataSet("2K", 2 * 1024));
        testDataSet.add(new DataSet("4K", 4 * 1024));
        testDataSet.add(new DataSet("8K", 8 * 1024));
        testDataSet.add(new DataSet("32K", 32 * 1024));
        testDataSet.add(new DataSet("64K", 64 * 1024));
        testDataSet.add(new DataSet("1M", 1024 * 1024));
        testDataSet.add(new DataSet("32M", 32 * 1024 * 1024));
        testDataSet.add(new DataSet("64M", 64 * 1024 * 1024));
        testDataSet.add(new DataSet("512M", 512 * 1024 * 1024));
        return testDataSet;
    }

}
