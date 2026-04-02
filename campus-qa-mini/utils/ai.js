function normalize(text) {
  return (text || "").toLowerCase().trim();
}

function splitKeywords(text) {
  return normalize(text)
    .replace(/[^\u4e00-\u9fa5a-z0-9]+/g, " ")
    .split(/\s+/)
    .filter((item) => item && item.length > 1);
}

function getQuestions() {
  const stored = wx.getStorageSync("campus-questions");
  if (stored && stored.length) {
    return stored;
  }
  const seed = require("../mock/data");
  return seed.questions || [];
}

function scoreQuestion(question, keywords, rawQuery) {
  const corpus = normalize([
    question.title,
    question.content,
    question.category,
    (question.tags || []).join(" "),
    question.aiAnswer ? question.aiAnswer.summary : ""
  ].join(" "));

  let score = 0;
  keywords.forEach((keyword) => {
    if (corpus.includes(keyword)) {
      score += keyword.length > 2 ? 3 : 2;
    }
  });

  if (rawQuery && normalize(question.title).includes(normalize(rawQuery))) {
    score += 4;
  }

  if (question.hot) {
    score += 1;
  }

  return score;
}

function buildOfficialRefs(ranked) {
  const refs = [];
  ranked.forEach((item) => {
    if (item.aiAnswer && item.aiAnswer.refs) {
      refs.push(...item.aiAnswer.refs);
    }
    if (item.aiAnswer && item.aiAnswer.summary) {
      refs.push(item.title);
    }
  });
  return Array.from(new Set(refs)).slice(0, 4);
}

function buildStudentExperiences(ranked) {
  const items = [];
  ranked.forEach((item) => {
    (item.answers || []).forEach((answer) => {
      if (answer && answer.content) {
        items.push(`${answer.authorName || "同学经验"}：${answer.content}`);
      }
    });
  });
  return Array.from(new Set(items)).slice(0, 3);
}

function generateAiReply(questionText) {
  const rawQuery = (questionText || "").trim();
  if (!rawQuery) {
    return {
      answer: "请先输入你想咨询的校园问题，比如图书馆、自习室、食堂、快递或宿舍服务。",
      refs: [],
      officialRefs: [],
      studentExperiences: [],
      matchedQuestions: []
    };
  }

  const keywords = splitKeywords(rawQuery);
  const questions = getQuestions();
  const ranked = questions
    .map((item) => ({
      ...item,
      _score: scoreQuestion(item, keywords, rawQuery)
    }))
    .filter((item) => item._score > 0)
    .sort((a, b) => b._score - a._score)
    .slice(0, 3);

  if (!ranked.length) {
    return {
      answer: "【直接结论】我暂时没有在当前校园知识中检索到直接相关的内容。\n【官方依据】当前知识不足以确认官方结论。\n【学生经验】暂未检索到可靠学生经验。",
      refs: [],
      officialRefs: [],
      studentExperiences: [],
      matchedQuestions: []
    };
  }

  const top = ranked[0];
  const officialRefs = buildOfficialRefs(ranked);
  const studentExperiences = buildStudentExperiences(ranked);
  const directAnswer = top.aiAnswer && top.aiAnswer.summary
    ? top.aiAnswer.summary
    : (top.answers && top.answers.length ? top.answers[0].content : `我为你匹配到了与“${top.title}”最相关的校园问题。`);

  const answer = [
    `【直接结论】${directAnswer}`,
    `【官方依据】${officialRefs.length ? `可参考：${officialRefs.join("、")}` : "当前知识不足以确认官方结论。"}`,
    `【学生经验】${studentExperiences.length ? studentExperiences.join("；") : "暂未检索到可靠学生经验。"}`
  ].join("\n");

  return {
    answer,
    refs: officialRefs,
    officialRefs,
    studentExperiences,
    matchedQuestions: ranked.map((item) => ({
      id: item.id,
      title: item.title,
      category: item.category
    }))
  };
}

module.exports = {
  generateAiReply
};