# -*- coding: utf-8 -*-
"""
毕业设计第六套 · 模拟种子数据生成器
产出：
  data/01_schema.sql          建表（严格按 01-数据库设计.md）
  data/02_college_major_course.sql
  data/03_knowledge_point.sql （MySQL 无 KP 表，仅作编号说明 + 引用 Neo4j）
  data/04_resource.sql        （含 review_record / neo4j_sync_log）
  data/05_user.sql            （管理员 + 学生）
  data/06_behavior.sql        （收藏/下载/评分）
  data/07_social.sql          （评论/需求/问答/公告）
  data/neo4j_init.cypher
  data/README.md

编号规则：
  College.id       1..5           College.code  CS/INFO/SE/MATH/FL
  Major.id         1..10
  Course.id        101..141 (41门)
  KnowledgePoint.id 1001..1060 (60个)
  learning_resource.id  1..200
  resource_no = 'RES2026' + 4位序号  -> RES20260001..RES20260200
  Neo4j Resource.resource_id = resource_no （严格一致）
  student_user.id  1..30   student_no 2023001..2023030
  admin.id         1..2
"""
import random, bcrypt, os, datetime

random.seed(20260920)
OUT = os.path.dirname(os.path.abspath(__file__))

# ---------- 基础工具 ----------
def sq(v):
    """MySQL 字符串/值转义"""
    if v is None:
        return "NULL"
    if isinstance(v, str):
        return "'" + v.replace("\\", "\\\\").replace("'", "''") + "'"
    return str(v)

def cy(v):
    """Cypher 字符串转义（单引号包裹，' 转义为 \\'）"""
    if v is None:
        return "NULL"
    if isinstance(v, str):
        return "'" + v.replace("\\", "\\\\").replace("'", "\\'") + "'"
    return str(v)

def rand_dt(start_s, end_s):
    """在 [start, end] 之间返回 YYYY-MM-DD HH:MM:SS"""
    fmt = "%Y-%m-%d %H:%M:%S"
    a = datetime.datetime.strptime(start_s, fmt)
    b = datetime.datetime.strptime(end_s, fmt)
    delta = (b - a).total_seconds()
    t = a + datetime.timedelta(seconds=random.randint(0, int(delta)))
    return t.strftime(fmt)

# ================= 1. 学院 =================
colleges = [
    (1, "计算机学院", "CS", "计算机科学与技术学院，涵盖计算机、人工智能等方向"),
    (2, "信息工程学院", "INFO", "信息与电子工程，含电子信息、通信、自动化"),
    (3, "软件学院", "SE", "软件工程、网络工程，侧重工程实践"),
    (4, "数学学院", "MATH", "数学与应用数学、信息与计算科学"),
    (5, "外国语学院", "FL", "外国语言文化，以英语为主"),
]

# ================= 2. 专业 =================
majors = [
    (1, 1, "计算机科学与技术", "CS01", "计算机系统与软件基础专业"),
    (2, 1, "人工智能", "CS02", "机器学习与智能系统方向"),
    (3, 2, "电子信息工程", "EE01", "电子电路与信息处理"),
    (4, 2, "通信工程", "EE02", "通信系统与网络"),
    (5, 2, "自动化", "EE03", "控制理论与系统工程"),
    (6, 3, "软件工程", "SE01", "软件开发与工程方法"),
    (7, 3, "网络工程", "SE02", "网络系统与云计算"),
    (8, 4, "数学与应用数学", "MA01", "基础数学与应用"),
    (9, 4, "信息与计算科学", "MA02", "计算数学与算法"),
    (10, 5, "英语", "FL01", "英语语言文学"),
]

