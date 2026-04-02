const api = require("../../utils/api");

function formatNow() {
  const date = new Date();
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  const hour = `${date.getHours()}`.padStart(2, "0");
  const minute = `${date.getMinutes()}`.padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

Page({
  data: {
    title: "",
    content: "",
    categoryIndex: -1,
    categoryOptions: [],
    tagOptions: [],
    selectedTags: [],
    navMetrics: {
      statusBarHeight: 24,
      totalHeight: 68
    }
  },

  async onShow() {
    const app = getApp();
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 1
      });
    }
    if (!api.getUser()) {
      wx.reLaunch({
        url: "/pages/login/login"
      });
      return;
    }

    try {
      const meta = await api.getMeta();
      this.setData({
        categoryOptions: meta.categories || app.globalData.categories,
        tagOptions: this.buildTagOptions(meta.tags || app.globalData.tags, this.data.selectedTags),
        navMetrics: app.globalData.navMetrics
      });
    } catch (error) {
      wx.showToast({
        title: "分类加载失败",
        icon: "none"
      });
    }
  },

  buildTagOptions(tags, selectedTags) {
    const selectedSet = new Set(selectedTags);
    return (tags || []).map((tag) => ({
      name: tag,
      selected: selectedSet.has(tag)
    }));
  },

  handleInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [field]: event.detail.value
    });
  },

  handleCategoryChange(event) {
    this.setData({
      categoryIndex: Number(event.currentTarget.dataset.index)
    });
  },

  toggleTag(event) {
    const tag = event.currentTarget.dataset.tag;
    const set = new Set(this.data.selectedTags);
    if (set.has(tag)) {
      set.delete(tag);
    } else {
      set.add(tag);
    }
    const selectedTags = Array.from(set);
    this.setData({
      selectedTags,
      tagOptions: this.buildTagOptions(this.data.tagOptions.map((item) => item.name), selectedTags)
    });
  },

  async submitQuestion() {
    const { title, content, categoryIndex, categoryOptions, selectedTags } = this.data;
    if (!title.trim() || !content.trim()) {
      wx.showToast({
        title: "标题和内容不能为空",
        icon: "none"
      });
      return;
    }

    if (categoryIndex < 0 || !categoryOptions[categoryIndex]) {
      wx.showToast({
        title: "请选择问题分类",
        icon: "none"
      });
      return;
    }

    const user = api.getUser();
    try {
      await api.createQuestion({
        title: title.trim(),
        content: content.trim(),
        category: categoryOptions[categoryIndex],
        tags: selectedTags,
        author: user.studentId,
        authorName: user.nickname,
        createdAt: formatNow()
      });

      this.setData({
        title: "",
        content: "",
        categoryIndex: -1,
        selectedTags: [],
        tagOptions: this.buildTagOptions(this.data.tagOptions.map((item) => item.name), [])
      });

      wx.showToast({
        title: "提问成功",
        icon: "success"
      });

      setTimeout(() => {
        wx.switchTab({
          url: "/pages/index/index"
        });
      }, 500);
    } catch (error) {
      wx.showToast({
        title: error.message || "提问失败",
        icon: "none"
      });
    }
  }
});