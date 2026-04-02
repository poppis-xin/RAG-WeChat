const ai = require("../../utils/ai");
const api = require("../../utils/api");

function createMessage(role, content, extra = {}) {
  return {
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    role,
    content,
    refs: extra.refs || [],
    officialRefs: extra.officialRefs || [],
    studentExperiences: extra.studentExperiences || [],
    matchedQuestions: extra.matchedQuestions || []
  };
}

Page({
  data: {
    navMetrics: {
      statusBarHeight: 24,
      totalHeight: 68
    },
    inputValue: "",
    quickQuestions: [
      "图书馆周末几点闭馆？",
      "快递站周末能代取吗？",
      "食堂早餐哪个窗口人少？"
    ],
    messages: [],
    streaming: false
  },

  onLoad() {
    const app = getApp();
    this.setData({
      navMetrics: app.globalData.navMetrics,
      messages: [
        createMessage(
          "assistant",
          "你好，我是校园 AI 助手。你可以直接问我图书馆、食堂、快递、宿舍或教学办事相关问题，我会尽量把回答拆成“直接结论、官方依据、学生经验”三部分。"
        )
      ]
    });
  },

  handleInput(event) {
    this.setData({
      inputValue: event.detail.value
    });
  },

  useQuickQuestion(event) {
    const question = event.currentTarget.dataset.question;
    this.setData({
      inputValue: question
    });
    this.sendMessage();
  },

  appendAssistantChunk(content) {
    const messages = this.data.messages.slice();
    const lastIndex = messages.length - 1;
    if (lastIndex < 0 || messages[lastIndex].role !== "assistant") {
      messages.push(createMessage("assistant", content));
    } else {
      messages[lastIndex] = {
        ...messages[lastIndex],
        content: messages[lastIndex].content + content
      };
    }
    this.setData({ messages });
  },

  updateAssistantMeta(payload) {
    const messages = this.data.messages.slice();
    const lastIndex = messages.length - 1;
    if (lastIndex < 0 || messages[lastIndex].role !== "assistant") {
      messages.push(createMessage("assistant", "", {
        refs: payload.refs || payload.officialRefs || [],
        officialRefs: payload.officialRefs || payload.refs || [],
        studentExperiences: payload.studentExperiences || [],
        matchedQuestions: payload.matchedQuestions || []
      }));
    } else {
      messages[lastIndex] = {
        ...messages[lastIndex],
        refs: payload.refs || payload.officialRefs || messages[lastIndex].refs,
        officialRefs: payload.officialRefs || payload.refs || messages[lastIndex].officialRefs,
        studentExperiences: payload.studentExperiences || messages[lastIndex].studentExperiences,
        matchedQuestions: payload.matchedQuestions || messages[lastIndex].matchedQuestions
      };
    }
    this.setData({ messages });
  },

  sendMessage() {
    const content = (this.data.inputValue || "").trim();
    if (!content || this.data.streaming) {
      wx.showToast({
        title: content ? "正在生成中" : "请输入问题",
        icon: "none"
      });
      return;
    }

    const userMessage = createMessage("user", content);
    const assistantPlaceholder = createMessage("assistant", "");
    const baseMessages = this.data.messages.concat([userMessage, assistantPlaceholder]);
    this.setData({
      inputValue: "",
      messages: baseMessages,
      streaming: true
    });

    api.streamAiQuestion(content, {
      onMessage: (payload) => {
        if (payload.type === "meta") {
          this.updateAssistantMeta(payload);
          return;
        }
        if (payload.type === "delta") {
          this.appendAssistantChunk(payload.content || "");
          return;
        }
        if (payload.type === "done") {
          this.setData({
            streaming: false
          });
        }
      },
      onError: () => {
        const result = ai.generateAiReply(content);
        this.setData({
          streaming: false
        });
        this.updateAssistantMeta(result);
        this.appendAssistantChunk(result.answer);
        wx.showToast({
          title: "流式接口不可用，已切换演示回答",
          icon: "none"
        });
      },
      onComplete: () => {
        this.setData({
          streaming: false
        });
      }
    });
  },

  openQuestion(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
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