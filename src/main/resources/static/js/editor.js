const formulario = document.getElementById('editor');
const contenido = document.getElementById('bloquesGuardados');
const lienzo = document.getElementById('bloques');
const nombres = {
    ENCABEZADO: 'Encabezado', DESTACADA: 'Imagen destacada',
    FILA_IMAGENES: 'Fila de imágenes', TEXTO: 'Texto', BOTON: 'Botón'
};
const estilos = {
    ENCABEZADO: ['izquierda', 'centro', 'derecha'],
    DESTACADA: ['baja', 'media', 'alta'],
    TEXTO: ['parrafos', 'lista'],
    BOTON: ['principal', 'secundario']
};
let bloques;
try {
    const datos = JSON.parse(contenido.value);
    bloques = Array.isArray(datos) ? datos.filter(bloque => bloque && Object.hasOwn(nombres, bloque.tipo)) : [];
} catch {
    bloques = [];
}

function elemento(etiqueta, texto) {
    const nodo = document.createElement(etiqueta);
    if (texto) nodo.textContent = texto;
    return nodo;
}

function boton(texto, accion, desactivado = false) {
    const control = elemento('button', texto);
    control.type = 'button';
    control.className = 'secundario';
    control.disabled = desactivado;
    control.addEventListener('click', accion);
    return control;
}

function campo(padre, objeto, clave, etiqueta, opciones = {}) {
    const { requerido = false, multilinea = false, maximo = 160, valores } = opciones;
    const rotulo = elemento('label', etiqueta);
    const control = elemento(valores ? 'select' : multilinea ? 'textarea' : 'input');
    if (valores) {
        for (const valor of valores) {
            control.append(new Option(valor[1], valor[0]));
        }
    } else {
        control.maxLength = maximo;
    }
    control.value = objeto[clave] || (valores ? valores[0][0] : '');
    control.required = requerido;
    control.addEventListener('input', () => { objeto[clave] = control.value; });
    rotulo.append(control);
    padre.append(rotulo);
    return control;
}

function imagenVacia() {
    return {url: '', alternativo: '', enlace: ''};
}

function imagenPrevia(valor, imagen) {
    imagen.hidden = true;
    imagen.removeAttribute('src');
    if (typeof valor !== 'string' || valor.includes('\\')) return;
    try {
        const url = new URL(valor, location.origin);
        const interna = valor.startsWith('/') && !valor.startsWith('//') && url.origin === location.origin;
        if (interna || /^https:\/\//i.test(valor) && url.protocol === 'https:' && !url.username && !url.password) {
            imagen.src = url.href;
            imagen.hidden = false;
        }
    } catch {
        imagen.hidden = true;
    }
}

function mostrarImagenes(panel, bloque) {
    const cantidadMaxima = bloque.tipo === 'FILA_IMAGENES' ? 4 : 1;
    const actuales = Array.isArray(bloque.imagenes) ? bloque.imagenes.slice(0, cantidadMaxima) : [];
    bloque.imagenes = actuales.length
        ? actuales.map(imagen => imagen && typeof imagen === 'object' && !Array.isArray(imagen) ? imagen : imagenVacia())
        : [imagenVacia()];
    if (bloque.tipo === 'FILA_IMAGENES') {
        const ajustes = {cantidad: String(bloque.imagenes.length)};
        const selector = campo(panel, ajustes, 'cantidad', 'Imágenes por fila', {
            valores: [1, 2, 3, 4].map(numero => [String(numero), String(numero)])
        });
        selector.addEventListener('change', () => {
            const cantidad = Number(selector.value);
            bloque.imagenes = Array.from({length: cantidad}, (_, i) => bloque.imagenes[i] ?? imagenVacia());
            mostrarBloques();
        });
    }
    const fila = elemento('div');
    fila.className = 'campos';
    bloque.imagenes.forEach((datos, indice) => {
        const tarjeta = elemento('fieldset');
        tarjeta.className = 'imagen-editor';
        tarjeta.append(elemento('legend', 'Imagen ' + (indice + 1)));
        const url = campo(tarjeta, datos, 'url', 'URL de la imagen', {requerido: true, maximo: 500});
        campo(tarjeta, datos, 'alternativo', 'Texto alternativo', {requerido: true});
        if (bloque.tipo === 'FILA_IMAGENES') campo(tarjeta, datos, 'enlace', 'Enlace', {maximo: 500});
        const imagen = elemento('img');
        imagen.alt = '';
        imagen.referrerPolicy = 'no-referrer';
        imagenPrevia(datos.url, imagen);
        url.addEventListener('change', () => imagenPrevia(datos.url, imagen));
        tarjeta.append(imagen);
        fila.append(tarjeta);
    });
    panel.append(fila);
}

function mover(indice, destino) {
    const [bloque] = bloques.splice(indice, 1);
    bloques.splice(destino, 0, bloque);
    mostrarBloques();
}

