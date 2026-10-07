document.addEventListener('DOMContentLoaded', () => {
    const imprimir = () => window.print();
    document.getElementById('imprimir-factura').addEventListener('click', imprimir);
    if (document.querySelector('.factura').dataset.imprimir === 'true') {
        window.addEventListener('load', imprimir, { once: true });
    }
});
