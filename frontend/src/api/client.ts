import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export const client = axios.create({
  baseURL: '/api',
  timeout: 10000
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await client.get<ApiResponse<T>>(url, { params })
  return response.data.data
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const response = await client.post<ApiResponse<T>>(url, data)
  return response.data.data
}

export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const response = await client.put<ApiResponse<T>>(url, data)
  return response.data.data
}

export async function deleteData<T>(url: string): Promise<T> {
  const response = await client.delete<ApiResponse<T>>(url)
  return response.data.data
}
