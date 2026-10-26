import { createRouter, createWebHistory } from 'vue-router'
import PaginaHarta from './pagini/PaginaHarta.vue'
import PaginaRetea from './pagini/PaginaRetea.vue'
import PaginaAlarme from './pagini/PaginaAlarme.vue'
import PaginaLogin from './pagini/PaginaLogin.vue'
import PaginaSetari from './pagini/PaginaSetari.vue'
import PaginaConturi from './pagini/PaginaConturi.vue'
import { sesiune, asiguraUtilizatorul, esteAdmin } from './sesiune.js'

/** Paginile din spec §7 și §16.4. Doar /login e publică. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: PaginaLogin, meta: { public: true } },
    { path: '/', component: PaginaHarta },
    { path: '/retea', component: PaginaRetea },
    { path: '/alarme', component: PaginaAlarme },
    { path: '/setari', component: PaginaSetari },
    { path: '/conturi', component: PaginaConturi, meta: { doarAdmin: true } },
  ],
})

/**
 * Garda e recepția: înainte să te lase pe un hol, se uită dacă ai brățară, iar pentru biroul șefului, dacă scrie
 * „admin” pe ea. Brățara falsă o descoperă serverul, la prima cerere, cu 401.
 */
router.beforeEach(async (spre) => {
  if (!sesiune.token && !spre.meta.public) return { path: '/login', query: { inapoi: spre.fullPath } }
  if (sesiune.token && spre.path === '/login') return '/'
  if (spre.meta.doarAdmin) {
    try {
      await asiguraUtilizatorul()
    } catch {
      return sesiune.token ? '/' : '/login'
    }
    if (!esteAdmin()) return '/'
  }
  return true
})