# ================= 3. 课程 =================
# (id, college_id, major_id, name, code, teacher, credit, semester)
courses = [
    (101, 1, 1, "数据结构", "CS101", "张建国", 4.0, "2025-春"),
    (102, 1, 1, "计算机组成原理", "CS102", "李伟", 3.5, "2025-秋"),
    (103, 1, 1, "操作系统", "CS103", "王芳", 4.0, "2025-春"),
    (104, 1, 1, "计算机网络", "CS104", "刘洋", 3.5, "2025-秋"),
    (105, 1, 1, "编译原理", "CS105", "陈静", 3.0, "2025-春"),
    (106, 1, 1, "算法设计与分析", "CS106", "赵磊", 3.5, "2025-秋"),
    (107, 1, 2, "人工智能导论", "CS107", "孙明", 3.0, "2025-春"),
    (108, 1, 2, "机器学习", "CS108", "周婷", 3.5, "2025-秋"),
    (109, 1, 1, "离散数学", "CS109", "吴军", 3.0, "2025-春"),
    (110, 1, 1, "数据库原理", "CS110", "郑华", 3.5, "2025-秋"),
    (201, 2, 3, "电路分析基础", "EE201", "黄强", 3.5, "2025-春"),
    (202, 2, 3, "信号与系统", "EE202", "徐丽", 3.5, "2025-秋"),
    (203, 2, 3, "数字电子技术", "EE203", "马涛", 3.0, "2025-春"),
    (204, 2, 4, "通信原理", "EE204", "高远", 3.5, "2025-秋"),
    (205, 2, 5, "嵌入式系统", "EE205", "林峰", 3.0, "2025-春"),
    (206, 2, 5, "自动控制原理", "EE206", "何勇", 3.5, "2025-秋"),
    (207, 2, 3, "微机原理与接口", "EE207", "罗刚", 3.0, "2025-春"),
    (208, 2, 4, "物联网技术", "EE208", "宋佳", 2.5, "2025-秋"),
    (301, 3, 6, "软件工程", "SE301", "钱进", 3.5, "2025-春"),
    (302, 3, 6, "Java程序设计", "SE302", "冯媛", 3.5, "2025-秋"),
    (303, 3, 6, "Web开发技术", "SE303", "邓超", 3.0, "2025-春"),
    (304, 3, 6, "软件测试", "SE304", "韩雪", 2.5, "2025-秋"),
    (305, 3, 6, "软件体系结构", "SE305", "曹斌", 2.5, "2025-春"),
    (306, 3, 7, "移动应用开发", "SE306", "彭飞", 3.0, "2025-秋"),
    (307, 3, 6, "Python程序设计", "SE307", "袁琳", 3.0, "2025-春"),
    (308, 3, 7, "云计算与大数据", "SE308", "董磊", 2.5, "2025-春"),
    (309, 3, 6, "人机交互", "SE309", "潘虹", 2.0, "2025-秋"),
    (310, 3, 6, "软件项目管理", "SE310", "范伟", 2.0, "2025-春"),
    (401, 4, 8, "高等数学", "MA401", "苏梅", 5.0, "2025-春"),
    (402, 4, 8, "线性代数", "MA402", "石磊", 3.5, "2025-秋"),
    (403, 4, 8, "概率论与数理统计", "MA403", "崔红", 3.5, "2025-春"),
    (404, 4, 8, "数学分析", "MA404", "阎良", 5.0, "2025-秋"),
    (405, 4, 9, "复变函数", "MA405", "段鹏", 3.0, "2025-春"),
    (406, 4, 9, "数值分析", "MA406", "雷蕾", 3.0, "2025-秋"),
    (407, 4, 8, "运筹学", "MA407", "汪洋", 3.0, "2025-春"),
    (501, 5, 10, "大学英语", "FL501", "叶静", 4.0, "2025-春"),
    (502, 5, 10, "英语口语", "FL502", "葛亮", 2.0, "2025-秋"),
    (503, 5, 10, "英语写作", "FL503", "鲍蕾", 2.0, "2025-春"),
    (504, 5, 10, "翻译理论与实践", "FL504", "田甜", 3.0, "2025-秋"),
    (505, 5, 10, "英美文学", "FL505", "尹航", 2.5, "2025-春"),
    (506, 5, 10, "学术英语", "FL506", "易凡", 2.0, "2025-秋"),
]
course_by_id = {c[0]: c for c in courses}

# 课程先修关系 (from_course_id -> to_course_id 即 先修)
course_prereq = [
    (106, 101), (103, 102), (108, 107), (108, 403), (110, 101), (104, 102),
    (202, 201), (204, 202), (206, 201), (302, 101), (303, 302), (305, 301),
    (306, 302), (402, 401), (403, 401), (404, 401), (406, 402), (407, 402),
    (502, 501), (503, 501), (504, 501), (505, 501),
]

