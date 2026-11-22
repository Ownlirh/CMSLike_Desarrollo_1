const boton = document.getElementById('alternarVista');
boton.addEventListener('click', () => {
    const movil = document.querySelector('.contenido').classList.toggle('movil');
    boton.textContent = movil ? 'Ver en escritorio' : 'Simular móvil';
    boton.setAttribute('aria-pressed', String(movil));
});
