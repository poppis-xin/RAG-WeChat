const api = require("../../utils/api");

Page({
  data: {
    studentId: "",
    nickname: "",
    college: "",
    navMetrics: {
      statusBarHeight: 24,
      totalHeight: 68
    }
  },

  onLoad() {
    this.setData({
      navMetrics: getApp().globalData.navMetrics
    });
    const user = api.getUser();
    if (user && user.studentId) {
      wx.switchTab({
        url: "/pages/index/index"
      });
    }
  },

  handleInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [field]: event.detail.value
    });
  },

  submitLogin() {
    const { studentId, nickname, college } = this.data;
    if (!studentId || !nickname || !college) {
      wx.showToast({
        title: "请完整填写信息",
        icon: "none"
      });
      return;
    }

    const app = getApp();
    const user = { studentId, nickname, college };
    app.globalData.user = api.saveUser(user);

    wx.showToast({
      title: "登录成功",
      icon: "success"
    });

    setTimeout(() => {
      wx.switchTab({
        url: "/pages/index/index"
      });
    }, 400);
  }
});