# ================= 4. 知识点 =================
# (course_id, name, description)  -> 顺序分配 id 1001..
kp_defs = [
    (101, "线性表", "顺序表与链表的基本操作与实现"),
    (101, "栈与队列", "栈和队列的定义、操作与应用场景"),
    (101, "链表", "单链表、双链表与循环链表的实现"),
    (101, "树与二叉树", "二叉树性质、遍历与存储结构"),
    (101, "图", "图的存储、遍历与最短路径算法"),
    (102, "数据表示", "定点数与浮点数的机器表示"),
    (102, "CPU结构", "运算器与控制器的基本组成"),
    (103, "进程管理", "进程概念、状态与进程控制块"),
    (103, "内存管理", "分区、分页与分段存储管理"),
    (103, "文件系统", "文件逻辑结构与目录管理"),
    (103, "进程同步", "临界区、信号量与管程机制"),
    (103, "死锁", "死锁必要条件与处理策略"),
    (104, "网络分层模型", "OSI 与 TCP/IP 分层体系结构"),
    (104, "TCP与UDP", "传输层协议原理与可靠传输"),
    (105, "词法分析", "正则文法与有穷自动机"),
    (105, "语法分析", "上下文无关文法与语法树构造"),
    (106, "递归与分治", "递归思想与分治算法设计"),
    (106, "动态规划", "最优子结构与状态转移方程"),
    (106, "贪心算法", "贪心选择性质与活动安排"),
    (106, "最短路径", "Dijkstra 与 Floyd 算法"),
    (107, "搜索策略", "盲目搜索与启发式搜索"),
    (107, "知识表示", "产生式、框架与语义网络"),
    (108, "线性回归", "最小二乘与梯度下降"),
    (108, "决策树", "信息增益与 ID3/C4.5"),
    (108, "神经网络", "感知机与反向传播"),
    (109, "图论", "图的基本性质与欧拉/哈密顿问题"),
    (109, "命题逻辑", "命题、联结词与推理规则"),
    (110, "关系模型", "关系代数与关系完整性"),
    (110, "SQL语言", "数据定义、查询与更新语句"),
    (201, "基尔霍夫定律", "电路节点与回路电流定律"),
    (202, "傅里叶变换", "连续与离散时间傅里叶变换"),
    (203, "逻辑门电路", "与或非门与组合逻辑设计"),
    (204, "调制与解调", "模拟与数字调制基本原理"),
    (205, "微控制器架构", "MCU 内核与寄存器组织"),
    (206, "传递函数", "线性定常系统的频域描述"),
    (207, "汇编指令", "指令格式与寻址方式"),
    (208, "传感器网络", "无线传感网的拓扑与协议"),
    (301, "需求分析", "需求获取、建模与规格说明"),
    (302, "面向对象基础", "类、对象、继承与多态"),
    (303, "前端框架", "组件化开发与状态管理"),
    (304, "测试用例设计", "等价类划分与边界值分析"),
    (305, "设计模式", "创建型、结构型与行为型模式"),
    (306, "移动UI设计", "移动端交互与界面布局"),
    (307, "Python语法", "数据类型、函数与面向对象"),
    (308, "分布式存储", "一致性与副本管理机制"),
    (309, "交互设计原则", "可用性设计与反馈机制"),
    (310, "敏捷开发", "迭代、Scrum 与看板方法"),
    (401, "极限与连续", "极限定义与函数连续性"),
    (402, "矩阵运算", "矩阵乘法、秩与初等变换"),
    (403, "随机变量", "分布函数与数字特征"),
    (404, "实数完备性", "确界原理与闭区间套定理"),
    (405, "解析函数", "复变函数的可导与解析性"),
    (406, "数值逼近", "插值、拟合与数值积分"),
    (407, "线性规划", "单纯形法与对偶问题"),
    (501, "词汇与语法", "核心词汇与基础语法体系"),
    (502, "情景对话", "日常与学术场景口语表达"),
    (503, "段落写作", "段落结构与衔接手段"),
    (504, "英汉互译", "翻译策略与长难句处理"),
    (505, "文学赏析", "英美文学流派与作品解读"),
    (506, "学术写作规范", "论文结构、引文与学术伦理"),
]
# 分配 id
knowledge_points = []  # dict: id,name,desc,course_id
kp_id_counter = 1001
kp_by_course = {}      # course_id -> [kp_id,...]
for (cid, name, desc) in kp_defs:
    kid = kp_id_counter
    knowledge_points.append({"id": kid, "name": name, "desc": desc, "course_id": cid})
    kp_by_course.setdefault(cid, []).append(kid)
    kp_id_counter += 1
kp_by_id = {k["id"]: k for k in knowledge_points}

# 知识点关系 (from, to, type)  type in PREREQUISITE / RELATED_TO / CONTAINS
# 用 (from_kp_name, to_kp_name, type) 解析为 id
kp_name_to_id = {k["name"]: k["id"] for k in knowledge_points}
kp_rel_defs = [
    ("树与二叉树", "栈与队列", "PREREQUISITE"),
    ("图", "树与二叉树", "PREREQUISITE"),
    ("动态规划", "递归与分治", "PREREQUISITE"),
    ("贪心算法", "递归与分治", "PREREQUISITE"),
    ("最短路径", "图", "PREREQUISITE"),
    ("进程同步", "进程管理", "PREREQUISITE"),
    ("死锁", "进程管理", "PREREQUISITE"),
    ("文件系统", "内存管理", "PREREQUISITE"),
    ("SQL语言", "关系模型", "PREREQUISITE"),
    ("决策树", "线性回归", "PREREQUISITE"),
    ("神经网络", "线性回归", "PREREQUISITE"),
    ("傅里叶变换", "基尔霍夫定律", "PREREQUISITE"),
    ("动态规划", "贪心算法", "RELATED_TO"),
    ("图论", "图", "RELATED_TO"),
    ("矩阵运算", "线性回归", "RELATED_TO"),
    ("随机变量", "神经网络", "RELATED_TO"),
    ("递归与分治", "树与二叉树", "RELATED_TO"),
    ("面向对象基础", "线性表", "RELATED_TO"),
    ("设计模式", "需求分析", "RELATED_TO"),
    ("前端框架", "面向对象基础", "RELATED_TO"),
    ("词汇与语法", "段落写作", "RELATED_TO"),
    ("英汉互译", "段落写作", "RELATED_TO"),
    # CONTAINS 知识层级（少量）
    ("线性表", "链表", "CONTAINS"),
    ("进程管理", "进程同步", "CONTAINS"),
    ("关系模型", "SQL语言", "CONTAINS"),
]
kp_rels = []
for (a, b, t) in kp_rel_defs:
    if a in kp_name_to_id and b in kp_name_to_id:
        kp_rels.append((kp_name_to_id[a], kp_name_to_id[b], t))

