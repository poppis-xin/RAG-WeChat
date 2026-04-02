function getAppSafe() {
  try {
    return getApp();
  } catch (error) {
    return null;
  }
}

function getSeed() {
  return require("../mock/data");
}

function isMockEnabled() {
  const app = getAppSafe();
  return !!(app && app.globalData && app.globalData.useMock);
}

function getQuestionsLocal() {
  return wx.getStorageSync("campus-questions") || [];
}

function saveQuestionsLocal(questions) {
  wx.setStorageSync("campus-questions", questions);
  return questions;
}

function request({ url, method = "GET", data }) {
  const app = getAppSafe();
  const baseUrl = app && app.globalData ? app.globalData.apiBaseUrl : "";
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}${url}`,
      method,
      data,
      timeout: 20000,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
          return;
        }
        const message = res.data && res.data.message ? res.data.message : `HTTP ${res.statusCode}`;
        const error = new Error(message);
        error.response = res.data;
        reject(error);
      },
      fail(error) {
        reject(error);
      }
    });
  });
}

function requestWithFallback(options, fallback) {
  if (isMockEnabled()) {
    return Promise.resolve().then(fallback);
  }
  return request(options).catch((error) => {
    console.warn("request fallback:", options.url, error);
    return fallback();
  });
}

function computeStats(questions) {
  return {
    totalQuestions: questions.length,
    totalAnswers: questions.reduce((sum, item) => sum + ((item.answers || []).length), 0),
    acceptedAnswers: questions.filter((item) => (item.answers || []).some((answer) => answer.accepted)).length
  };
}

function saveLocalStats(questions) {
  const stats = computeStats(questions);
  wx.setStorageSync("campus-profile-stats", stats);
  return stats;
}

function queryLocalQuestions(keyword, category) {
  const normalizedKeyword = (keyword || "").trim().toLowerCase();
  return getQuestionsLocal().filter((item) => {
    const matchKeyword = !normalizedKeyword || [item.title, item.content, item.category, (item.tags || []).join(" ")]
      .join(" ")
      .toLowerCase()
      .includes(normalizedKeyword);
    const matchCategory = !category || category === "全部" || item.category === category;
    return matchKeyword && matchCategory;
  });
}

function getQuestions(options = {}) {
  const { keyword = "", category = "" } = options;
  return requestWithFallback({
    url: "/questions",
    method: "GET",
    data: { keyword, category }
  }, () => Promise.resolve(queryLocalQuestions(keyword, category)));
}

function getQuestionById(id) {
  return requestWithFallback({
    url: `/questions/${id}`,
    method: "GET"
  }, () => {
    const questions = getQuestionsLocal();
    const updated = questions.map((item) => {
      if (item.id === id) {
        return {
          ...item,
          views: (item.views || 0) + 1
        };
      }
      return item;
    });
    saveQuestionsLocal(updated);
    return Promise.resolve(updated.find((item) => item.id === id));
  });
}

function createQuestion(payload) {
  return requestWithFallback({
    url: "/questions",
    method: "POST",
    data: payload
  }, () => {
    const questions = getQuestionsLocal();
    const newQuestion = {
      id: `q${Date.now()}`,
      title: payload.title,
      content: payload.content,
      category: payload.category,
      tags: payload.tags || [],
      author: payload.author,
      authorName: payload.authorName,
      createdAt: payload.createdAt,
      views: 0,
      hot: false,
      aiAnswer: {
        summary: "当前为基础版本演示，小程序已保留 AI 回答位。后续接入知识检索与大模型后，这里会展示基于校园知识库生成的答案。",
        refs: ["待接入知识库", "待接入大模型接口"]
      },
      answers: []
    };
    const updated = [newQuestion].concat(questions);
    saveQuestionsLocal(updated);
    saveLocalStats(updated);
    return Promise.resolve(newQuestion);
  });
}

function deleteQuestion(id, author) {
  return requestWithFallback({
    url: `/questions/${id}`,
    method: "DELETE",
    data: { author }
  }, () => {
    const questions = getQuestionsLocal();
    const target = questions.find((item) => item.id === id);
    if (!target) {
      return Promise.resolve({ success: false, message: "问题不存在" });
    }

    if (author && target.author !== author) {
      return Promise.resolve({ success: false, message: "只能删除自己的提问" });
    }

    const updated = questions.filter((item) => item.id !== id);
    saveQuestionsLocal(updated);
    saveLocalStats(updated);
    return Promise.resolve({ success: true });
  });
}

function getMeta() {
  return requestWithFallback({
    url: "/meta",
    method: "GET"
  }, () => {
    const seed = getSeed();
    return Promise.resolve({
      categories: seed.categories,
      tags: seed.tags
    });
  }).then((meta) => {
    const app = getAppSafe();
    if (app && app.globalData) {
      app.globalData.categories = meta.categories || [];
      app.globalData.tags = meta.tags || [];
    }
    return meta;
  });
}

function getProfileStats() {
  return getQuestions().then((questions) => {
    const stats = computeStats(questions || []);
    wx.setStorageSync("campus-profile-stats", stats);
    return stats;
  });
}

function saveUser(user) {
  wx.setStorageSync("campus-user", user);
  return user;
}

function getUser() {
  return wx.getStorageSync("campus-user");
}

function askAiQuestion(question) {
  return request({
    url: "/ai/answer",
    method: "POST",
    data: {
      question
    }
  });
}

function streamAiQuestion(question, handlers = {}) {
  const app = getAppSafe();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let requestTask = null;

  requestTask = wx.request({
    url: `${app.globalData.apiBaseUrl}/ai/stream`,
    method: "POST",
    timeout: 120000,
    enableChunked: true,
    responseType: "arraybuffer",
    header: {
      "Content-Type": "application/json"
    },
    data: {
      question
    },
    success() {
      if (handlers.onComplete) {
        handlers.onComplete();
      }
    },
    fail(error) {
      if (handlers.onError) {
        handlers.onError(error);
      }
    }
  });

  if (requestTask && typeof requestTask.onChunkReceived === "function") {
    requestTask.onChunkReceived((res) => {
      buffer += decoder.decode(res.data, { stream: true });
      const events = buffer.split("\n\n");
      buffer = events.pop() || "";

      events.forEach((item) => {
        const lines = item
          .split("\n")
          .map((line) => line.trim())
          .filter(Boolean);

        lines.forEach((line) => {
          if (!line.startsWith("data:")) {
            return;
          }
          const payload = line.slice(5).trim();
          if (!payload) {
            return;
          }
          try {
            const parsed = JSON.parse(payload);
            if (handlers.onMessage) {
              handlers.onMessage(parsed);
            }
          } catch (error) {
            // ignore incomplete chunks
          }
        });
      });
    });
  }

  return requestTask;
}

module.exports = {
  getQuestions,
  getQuestionById,
  createQuestion,
  deleteQuestion,
  getProfileStats,
  getMeta,
  saveUser,
  getUser,
  askAiQuestion,
  streamAiQuestion
};