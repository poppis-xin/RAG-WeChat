Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: "/pages/index/index",
        text: "首页",
        icon: "home"
      },
      {
        pagePath: "/pages/ask/ask",
        text: "提问",
        icon: "plus"
      },
      {
        pagePath: "/pages/profile/profile",
        text: "我的",
        icon: "user"
      }
    ]
  },

  methods: {
    switchTab(event) {
      const { index, path } = event.currentTarget.dataset;
      if (this.data.selected === index) {
        return;
      }
      this.setData({ selected: index });
      wx.switchTab({
        url: path
      });
    }
  }
});