# ================= 5. 资源分类（二级） =================
categories = [
    (1, "课件资料", 0),
    (2, "PPT课件", 1),
    (3, "讲义教案", 1),
    (4, "视频教程", 0),
    (5, "录播视频", 4),
    (6, "实操演示", 4),
    (7, "文档笔记", 0),
    (8, "学习笔记", 7),
    (9, "速查手册", 7),
    (10, "习题试卷", 0),
    (11, "课后习题", 10),
    (12, "历年试卷", 10),
    (13, "论文文献", 0),
    (14, "期刊论文", 13),
    (15, "学位论文", 13),
    (16, "其他资料", 0),
]
# type -> 候选 category_id
type_cat = {
    1: [7, 8, 9],
    2: [4, 5, 6],
    3: [1, 2, 3],
    4: [13, 14, 15, 16],
}

# ================= 6. 资源（200） =================
# 标题模板
templates = {
    3: ["{c}《{k}》课堂精讲PPT", "{c}{k}第{ch}章课件", "{k}知识点串讲课件", "{c}复习课件（{k}专题）"],
    2: ["{k}原理动画演示", "{c}{k}实战视频教程", "{k}速成教学视频", "{k}重难点精讲视频"],
    1: ["{k}学习笔记（{c}）", "{k}知识点总结与易错点", "{c}{k}考点梳理", "{k}思维导图笔记"],
    4: ["{k}研究综述", "{c}学习路线与{k}资源包", "基于{k}的课程设计案例", "{k}拓展阅读资料"],
}
type_label = {1: "文档", 2: "视频", 3: "课件", 4: "其他"}

# 作者池（按学院）
teacher_by_college = {
    1: ["张建国", "李伟", "王芳", "刘洋", "陈静", "赵磊", "孙明", "周婷", "吴军", "郑华"],
    2: ["黄强", "徐丽", "马涛", "高远", "林峰", "何勇", "罗刚", "宋佳"],
    3: ["钱进", "冯媛", "邓超", "韩雪", "曹斌", "彭飞", "袁琳", "董磊", "潘虹", "范伟"],
    4: ["苏梅", "石磊", "崔红", "阎良", "段鹏", "雷蕾", "汪洋"],
    5: ["叶静", "葛亮", "鲍蕾", "田甜", "尹航", "易凡"],
}
group_author = ["学习资料共享组", "考研笔记互助组", "学霸笔记工作室"]

resources = []  # dict
# 先决定每门课程的资源数量：核心课程多，其余少，合计 200
course_resource_count = {}
# 给核心课程分配更多
core_courses = [101, 103, 106, 110, 108, 401, 501, 302, 301]
ci = 0
# 初始每门 3 个，核心多给
for c in course_by_id:
    course_resource_count[c] = 4 if c in core_courses else 3
# 调整使其总和 200
total_pre = sum(course_resource_count.values())
need = 200 - total_pre
# 把 need 额外分给核心课程
for c in core_courses:
    if need <= 0:
        break
    add = min(need, 6)
    course_resource_count[c] += add
    need -= add
# 若仍不够，平摊
idx = 0
while need > 0:
    c = list(course_by_id.keys())[idx % len(course_by_id)]
    course_resource_count[c] += 1
    need -= 1
    idx += 1

rid = 1
used_titles = set()
for c in course_by_id:
    cid, col, maj, cname, code, teacher, credit, sem = course_by_id[c]
    kps = kp_by_course.get(c, [])
    n = course_resource_count[c]
    for j in range(n):
        if rid > 200:
            break
        # type 分布：课件/视频/文档/其他
        r = random.random()
        if r < 0.35:
            t = 3
        elif r < 0.6:
            t = 2
        elif r < 0.85:
            t = 1
        else:
            t = 4
        kp = random.choice(kps) if kps else None
        kp_name = kp_by_id[kp]["name"] if kp else cname
        tmpl = random.choice(templates[t])
        title = tmpl.format(c=cname, k=cname if not kps else kp_name, ch=random.randint(1, 12))
        # 去重
        base_title = title
        dup = 1
        while title in used_titles:
            title = f"{base_title}（{type_label[t]}版{dup}）"
            dup += 1
        used_titles.add(title)
        cat = random.choice(type_cat[t])
        # 作者
        if random.random() < 0.7:
            author = teacher
        else:
            author = random.choice(group_author)
        # 上传者
        if random.random() < 0.7:
            uploader_type = 2
            uploader_id = random.choice([1, 2])
        else:
            uploader_type = 1
            uploader_id = random.randint(1, 30)
        # 状态分布：默认通过，少量待审/驳回
        if rid in (197, 198, 199, 200):
            status = 2
        elif rid in (193, 194, 195, 196):
            status = 0
        else:
            status = 1
        res_no = "RES2026" + f"{rid:04d}"
        ctime = rand_dt("2025-09-01 08:00:00", "2026-06-10 22:00:00")
        resources.append({
            "id": rid,
            "resource_no": res_no,
            "title": title,
            "category_id": cat,
            "college_id": col,
            "major_id": maj,
            "course_id": c,
            "primary_kp_id": kp,
            "type": t,
            "file_url": f"https://oss.campus.edu/res/{res_no}.{'pdf' if t in (1,4) else ('mp4' if t==2 else 'pptx')}",
            "cover_url": f"https://oss.campus.edu/cover/{res_no}.jpg",
            "description": f"《{cname}》{kp_name}相关{type_label[t]}，适合课程复习与考前突击。",
            "author": author,
            "uploader_id": uploader_id,
            "uploader_type": uploader_type,
            "view_count": random.randint(120, 4200),
            "download_count_base": random.randint(20, 680),
            "favorite_count": 0,
            "avg_rating": 0.0,
            "rating_count": 0,
            "heat": 0.0,
            "status": status,
            "audit_time": ctime if status == 1 else None,
            "create_time": ctime,
            "update_time": ctime,
        })
        rid += 1
