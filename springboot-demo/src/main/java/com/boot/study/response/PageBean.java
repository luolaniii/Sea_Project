package com.boot.study.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageBean<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页
     */
    private int pageNum;

    /**
     * 每页的数量
     */
    private int pageSize;

    /**
     * 当前页的数量
     */
    private int size;

    /**
     * 总记录数
     */
    private int total;

    /**
     * 总页数
     */
    private int pages;

    /**
     * 结果集
     */
    private List<T> list;

    public PageBean() {
    }

    private PageBean(IPage<?> iPage) {
        this(iPage, null);
    }

    private PageBean(IPage<?> iPage, List<T> list) {
        this.pageNum = (int) iPage.getCurrent();
        this.pageSize = (int) iPage.getSize();
        this.pages = (int) iPage.getPages();
        this.list = list;
        this.size = iPage.getRecords().size();
        this.total = (int)iPage.getTotal();
    }

    public static <T> PageBean<T> page(IPage<?> iPage, List<T> list) {
        return new PageBean<>(iPage, list);
    }


    public static <T> PageBean<T> pageNull(IPage<?> iPage) {
        return new PageBean<>(iPage);
    }
}
