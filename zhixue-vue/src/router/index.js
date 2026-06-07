import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import Community from '../views/Community.vue'
import User from '../views/User.vue'
import PostDetail from '../views/PostDetail.vue'
import Course from '../views/Course.vue'
import AITool from '../views/AITool.vue'
import Admin from '../views/Admin.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', component: Home },
      { path: 'community', component: Community },
      { path: 'user', component: User },
      { path: 'post/:id', component: PostDetail },
      { path: 'course', component: Course },
      { path: 'aitool', component: AITool }
    ]
  },
  { path: '/login', component: Login },
  { path: '/admin', component: Admin }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router