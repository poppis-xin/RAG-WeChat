const api = require("../../utils/api");

Page({
  data: {
    user: null,
    stats: null,
    myQuestions: [],
    navMetrics: {
      statusBarHeight: 24,
      totalHeight: 68
    }
  },

  async loadProfileData() {
    const user = api.getUser();
    const all = await api.getQuestions();
    const stats = await api.getProfileStats();
    this.setData({
      user,
      stats,
      myQuestions: all.filter((item) => item.author === user.studentId),
      navMetrics: getApp().globalData.navMetrics
    });
  },

  async onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 2
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
      await this.loadProfileData();
    } catch (error) {
      wx.showToast({
        title: "个人信息加载失败",
        icon: "none"
      });
    }
  },

  logout() {
    wx.removeStorageSync("campus-user");
    getApp().globalData.user = null;
    wx.redirectTo({
      url: "/pages/login/login"
    });
  },

  toDetail(event) {
    wx.navigateTo({
      url: `/pages/detail/detail?id=${event.currentTarget.dataset.id}`
    });
  },

  deleteQuestion(event) {
    const { id } = event.currentTarget.dataset;
    const user = api.getUser();
    wx.showModal({
      title: "删除确认",
      content: "删除后将无法恢复，确定删除这条提问吗？",
      success: async (res) => {
        if (!res.confirm) {
          return;
        }
        try {
          const result = await api.deleteQuestion(id, user.studentId);
          if (!result.success) {
            wx.showToast({
              title: result.message,
              icon: "none"
            });
            return;
          }
          wx.showToast({
            title: "删除成功",
            icon: "success"
          });
          await this.loadProfileData();
        } catch (error) {
          wx.showToast({
            title: error.message || "删除失败",
            icon: "none"
          });
        }
      }
    });
  }
});