import { defineStore } from 'pinia'
import { postData } from '../api/client'

interface MenuItem {
  title: string
  path: string
  icon: string
  permission: string
}

interface LoginResponse {
  accessToken: string
  user: {
    id: number
    username: string
    displayName: string
    roles: string[]
    permissions: string[]
  }
  menus: MenuItem[]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null') as LoginResponse['user'] | null,
    menus: JSON.parse(localStorage.getItem('menus') || '[]') as MenuItem[]
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await postData<LoginResponse>('/auth/login', { username, password })
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('user', JSON.stringify(data.user))
      localStorage.setItem('menus', JSON.stringify(data.menus))
      this.user = data.user
      this.menus = data.menus
    },
    logout() {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
      localStorage.removeItem('menus')
      this.user = null
      this.menus = []
    }
  }
})