# 截断至 200
resources = resources[:200]
res_by_id = {r["id"]: r for r in resources}
res_by_course = {}
for r in resources:
    res_by_course.setdefault(r["course_id"], []).append(r["id"])

# ================= 7. 学生（30） =================
student_profiles = [
    ("林浩然", "linhaoran", 1, 1, 1, "2023"),
    ("苏晓彤", "suxiaotong", 2, 1, 1, "2023"),
    ("陈思远", "chensiyuan", 1, 1, 2, "2023"),
    ("王梓萱", "wangzixuan", 1, 1, 1, "2024"),
    ("李泽宇", "lizeyu", 1, 1, 2, "2024"),
    ("赵雨欣", "zhaoyuxin", 2, 1, 1, "2023"),
    ("黄俊杰", "huangjunjie", 1, 2, 3, "2023"),
    ("周梦琪", "zhoumengqi", 2, 2, 3, "2024"),
    ("吴佳怡", "wujiayi", 2, 2, 4, "2023"),
    ("徐子涵", "xuzihan", 1, 2, 5, "2024"),
    ("孙浩", "sunhao", 2, 2, 4, "2023"),
    ("胡安琪", "huanqi", 1, 3, 6, "2023"),
    ("郭子睿", "guozirui", 2, 3, 6, "2024"),
    ("何思源", "hesiyuan", 1, 3, 7, "2023"),
    ("高欣怡", "gaoxinyi", 2, 3, 6, "2024"),
    ("罗一鸣", "luoyiming", 1, 4, 8, "2023"),
    ("梁诗韵", "liangshiyun", 2, 4, 8, "2024"),
    ("宋雨泽", "songyuze", 1, 4, 9, "2023"),
    ("韩雪儿", "hanxueer", 2, 4, 8, "2024"),
    ("冯天磊", "fengtianlei", 1, 5, 10, "2023"),
    ("邓雅婷", "dengyating", 2, 5, 10, "2024"),
    ("萧子墨", "xiaozimo", 1, 1, 1, "2022"),
    ("曾子涵", "zengzihan", 2, 3, 6, "2022"),
    ("蒋宇航", "jiangyuhang", 1, 2, 5, "2022"),
    ("沈佳怡", "shenjiayi", 2, 4, 9, "2023"),
    ("邹明轩", "zoumingxuan", 1, 3, 7, "2022"),
    ("钟丽娟", "zhonglijuan", 2, 1, 2, "2023"),
    ("范文博", "fanwenbo", 1, 4, 8, "2022"),
    ("唐果", "tangguo", 2, 5, 10, "2024"),
    ("邵子谦", "shaoziqian", 1, 2, 3, "2023"),
]
students = []
for i, (real, uname, gender, col, maj, grade) in enumerate(student_profiles, start=1):
    students.append({
        "id": i,
        "student_no": "2023" + f"{i:03d}",
        "username": uname,
        "nickname": real,
        "real_name": real,
        "gender": gender,
        "college_id": col,
        "major_id": maj,
        "grade": grade,
        "email": f"{uname}@campus.edu.cn",
        "phone": "138" + f"{random.randint(0,99999999):08d}"[:8],
        "avatar": f"https://oss.campus.edu/avatar/{uname}.png",
        "status": 1,
        "create_time": rand_dt("2024-09-01 08:00:00", "2025-12-31 22:00:00"),
    })

# ================= 8. 管理员（2） =================
admins = [
    {"id": 1, "username": "admin", "real_name": "超级管理员", "role": 1,
     "email": "admin@campus.edu.cn", "phone": "13900000001"},
    {"id": 2, "username": "manager", "real_name": "内容审核员", "role": 2,
     "email": "manager@campus.edu.cn", "phone": "13900000002"},
]

# ================= 9. 行为（收藏/下载/评分） =================
# 20 个活跃学生
active_students = list(range(1, 21))
college_courses = {
    1: [101, 102, 103, 104, 105, 106, 107, 108, 109, 110],
    2: [201, 202, 203, 204, 205, 206, 207, 208],
    3: [301, 302, 303, 304, 305, 306, 307, 308, 309, 310],
    4: [401, 402, 403, 404, 405, 406, 407],
    5: [501, 502, 503, 504, 505, 506],
}
popular_courses = [101, 103, 106, 110, 108, 401, 501, 302]
# 选每门热门课里浏览量最高的资源作为“爆款”
popular_res = []
for c in popular_courses:
    rl = res_by_course.get(c, [])
    if rl:
        best = max(rl, key=lambda x: res_by_id[x]["view_count"])
        popular_res.append(best)

