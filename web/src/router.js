import { createRouter, createWebHistory } from 'vue-router'
import PaginaHarta from './pagini/PaginaHarta.vue'
import PaginaRetea from './pagini/PaginaRetea.vue'
import PaginaAlarme from './pagini/PaginaAlarme.vue'

/** Paginile din spec §7. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PaginaHarta },
    { path: '/retea', component: PaginaRetea },
    { path: '/alarme', component: PaginaAlarme },
  ],
})
