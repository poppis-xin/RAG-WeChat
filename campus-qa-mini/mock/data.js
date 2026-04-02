const categories = [
  "食堂餐饮",
  "图书馆",
  "快递服务",
  "教学办事",
  "宿舍生活",
  "校园常见问题"
];

const tags = [
  "营业时间",
  "位置导航",
  "借阅规则",
  "失物招领",
  "空调维修",
  "快递代取",
  "缴费办理",
  "自习室"
];

const questions = [
  {
    id: "q1001",
    title: "图书馆晚上几点闭馆？",
    content: "想问一下工作日图书馆一楼自习区最晚开放到几点，周末会不会提前闭馆？",
    category: "图书馆",
    tags: ["借阅规则", "自习室"],
    author: "20220001",
    authorName: "张同学",
    createdAt: "2026-03-20 18:20",
    views: 126,
    hot: true,
    aiAnswer: {
      summary: "根据图书馆公告，工作日一楼自习区通常开放至 22:30，周末为 22:00，节假日安排以馆内通知为准。",
      refs: ["图书馆开放时间通知", "2026 春季学期自习区安排"]
    },
    answers: [
      {
        id: "a9001",
        authorName: "图书馆管理员",
        content: "工作日一楼自习区到 22:30，周末到 22:00，如果遇到考试周可能会延长。",
        accepted: true,
        createdAt: "2026-03-20 18:40"
      }
    ]
  },
  {
    id: "q1002",
    title: "南苑食堂早餐推荐窗口有哪些？",
    content: "第一次去南苑食堂，想知道早餐哪几个窗口排队比较少，豆浆和包子在哪里买。",
    category: "食堂餐饮",
    tags: ["营业时间"],
    author: "20220002",
    authorName: "李同学",
    createdAt: "2026-03-21 08:10",
    views: 89,
    hot: true,
    aiAnswer: {
      summary: "南苑食堂 1 楼靠东侧窗口早餐高峰相对较快，豆浆和包子通常在 1 号和 3 号窗口供应。",
      refs: ["南苑食堂窗口分布", "后勤早餐供应安排"]
    },
    answers: [
      {
        id: "a9002",
        authorName: "王同学",
        content: "1 楼 1 号窗口最稳，包子和豆浆都有，7:30 以后人会多起来。",
        accepted: false,
        createdAt: "2026-03-21 08:25"
      }
    ]
  },
  {
    id: "q1003",
    title: "快递站周末能不能代取？",
    content: "周六不在学校，想让室友帮忙代取顺丰，校内快递站需要提供什么信息？",
    category: "快递服务",
    tags: ["快递代取"],
    author: "20220003",
    authorName: "陈同学",
    createdAt: "2026-03-22 14:00",
    views: 66,
    hot: false,
    aiAnswer: {
      summary: "多数校内快递站支持代取，需要出示取件码及收件人后四位手机号，部分站点需要登记代取人姓名。",
      refs: ["校园快递服务须知"]
    },
    answers: []
  }
];

const defaultUser = {
  studentId: "20225555",
  nickname: "演示用户",
  college: "计算机学院"
};

module.exports = {
  categories,
  tags,
  questions,
  defaultUser
};