function mostrarBloques() {
    lienzo.replaceChildren();
    if (!bloques.length) lienzo.append(elemento('p', 'Agrega un bloque desde el panel de la izquierda.'));
    for (const [indice, bloque] of bloques.entries()) {
        const panel = elemento('section');
        panel.className = 'bloque-editor';
        const cabecera = elemento('header');
        cabecera.className = 'acciones-inline';
        cabecera.append(elemento('strong', nombres[bloque.tipo]),
            boton('Subir', () => mover(indice, indice - 1), indice === 0),
            boton('Bajar', () => mover(indice, indice + 1), indice === bloques.length - 1),
            boton('Duplicar', () => {
                bloques.splice(indice + 1, 0, structuredClone(bloque));
                mostrarBloques();
            }, bloques.length >= 40),
            boton('Quitar', () => {
                bloques.splice(indice, 1);
                mostrarBloques();
            }));
        panel.append(cabecera);

        if (bloque.tipo === 'ENCABEZADO' || bloque.tipo === 'BOTON' || bloque.tipo === 'DESTACADA') {
            campo(panel, bloque, 'titulo', bloque.tipo === 'BOTON' ? 'Texto del botón' : 'Título',
                {requerido: bloque.tipo !== 'DESTACADA'});
        }
        if (bloque.tipo === 'TEXTO') {
            campo(panel, bloque, 'titulo', 'Subtítulo opcional');
            campo(panel, bloque, 'url', 'Enlace opcional', {maximo: 500});
        }
        if (bloque.tipo === 'TEXTO' || bloque.tipo === 'ENCABEZADO') {
            campo(panel, bloque, 'texto', bloque.tipo === 'TEXTO' ? 'Contenido (un elemento por línea si eliges Lista)' : 'Subtítulo',
                {requerido: bloque.tipo === 'TEXTO', multilinea: true, maximo: 4000});
        }
        if (estilos[bloque.tipo]) {
            const etiqueta = {ENCABEZADO: 'Alineación', DESTACADA: 'Altura', TEXTO: 'Formato', BOTON: 'Estilo'}[bloque.tipo];
            const etiquetasTexto = {parrafos: 'Párrafos', lista: 'Lista'};
            campo(panel, bloque, 'estilo', etiqueta, {valores: estilos[bloque.tipo].map(valor => [valor, etiquetasTexto[valor] ?? valor])});
        }
        if (bloque.tipo === 'BOTON') campo(panel, bloque, 'url', 'Enlace', {requerido: true, maximo: 500});
        if (bloque.tipo === 'DESTACADA' || bloque.tipo === 'FILA_IMAGENES') mostrarImagenes(panel, bloque);
        lienzo.append(panel);
    }
    formulario.querySelectorAll('[data-tipo]').forEach(control => { control.disabled = bloques.length >= 40; });
}

formulario.querySelectorAll('[data-tipo]').forEach(control => {
    control.addEventListener('click', () => {
        const tipo = control.dataset.tipo;
        bloques.push({tipo, titulo: '', texto: '', url: '', estilo: estilos[tipo]?.[0] ?? '',
            imagenes: tipo === 'DESTACADA' || tipo === 'FILA_IMAGENES' ? [imagenVacia()] : []});
        mostrarBloques();
        lienzo.lastElementChild.querySelector('input, textarea, select')?.focus();
    });
});
formulario.addEventListener('submit', () => { contenido.value = JSON.stringify(bloques); });
mostrarBloques();
document.getElementById('guardar').disabled = false;

const previsualizar = document.getElementById('previsualizar');
const vista = document.getElementById('vistaEditor');
const paginaPrevia = document.getElementById('paginaPrevia');
const errorVista = document.getElementById('errorVista');
previsualizar.disabled = false;
previsualizar.addEventListener('click', async () => {
    if (!formulario.reportValidity()) return;
    errorVista.hidden = true;
    previsualizar.disabled = true;
    contenido.value = JSON.stringify(bloques);
    const campos = new URLSearchParams(new FormData(formulario));
    if (formulario.dataset.pagina) campos.set('paginaId', formulario.dataset.pagina);
    try {
        const respuesta = await fetch(formulario.dataset.preview, {method: 'POST', body: campos, redirect: 'error'});
        if (!respuesta.ok) throw new Error('Vista previa no disponible');
        paginaPrevia.srcdoc = await respuesta.text();
        vista.hidden = false;
    } catch {
        vista.hidden = true;
        errorVista.textContent = 'No se pudo generar la vista previa. Revisa los bloques y tus permisos.';
        errorVista.hidden = false;
    } finally {
        previsualizar.disabled = false;
    }
});
document.getElementById('cerrarVista').addEventListener('click', () => {
    vista.hidden = true;
    paginaPrevia.removeAttribute('srcdoc');
    previsualizar.focus();
});
document.getElementById('vistaMovil').addEventListener('click', evento => {
    const movil = paginaPrevia.classList.toggle('movil');
    evento.currentTarget.textContent = movil ? 'Ver en escritorio' : 'Simular móvil';
});
