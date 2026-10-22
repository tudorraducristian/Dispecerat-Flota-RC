import { createRouter, createWebHistory } from 'vue-router'
import PaginaHarta from './pagini/PaginaHarta.vue'
import PaginaRetea from './pagini/PaginaRetea.vue'

/** Paginile din spec §7. Pagina Alarme intră în Task 13. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PaginaHarta },
    { path: '/retea', component: PaginaRetea },
  ],
})
