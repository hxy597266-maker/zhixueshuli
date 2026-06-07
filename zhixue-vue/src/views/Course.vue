<template>
  <div>
    <div class="page-title">
      <h1>全部课程</h1>
      <p style="color: #666; margin-top: 8px;">高数、线代、离散数学等数理课程一站式学习平台</p>
    </div>

    <div class="courses">
      <div class="course-grid">
        <div class="course-card">
          <h3>常微分方程</h3>
          <p>微分方程求解方法、常微分方程基础理论与应用</p>
          <a href="http://localhost:8080/ode.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>C语言</h3>
          <p>编程入门、基础语法、指针与数组，代码实现与习题</p>
          <a href="http://localhost:8080/c.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>线性代数</h3>
          <p>矩阵、行列式、向量空间、线性方程组与特征值问题教程</p>
          <a href="http://localhost:8080/math.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>Java</h3>
          <p>面型对象编程、Java基础语法、类与对象、常用API教程</p>
          <a href="http://localhost:8080/java.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>离散数学</h3>
          <p>集合论、命题逻辑、谓词逻辑、图论与代数系统讲解</p>
          <a href="http://localhost:8080/discrete.html" target="_blank">查看详情 →</a>
        </div>
        <div class="course-card">
          <h3>数据结构（C语言版）</h3>
          <p>线性表、栈队列、树、图与排序算法，配套代码实现与习题</p>
          <a href="http://localhost:8080/ds.html" target="_blank">查看详情 →</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  max-width: 1200px;
  margin: 30px auto 0;
  padding: 0 20px;
  color: #2c3e50;
}
.courses {
  max-width: 1200px;
  margin: 30px auto 50px;
  padding: 0 20px;
}
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 25px;
}
.course-card {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.08);
  transition: transform 0.2s;
}
.course-card:hover {
  transform: translateY(-5px);
}
.course-card h3 {
  color: #2c3e50;
  margin-bottom: 12px;
  font-size: 18px;
}
.course-card p {
  color: #666;
  margin-bottom: 18px;
  line-height: 1.6;
}
.course-card a {
  color: #667eea;
  text-decoration: none;
  font-weight: bold;
}
.course-card a:hover {
  text-decoration: underline;
}
</style>