# 兴趣配置
interests = {}
for s in active_students:
    col = next(st["college_id"] for st in students if st["id"] == s)
    base = random.sample(college_courses[col], k=random.randint(2, 4))
    extra = random.sample(popular_courses, k=random.randint(1, 3))
    interests[s] = list(dict.fromkeys(base + extra))

fav = set()
dl = set()
rate = {}  # (s,r) -> score

for s in active_students:
    for c in interests[s]:
        rl = res_by_course.get(c, [])
        if not rl:
            continue
        k = min(random.randint(2, 4), len(rl))
        for r in random.sample(rl, k):
            fav.add((s, r))
        m = min(random.randint(1, 3), len(rl))
        for r in random.sample(rl, m):
            dl.add((s, r))
        rr = min(random.randint(1, 3), len(rl))
        for r in random.sample(rl, rr):
            rate.setdefault((s, r), random.randint(3, 5))
    # 爆款共现（关键：让多个学生操作相同资源 -> ItemCF 相似度）
    for r in popular_res:
        if random.random() < 0.75:
            fav.add((s, r))
        if random.random() < 0.65:
            dl.add((s, r))
        if random.random() < 0.55:
            rate.setdefault((s, r), random.randint(4, 5))

# 聚合到资源字段
fav_cnt = {}
dl_cnt = {}
rate_cnt = {}
rate_sum = {}
for (s, r) in fav:
    fav_cnt[r] = fav_cnt.get(r, 0) + 1
for (s, r) in dl:
    dl_cnt[r] = dl_cnt.get(r, 0) + 1
for (s, r), sc in rate.items():
    rate_cnt[r] = rate_cnt.get(r, 0) + 1
    rate_sum[r] = rate_sum.get(r, 0) + sc

max_view = max(r["view_count"] for r in resources)
max_dl = max(r["download_count_base"] for r in resources)
for r in resources:
    rid_ = r["id"]
    r["favorite_count"] = fav_cnt.get(rid_, 0)
    r["download_count"] = r["download_count_base"] + dl_cnt.get(rid_, 0)
    rc = rate_cnt.get(rid_, 0)
    r["rating_count"] = rc
    r["avg_rating"] = round(rate_sum.get(rid_, 0) / rc, 2) if rc else 0.00
    # heat 计算
    nv = r["view_count"] / max_view
    nd = r["download_count"] / max(max_dl, 1)
    nr = (r["avg_rating"] - 3.0) / 2.0 if rc else 0.0
    heat = 0.45 * nv + 0.30 * nd + 0.25 * max(nr, 0.0)
    r["heat"] = round(min(max(heat, 0.0), 1.0), 4)

# 知识点 heat：所属资源 heat 均值
kp_res_heat = {}
kp_res_cnt = {}
for r in resources:
    if r["primary_kp_id"]:
        kp_res_heat[r["primary_kp_id"]] = kp_res_heat.get(r["primary_kp_id"], 0.0) + r["heat"]
        kp_res_cnt[r["primary_kp_id"]] = kp_res_cnt.get(r["primary_kp_id"], 0) + 1
for k in knowledge_points:
    c = kp_res_cnt.get(k["id"], 0)
    k["heat"] = round(kp_res_heat.get(k["id"], 0.0) / c, 4) if c else 0.0

# ================= 10. 社交数据 =================
# 评论
comment_texts = [
    "讲得很清楚，收藏了，感谢分享！", "老师思路清晰，受益匪浅。", "这份资料救了我的期末，太赞了。",
    "内容详实，就是排版还能再优化一下。", "例子很到位，一看就懂。", "和教材互补，复习利器。",
    "视频画质不错，声音稍小。", "笔记整理得真用心，给博主点赞。", "考点覆盖全面，强烈推荐。",
    "有些地方语速偏快，建议倍速看。", "配合课后习题食用更佳。", "终于搞懂这个知识点了，泪目。",
    "求配套习题答案～", "课件配色舒服，看着不累。", "案例很新，贴近工程实际。",
    "讲得深入浅出，零基础也能跟。", "这部分一直是我的盲区，讲透了。", "已下载，准备二刷。",
    "内容扎实，但缺了点总结。", "博主牛，希望多出这类资源。",
]
comments = []  # dict id,resource_id,student_id,content,parent_id,like_count,create_time
cid_counter = 1
comment_pool = []  # (id, resource_id) for replies
for r in resources:
    if r["status"] != 1:
        continue
    n = random.randint(0, 2)
    for _ in range(n):
        sid = random.randint(1, 30)
        ctime = rand_dt(r["create_time"], "2026-06-15 22:00:00")
        parent = None
        # 30% 作为楼中楼
        if comment_pool and random.random() < 0.3:
            parent = random.choice(comment_pool)[0]
        comments.append({
            "id": cid_counter,
            "resource_id": r["id"],
            "student_id": sid,
            "content": random.choice(comment_texts),
            "parent_id": parent,
            "like_count": random.randint(0, 35),
            "create_time": ctime,
        })
        comment_pool.append((cid_counter, r["id"]))
        cid_counter += 1

