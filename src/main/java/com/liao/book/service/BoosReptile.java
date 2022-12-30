package com.liao.book.service;

import com.liao.book.entity.BookData;

import java.util.List;

/**
 * <p>
 * 电子书爬虫接口类
 * 将原水平的拆分，改为垂直的拆分，即一个抽象类表示一个电子书网站
 * 视图层面向爬虫工厂进行爬取
 * </p>
 *
 * @author LiAo
 * @since 2022-12-30
 */
public interface BoosReptile {

    /**
     * 根据名称爬取书籍列表
     *
     * @param bookName 书籍名称
     * @return 书籍列表
     */
    List<BookData> getBookList(String bookName);


    /**
     * 根据链接，爬取指定书籍章节列表列表
     *
     * @param link 书籍链接
     */
    void getBookChapterList(String link);


    /**
     * 根据链接爬取书籍指定章节内容
     *
     * @param link 书籍链接
     * @return 章节内容
     */
    String getBookChapterContent(String link);
}
