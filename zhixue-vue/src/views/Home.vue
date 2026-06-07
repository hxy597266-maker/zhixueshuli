<template>
  <div>
    <!-- Banner -->
    <div class="banner">
      <h1>数理学习，从这里开始</h1>
      <p>Java、离散数学、C语言等一站式学习辅助平台</p>
      <a href="http://localhost:8080/course.html" class="btn" target="_blank">立即学习</a>
    </div>

    <!-- 热门课程推荐 -->
    <div class="courses">
      <h2>热门课程推荐</h2>
      <div class="course-list">
        <div class="course-card">
          <h3>C语言</h3>
          <p>基础语法、数组、函数</p>
          <a href="http://localhost:8080/c.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>线性代数</h3>
          <p>矩阵、行列式、向量空间</p>
          <a href="http://localhost:8080/math.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>离散数学</h3>
          <p>集合论、逻辑、图论</p>
          <a href="http://localhost:8080/discrete.html" target="_blank">查看详情 →</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.banner {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 80px 20px;
  text-align: center;
}
.banner h1 {
  font-size: 36px;
  margin-bottom: 15px;
}
.banner p {
  font-size: 18px;
  margin-bottom: 30px;
}
.banner .btn {
  display: inline-block;
  background: white;
  color: #667eea;
  padding: 15px 40px;
  border-radius: 50px;
  text-decoration: none;
  font-weight: bold;
  font-size: 18px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}
.banner .btn:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.3);
  background: #f8f9ff;
}
.courses {
  max-width: 1200px;
  margin: 50px auto;
  padding: 0 20px;
}
.courses h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #2c3e50;
}
.course-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.course-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.course-card h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}
.course-card p {
  color: #666;
  margin-bottom: 15px;
}
.course-card a {
  color: #667eea;
  text-decoration: none;
  font-weight: bold;
}
</style>