const api = require("../../utils/api");

Page({
  data: {
    keyword: "",
    categoryOptions: ["全部"],
    currentCategory: "全部",
    hotQuestions: [],
    questionList: [],
    userName: "",
    navMetrics: {
      statusBarHeight: 24,
      totalHeight: 68
    }
  },

  async onShow() {
    const app = getApp();
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 0
      });
    }
    const user = api.getUser();
    if (!user) {
      wx.reLaunch({
        url: "/pages/login/login"
      });
      return;
    }

    try {
      const meta = await api.getMeta();
      this.setData({
        userName: user.nickname,
        categoryOptions: ["全部"].concat(meta.categories || []),
        navMetrics: app.globalData.navMetrics
      });
      await this.loadQuestions();
    } catch (error) {
      wx.showToast({
        title: "加载列表失败",
        icon: "none"
      });
    }
  },

  async loadQuestions() {
    const { keyword, currentCategory } = this.data;
    const list = await api.getQuestions({ keyword, category: currentCategory });
    this.setData({
      questionList: list,
      hotQuestions: list.filter((item) => item.hot).slice(0, 3)
    });
  },

  async handleSearch(event) {
    this.setData({
      keyword: event.detail.value
    });
    await this.loadQuestions();
  },

  async selectCategory(event) {
    this.setData({
      currentCategory: event.currentTarget.dataset.category
    });
    await this.loadQuestions();
  },

  toDetail(event) {
    const id = event.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  toAi() {
    wx.navigateTo({
      url: "/pages/ai/ai"
    });
  }
});