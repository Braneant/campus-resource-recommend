import { defineStore } from 'pinia'

// 登录态 store：管理 JWT、用户信息、角色（STUDENT / ADMIN）
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    role: localStorage.getItem('role') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isStudent: (state) => state.role === 'STUDENT',
    isAdmin: (state) => state.role === 'ADMIN',
    userId: (state) => (state.userInfo ? state.userInfo.id : null)
  },
  actions: {
    setAuth({ token, role, user }) {
      this.token = token
      this.role = role
      this.userInfo = user || null
      localStorage.setItem('token', token)
      localStorage.setItem('role', role)
      if (user) localStorage.setItem('userInfo', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.role = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('userInfo')
    },
    // 仅更新内存中的用户信息（资料编辑后）
    setUserInfo(user) {
      this.userInfo = { ...this.userInfo, ...user }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    }
  }
})