# 资源需求
req_titles = [
    "求数据结构树章节的习题答案", "谁有操作系统进程同步的真题？", "机器学习吴恩达笔记谁有电子版",
    "线性代数历年期末试卷求分享", "计算机网络 TCP 三次握手讲解资料", "软件工程需求分析报告模板",
    "概率论数理统计公式汇总", "英语四级写作万能句型整理", "编译原理 LL(1) 分析例题",
    "数据库 SQL 复杂查询练习", "高等数学上册知识点总结", "人工智能导论复习大纲",
    "Web 前端框架对比资料", "离散数学图论部分习题", "数值分析上机实验代码",
]
req_contents = [
    "马上要考试了，自己整理的有点乱，求一份清晰的版本，谢谢各位！",
    "老师讲得太快没跟上，想找点补充资料过一遍。",
    "准备考研，需要系统性的复习材料，有偿也可以。",
    "作业卡在这一章了，求大佬指点迷津。",
    "想做课程设计，缺参考案例，哪位有好资源吱一声。",
]
requirements = []
rid_counter = 1
for i, t in enumerate(req_titles):
    sid = random.randint(1, 30)
    col = random.choice([1, 2, 3, 4, 5])
    cat = random.choice([8, 9, 11, 12, 2, 14])
    status = 0 if i % 4 != 0 else 1
    requirements.append({
        "id": rid_counter,
        "student_id": sid,
        "title": t,
        "content": random.choice(req_contents),
        "category_id": cat,
        "college_id": col,
        "status": status,
        "reply_count": 0,
        "create_time": rand_dt("2025-10-01 08:00:00", "2026-06-01 22:00:00"),
    })
    rid_counter += 1
# 需求回复
req_replies = []
rr_counter = 1
for req in requirements:
    n = random.randint(0, 3)
    for _ in range(n):
        sid = random.randint(1, 30)
        txt = random.choice([
            "我这里有，已私发你～", "推荐看 B 站某 UP 的系列，讲得特别好。",
            "我有笔记，稍后传到资源区。", "这个考点常考，建议多做几遍真题。",
            "同求，蹲一个。", "其实教材例题就够用了，关键是动手。",
        ])
        req_replies.append({
            "id": rr_counter,
            "requirement_id": req["id"],
            "student_id": sid,
            "content": txt,
            "like_count": random.randint(0, 18),
            "create_time": rand_dt(req["create_time"], "2026-06-10 22:00:00"),
        })
        rr_counter += 1
# 回填 reply_count
for req in requirements:
    req["reply_count"] = sum(1 for x in req_replies if x["requirement_id"] == req["id"])

# 问答帖子
qa_titles = [
    "动态规划到底怎么理解啊？", "进程和线程的区别到底在哪？", "TCP 为什么是三次握手不是两次？",
    "考研数学高数怎么复习最高效？", "Java 和 Python 该先学哪个？", "数据库索引为什么用 B+ 树？",
    "线性代数有什么实际用途？", "四级写作总是低分怎么办？", "神经网络反向传播推导看不懂",
    "操作系统死锁怎么预防和避免？", "前端框架 Vue 和 React 怎么选？", "概率论大数定律到底啥意思",
    "编译原理难不难？怎么入门？", "软件测试到底测什么？", "图论最短路径算法选哪个好",
    "英语口语怎么练才地道？", "机器学习需要多强数学基础？", "离散数学对编程有帮助吗？",
    "云计算和大数据是什么关系？", "学术英语论文怎么写才规范？",
]
qa_contents = [
    "看了很多资料还是云里雾里，望大佬用通俗的话讲讲，最好带例子。",
    "考试要考，但概念总是记混，求一张对比表或者记忆口诀。",
    "刚入门，基础一般，求推荐学习路径和上手项目。",
    "作业要做，思路完全没方向，给点提示就行，谢谢！",
    "想深入但不知道从哪下手，有没有经典教材推荐。",
]
qa_tags_pool = ["算法", "操作系统", "网络", "数学", "编程", "数据库", "英语", "机器学习", "软件工程", "考研"]
qa_posts = []
qid_counter = 1
qa_pool = []
for i, t in enumerate(qa_titles):
    sid = random.randint(1, 30)
    cat = random.choice([8, 9, 11, 12, 2, 14])
    tags = ",".join(random.sample(qa_tags_pool, k=random.randint(1, 3)))
    status = 0 if i % 5 != 0 else 1
    post = {
        "id": qid_counter,
        "student_id": sid,
        "title": t,
        "content": random.choice(qa_contents),
        "category_id": cat,
        "tags": tags,
        "view_count": random.randint(30, 1200),
        "reply_count": 0,
        "like_count": random.randint(0, 60),
        "status": status,
        "create_time": rand_dt("2025-10-01 08:00:00", "2026-06-05 22:00:00"),
    }
    qa_posts.append(post)
    qa_pool.append(qid_counter)
    qid_counter += 1
