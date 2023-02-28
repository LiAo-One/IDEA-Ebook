package com.liao.book.window;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.liao.book.dao.ReadingProgressDao;
import com.liao.book.entity.Chapter;
import com.liao.book.entity.DataCenter;
import com.liao.book.enums.ToastType;
import com.liao.book.factory.BeanFactory;
import com.liao.book.service.BookTextService;
import com.liao.book.service.impl.BookTextServiceImpl;
import com.liao.book.utile.ToastUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <p>
 * 全屏
 * </p>
 *
 * @author LiAo
 * @since 2021/1/14
 */

public class FullScreenReading {

    // 主体窗口
    private JPanel fullScreenJpanel;

    // 文章内容滑动
    private JScrollPane paneTextContent;

    // 书籍内容
    private JTextArea textContent;

    // 上一章按钮
    private JButton btnOn;

    // 下一章按钮
    private JButton underOn;

    // 章节列表
    private JComboBox<String> chapterList;

    // 跳转到指定章节
    private JButton jumpButton;

    // 字体加
    private JButton fontSizeDown;

    // 字体减
    private JButton fontSizeUp;

    // 滚动间距
    private JSlider scrollSpacing;

    // 字体默认大小
    private Integer fontSize = 12;

    // 全局模块对象
    private final Project project;

    // 用于判断是否是当前选项卡切换
    private Content lastSelectedContent = null;

    // 内容爬虫
    static BookTextService textService = (BookTextServiceImpl) BeanFactory
            .getBean("BookTextServiceImpl");

    // 阅读进度持久化
    static ReadingProgressDao instance = ReadingProgressDao.getInstance();

    // 窗口信息
    public JPanel getBookMainJPanel() {
        return fullScreenJpanel;
    }


    // 初始化数据
    private void init() {

        // 页面滚动步长
        JScrollBar jScrollBar = new JScrollBar();
        // 滚动步长为2
        jScrollBar.setMaximum(2);
        paneTextContent.setVerticalScrollBar(jScrollBar);

        chapterList.setPreferredSize(new Dimension(1200, 20));

        // 设置设备滑块 最大最小值
        scrollSpacing.setMinimum(0);
        scrollSpacing.setMaximum(20);

        scrollSpacing.setValue(2);
        // 设置滑块刻度间距
        scrollSpacing.setMajorTickSpacing(2);

        // 显示标签
        scrollSpacing.setPaintLabels(true);
        scrollSpacing.setPaintTicks(true);
        scrollSpacing.setPaintTrack(true);

        // 加载提示信息
        setComponentTooltip();

        // 加载阅读进度
        loadReadingProgress();
    }

    // 页面打开方法
    public FullScreenReading(Project project, ToolWindow toolWindow) {

        this.project = project;

        // 初始化信息
        init();

        // 上一章节跳转
        btnOn.addActionListener(e -> {
            // 等待鼠标样式
            setTheMouseStyle(Cursor.WAIT_CURSOR);

            if (instance.chapters.size() == 0 || instance.nowChapterIndex == 0) {
                ToastUtil.showToastMassage(project, "已经是第一章了", ToastType.ERROR);
                // 恢复默认鼠标样式
                setTheMouseStyle(Cursor.DEFAULT_CURSOR);
                return;
            }
            instance.nowChapterIndex = instance.nowChapterIndex - 1;
            // 加载阅读信息
            new LoadChapterInformation().execute();
        });

        // 下一章跳转
        underOn.addActionListener(e -> {

            // 等待鼠标样式
            setTheMouseStyle(Cursor.WAIT_CURSOR);

            if (instance.chapters.size() == 0 || instance.nowChapterIndex == instance.chapters.size()) {
                ToastUtil.showToastMassage(project, "已经是最后一章了", ToastType.ERROR);
                return;
            }

            instance.nowChapterIndex = instance.nowChapterIndex + 1;

            // 加载阅读信息
            new LoadChapterInformation().execute();
        });

        // 章节跳转事件
        jumpButton.addActionListener(e -> {
            // 等待鼠标样式
            setTheMouseStyle(Cursor.WAIT_CURSOR);

            if (instance.chapters.size() == 0 || instance.nowChapterIndex < 0) {
                ToastUtil.showToastMassage(project, "未知章节", ToastType.ERROR);
                return;
            }

            // 根据下标跳转
            instance.nowChapterIndex = chapterList.getSelectedIndex();

            // 加载阅读信息
            new LoadChapterInformation().execute();
        });

        // 字号调小按钮单击事件
        fontSizeDown.addActionListener(e -> {

            if (fontSize == 1) {
                ToastUtil.showToastMassage(project, "已经是最小的了", ToastType.ERROR);
                return;
            }

            // 调小字体
            fontSize--;
            textContent.setFont(new Font("", Font.BOLD, fontSize));
        });

        // 字体增大按钮
        fontSizeUp.addActionListener(e -> {
            // 调大字体
            fontSize++;
            textContent.setFont(new Font("", Font.BOLD, fontSize));
        });

        // 窗口加载结束
        ApplicationManager.getApplication().invokeLater(() -> {

            if (project.isDisposed()) {
                return;
            }
            if (toolWindow == null) {
                // 窗口未初始化
                return;
            }

            final ContentManager contentManager = toolWindow.getContentManager();

            // 监听当前选中的面板 进行阅读进度同步
            contentManager.addContentManagerListener(new ContentManagerListener() {
                @Override
                public void selectionChanged(@NotNull ContentManagerEvent event) {
                    Content selectedContent = event.getContent();

                    if (selectedContent != lastSelectedContent) {

                        // 只有选择的内容面板发生变化时才进行相关操作
                        lastSelectedContent = selectedContent;
                        if (selectedContent.getDisplayName().equals(DataCenter.TAB_CONTROL_TITLE_UNFOLD)) {
                            // 等待鼠标样式
                            setTheMouseStyle(Cursor.WAIT_CURSOR);

                            if (instance.chapters.size() == 0 || instance.nowChapterIndex < 0) {
                                ToastUtil.showToastMassage(project, "未知章节", ToastType.ERROR);
                                return;
                            }

                            // 切换了书本
                            if (BookMainWindow.isReadClick){
                                startReading();
                            }else {
                                // 获取新的章节位置
                                Chapter chapter = instance.chapters.get(instance.nowChapterIndex);

                                // 章节内容赋值
                                textContent.setText(instance.textContent);
                                // 设置下拉框的值
                                chapterList.setSelectedItem(chapter.getName());
                                // 回到顶部
                                textContent.setCaretPosition(1);
                            }
                            setTheMouseStyle(Cursor.DEFAULT_CURSOR);
                        }
                    }
                }
            });
            toolWindow.installWatcher(contentManager);
        });
    }

