document.addEventListener('DOMContentLoaded', () => {
  const menuMovil = document.querySelector('.menu-movil');
  if (menuMovil) {
    menuMovil.addEventListener('click', () => {
      const abierto = menuMovil.closest('.app-header').classList.toggle('menu-abierto');
      menuMovil.setAttribute('aria-expanded', String(abierto));
      menuMovil.textContent = abierto ? '✕ Cerrar menú' : '☰ Menú';
    });
  }
  const ruta = window.location.pathname;
  const enlaces = Array.from(document.querySelectorAll('.app-header nav a'));
  const rutaExacta = enlaces.some(enlace => enlace.getAttribute('href') === ruta);
  enlaces.forEach(enlace => {
    const destino = enlace.getAttribute('href');
    if (ruta === destino || (!rutaExacta && destino.endsWith('/listar') && ruta.startsWith(destino.replace('/listar', ''))) || (destino.endsWith('/home') && ruta === '/')) {
      enlace.classList.add('activo'); enlace.setAttribute('aria-current', 'page');
    }
  });
  const filtros = document.getElementById('filtros-inventario');
  if (!filtros) return;
  const lista = document.getElementById('lista-productos');
  const filas = Array.from(lista.querySelectorAll('.fila-producto'));
  const buscador = document.getElementById('buscar-producto');
  const coincideEstado = (fila, estado) => {
    const stock = Number(fila.dataset.stock);
    return estado === 'todos' || (estado === 'disponible' && stock > 0) || (estado === 'bajo' && stock > 0 && stock <= 5) || (estado === 'agotado' && stock === 0);
  };
  document.querySelectorAll('[data-conteo]').forEach(conteo => {
    conteo.textContent = filas.filter(fila => coincideEstado(fila, conteo.dataset.conteo)).length;
  });
  const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  const actualizar = () => {
    const estado = filtros.elements.estado.value;
    const minimo = filtros.elements.minimo.value === '' ? 0 : Number(filtros.elements.minimo.value);
    const maximo = filtros.elements.maximo.value === '' ? Infinity : Number(filtros.elements.maximo.value);
    const texto = normalizar(buscador.value.trim());
    let visibles = 0;
    const orden = filtros.elements.orden.value;
    filas.sort((a, b) => orden === 'stock' ? Number(a.dataset.stock) - Number(b.dataset.stock) : orden === 'precio' ? Number(a.dataset.precio) - Number(b.dataset.precio) : orden === 'reciente' ? Number(b.dataset.id) - Number(a.dataset.id) : a.dataset.nombre.localeCompare(b.dataset.nombre, 'es'));
    filas.forEach(fila => {
      const coincide = coincideEstado(fila, estado) && Number(fila.dataset.precio) >= minimo && Number(fila.dataset.precio) <= maximo && normalizar(`${fila.dataset.nombre} ${fila.dataset.descripcion} ${fila.dataset.id}`).includes(texto);
      fila.hidden = !coincide; if (coincide) visibles++;
      lista.appendChild(fila);
    });
    document.getElementById('resultado-productos').textContent = `${visibles} de ${filas.length} productos`;
    document.getElementById('sin-resultados').hidden = visibles !== 0 || filas.length === 0;
  };
  filtros.addEventListener('input', actualizar);
  buscador.addEventListener('input', actualizar);
  filtros.addEventListener('reset', () => { buscador.value = ''; setTimeout(actualizar, 0); });
  document.getElementById('vista-productos').addEventListener('click', evento => {
    const tarjetas = lista.classList.toggle('vista-tarjetas');
    evento.currentTarget.setAttribute('aria-pressed', String(tarjetas));
    evento.currentTarget.textContent = tarjetas ? '☷ Vista de lista' : '▦ Vista de tarjetas';
  });
  actualizar();
});