# 问答回复
qa_replies = []
qr_counter = 1
for post in qa_posts:
    n = random.randint(0, 4)
    best_chosen = False
    for _ in range(n):
        sid = random.randint(1, 30)
        parent = None
        if random.random() < 0.25 and qa_replies:
            parent = random.choice([x["id"] for x in qa_replies if x["post_id"] == post["id"]]) if any(x["post_id"] == post["id"] for x in qa_replies) else None
        is_best = 0
        if post["status"] == 1 and not best_chosen and _ == n - 1:
            is_best = 1
            best_chosen = True
        qa_replies.append({
            "id": qr_counter,
            "post_id": post["id"],
            "student_id": sid,
            "content": random.choice([
                "核心就是抓住定义，多画图就通了。", "建议先啃教材例题，再刷真题巩固。",
                "我当初也是，后来跟着项目做一遍就懂了。", "这个可以看某公开课第 X 集，讲得贼清楚。",
                "本质是……（此处省略一千字），关键是多练。", "同问，蹲一个靠谱解答。",
                "推荐《XX》这本书，系统又通俗。", "记住一句话：理解 > 背诵。",
            ]),
            "parent_id": parent,
            "like_count": random.randint(0, 25),
            "is_best": is_best,
            "create_time": rand_dt(post["create_time"], "2026-06-12 22:00:00"),
        })
        qr_counter += 1
for post in qa_posts:
    post["reply_count"] = sum(1 for x in qa_replies if x["post_id"] == post["id"])

# 系统公告
announcements = [
    ("平台资源审核规则更新通知", "为提升资源质量，即日起所有上传资源需经管理员审核通过后方可公开展示，请各位同学知悉。", 2, 1),
    ("期末复习资源专题上线", "已上线数据结构、操作系统、高等数学等核心课程复习专题，欢迎收藏下载。", 1, 1),
    ("知识图谱推荐功能正式开放", "基于知识图谱的'相关资源'推荐已上线，浏览资源时可查看同课程、先修与相关知识点资源。", 3, 1),
    ("系统维护升级预告", "本周六凌晨 02:00-04:00 进行数据库维护，期间服务可能短暂不可用。", 2, 0),
    ("优秀学习资源征集活动", "开展季度优秀学习资源征集，入选作品将获得平台积分与首页推荐。", 1, 1),
]
ann_list = []
for i, (title, content, prio, pub) in enumerate(announcements, start=1):
    ann_list.append({
        "id": i,
        "admin_id": 1 if i % 2 == 1 else 2,
        "title": title,
        "content": content,
        "priority": prio,
        "publish_status": pub,
        "create_time": rand_dt("2025-09-10 08:00:00", "2026-05-20 22:00:00"),
        "publish_time": rand_dt("2025-09-12 08:00:00", "2026-05-25 22:00:00") if pub == 1 else None,
    })

# 资源审核记录 & Neo4j 同步日志
review_records = []
rrv_counter = 1
sync_logs = []
slog_counter = 1
for r in resources:
    submitter = r["uploader_id"] if r["uploader_type"] == 1 else random.randint(1, 30)
    reviewer = random.choice([1, 2])
    t0 = r["create_time"]
    review_records.append({
        "id": rrv_counter, "resource_id": r["id"], "submitter_id": submitter,
        "reviewer_id": None, "action": 1, "review_comment": "用户提交资源，等待审核",
        "create_time": t0,
    })
    rrv_counter += 1
    if r["status"] == 1:
        t1 = r["audit_time"] or t0
        review_records.append({
            "id": rrv_counter, "resource_id": r["id"], "submitter_id": None,
            "reviewer_id": reviewer, "action": 2, "review_comment": "内容合规，审核通过",
            "create_time": t1,
        })
        rrv_counter += 1
        # 同步日志：成功
        sync_logs.append({
            "id": slog_counter, "resource_id": r["id"], "sync_type": 1,
            "sync_status": 1, "error_msg": None, "create_time": t1,
        })
        slog_counter += 1
    elif r["status"] == 2:
        t1 = rand_dt(t0, "2026-06-12 22:00:00")
        review_records.append({
            "id": rrv_counter, "resource_id": r["id"], "submitter_id": None,
            "reviewer_id": reviewer, "action": 3, "review_comment": "内容与课程无关，予以驳回",
            "create_time": t1,
        })
        rrv_counter += 1
        # 同步失败日志（演示重试）
        sync_logs.append({
            "id": slog_counter, "resource_id": r["id"], "sync_type": 1,
            "sync_status": 2, "error_msg": "知识点映射缺失，已记录待重试",
            "create_time": t1,
        })
        slog_counter += 1
    else:  # status 0 待审核
        sync_logs.append({
            "id": slog_counter, "resource_id": r["id"], "sync_type": 1,
            "sync_status": 0, "error_msg": None, "create_time": t0,
        })
        slog_counter += 1

print("数据生成完毕：",
      "college", len(colleges),
      "major", len(majors),
      "course", len(courses),
      "kp", len(knowledge_points),
      "res", len(resources),
      "student", len(students),
      "admin", len(admins),
      "fav", len(fav), "dl", len(dl), "rate", len(rate),
      "comment", len(comments),
      "req", len(requirements), "req_reply", len(req_replies),
      "qa", len(qa_posts), "qa_reply", len(qa_replies),
      "ann", len(ann_list),
      "review", len(review_records), "sync", len(sync_logs))
