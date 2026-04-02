const api = require("../../utils/api");

Page({
  data: {
    question: null,
    navMetrics: {
      statusBarHeight: 24,
      totalHeight: 68
    }
  },

  async onLoad(options) {
    this.setData({
      navMetrics: getApp().globalData.navMetrics
    });
    try {
      const question = await api.getQuestionById(options.id);
      if (!question) {
        wx.showToast({
          title: "问题不存在",
          icon: "none"
        });
        return;
      }

      this.setData({
        question
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  },

  goBack() {
    wx.navigateBack({
      delta: 1,
      fail() {
        wx.switchTab({
          url: "/pages/index/index"
        });
      }
    });
  }
});