    // 开始阅读事件
    public void startReading() {
        // 清空下拉列表
        chapterList.removeAllItems();

        // 加载下拉列表
        for (Chapter chapter1 : instance.chapters) {
            chapterList.addItem(chapter1.getName());
        }
        // 加载阅读信息
        new LoadChapterInformation().execute();
    }

    /**
     * 异步GUI 线程加载 加载章节信息
     */
    final class LoadChapterInformation extends SwingWorker<Void, Chapter> {
        @Override
        protected Void doInBackground() {
            // 清空书本表格
            Chapter chapter = instance.chapters.get(instance.nowChapterIndex);

            // 重置重试次数
            BookTextServiceImpl.index = 2;

            // 内容
            textService.searchBookChapterData(chapter.getLink());

            if (instance.textContent == null) {
                ToastUtil.showToastMassage(project, "章节内容为空", ToastType.ERROR);
                return null;
            }

            //将当前进度信息加入chunks中
            publish(chapter);
            return null;
        }

        @Override
        protected void process(List<Chapter> chapters) {
            Chapter chapter = chapters.get(0);
            // 章节内容赋值
            textContent.setText(instance.textContent);
            // 设置下拉框的值
            chapterList.setSelectedItem(chapter.getName());
            // 回到顶部
            textContent.setCaretPosition(1);
        }

        @Override
        protected void done() {
            // 恢复默认鼠标样式
            setTheMouseStyle(Cursor.DEFAULT_CURSOR);
        }
    }

    /**
     * 加载页面鼠标样式
     *
     * @param type 鼠标样式 {@link Cursor}
     */
    public void setTheMouseStyle(int type) {
        Cursor cursor = new Cursor(type);
        fullScreenJpanel.setCursor(cursor);
    }

    /**
     * 初始化页面组件提示信息
     */
    public void setComponentTooltip() {
        // 上一章
        btnOn.setToolTipText(DataCenter.BTN_ON);
        // 下一章
        underOn.setToolTipText(DataCenter.UNDER_ON);
        // 放大
        fontSizeDown.setToolTipText(DataCenter.FONT_SIZE_DOWN);
        // 缩小
        fontSizeUp.setToolTipText(DataCenter.FONT_SIZE_UP);
        // 滚动间距
        scrollSpacing.setToolTipText(DataCenter.SCROLL_SPACING);

    }

    // 初始化阅读信息
    /*public void initReadText() {

        // 清空书本表格
        Chapter chapter = instance.chapters.get(instance.nowChapterIndex);

        // 重置重试次数
        BookTextServiceImpl.index = 2;

        // 内容
        textService.searchBookChapterData(chapter.getLink());
        // 章节内容赋值
        textContent.setText(instance.textContent);

        // 设置下拉框的值
        chapterList.setSelectedItem(chapter.getName());
        // 回到顶部
        textContent.setCaretPosition(1);

    }*/

    /**
     * 加载阅读进度
     */
    public void loadReadingProgress() {
        // 等待鼠标样式
        setTheMouseStyle(Cursor.WAIT_CURSOR);

        if (instance.chapters.size() == 0 || instance.nowChapterIndex < 0) {
            ToastUtil.showToastMassage(project, "未知章节", ToastType.ERROR);
            // 恢复默认鼠标样式
            setTheMouseStyle(Cursor.DEFAULT_CURSOR);
            return;
        }
        // 清空下拉列表
        chapterList.removeAllItems();

        // 加载下拉列表
        for (Chapter chapter : instance.chapters) {
            chapterList.addItem(chapter.getName());
        }

        Chapter chapter = instance.chapters.get(instance.nowChapterIndex);
        // 章节内容赋值
        textContent.setText(instance.textContent);
        // 设置下拉框的值
        chapterList.setSelectedItem(chapter.getName());
        // 回到顶部
        textContent.setCaretPosition(1);
        // 恢复默认鼠标样式
        setTheMouseStyle(Cursor.DEFAULT_CURSOR);

    }
}
