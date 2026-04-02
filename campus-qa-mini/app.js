const seed = require("./mock/data");

App({
  globalData: {
    user: null,
    categories: seed.categories,
    tags: seed.tags,
    useMock: false,
    apiBaseUrl: "http://localhost:8081/api",
    deviceInfo: null,
    isHarmonyOS: false,
    navMetrics: {
      statusBarHeight: 24,
      navBarHeight: 44,
      totalHeight: 68
    }
  },

  onLaunch() {
    this.initDeviceProfile();

    const user = wx.getStorageSync("campus-user") || seed.defaultUser;
    const questions = wx.getStorageSync("campus-questions");
    const profileStats = wx.getStorageSync("campus-profile-stats");

    this.globalData.user = user;

    if (!questions || !questions.length) {
      wx.setStorageSync("campus-questions", seed.questions);
    }

    if (!profileStats) {
      wx.setStorageSync("campus-profile-stats", {
        totalQuestions: seed.questions.length,
        totalAnswers: seed.questions.reduce((sum, item) => sum + item.answers.length, 0),
        acceptedAnswers: seed.questions.filter((item) => item.answers.some((answer) => answer.accepted)).length
      });
    }
  },

  initDeviceProfile() {
    try {
      const deviceInfo = wx.getDeviceInfo ? wx.getDeviceInfo() : {};
      const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : {};
      const platform = deviceInfo.platform || "";
      const system = deviceInfo.system || "";
      const isHarmonyOS = platform === "ohos" || (platform === "devtools" && system === "HarmonyOS");
      const statusBarHeight = windowInfo.statusBarHeight || 24;
      const navBarHeight = 44;

      this.globalData.deviceInfo = deviceInfo;
      this.globalData.isHarmonyOS = isHarmonyOS;
      this.globalData.navMetrics = {
        statusBarHeight,
        navBarHeight,
        totalHeight: statusBarHeight + navBarHeight
      };
    } catch (error) {
      this.globalData.deviceInfo = null;
      this.globalData.isHarmonyOS = false;
      this.globalData.navMetrics = {
        statusBarHeight: 24,
        navBarHeight: 44,
        totalHeight: 68
      };
    }
  }
});