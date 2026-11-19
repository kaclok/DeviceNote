import { login as apiLogin, getMe } from '../api'

const TOKEN_KEY = 'vs_token'
const USER_KEY = 'vs_username'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getUsername() {
  return localStorage.getItem(USER_KEY) || ''
}

export function setToken(token, username) {
  localStorage.setItem(TOKEN_KEY, token)
  if (username) localStorage.setItem(USER_KEY, username)
}

export function clear() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export async function login(username, password) {
  const res = await apiLogin(username, password)
  const data = res.data || res
  setToken(data.token, data.username)
  return data
}

export async function logout() {
  clear()
}

export async function fetchMe() {
  const res = await getMe()
  return res.data || res
}
